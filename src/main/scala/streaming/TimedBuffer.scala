package streaming

import akka.actor.Props
import akka.stream.actor.{ActorSubscriberMessage, WatermarkRequestStrategy, RequestStrategy, ActorSubscriber}
import sensor.{Measurement, Timestamp}

import scala.collection.immutable.SortedSet
import scala.concurrent.duration.FiniteDuration

class TimedBuffer(keepLast: FiniteDuration) extends ActorSubscriber {

  import sensor.TimestampOrd

  var recorded = SortedSet.empty[Measurement]

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(20)

  override def receive: Receive = {
    case ActorSubscriberMessage.OnNext(m: Measurement) =>
      recorded = recorded + m

    case TimedBuffer.Get =>
      sender() ! recorded.filter(recordedAfter(oldestAllowed(System.currentTimeMillis())))

  }

  def recordedAfter(earliestAllowed: Timestamp)(elem: Measurement) = {
    elem.timestamp > earliestAllowed
  }

  def oldestAllowed(mostRecent: Timestamp ) =
    mostRecent - keepLast.toMillis

}

object TimedBuffer {

  def props(keepLast: FiniteDuration) = Props(classOf[TimedBuffer], keepLast)

  object Get

}