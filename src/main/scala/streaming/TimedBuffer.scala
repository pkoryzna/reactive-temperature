package streaming

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.stream.actor.{ActorSubscriberMessage, WatermarkRequestStrategy, RequestStrategy, ActorSubscriber}
import sensor.{SerialNumber, Measurement, Timestamp}
import streaming.TimedBuffer.{LogStats, Get}

import scala.collection.immutable.SortedSet
import scala.concurrent.duration.FiniteDuration

class TimedBuffer(keepLast: FiniteDuration) extends ActorSubscriber with ActorLogging {

  import sensor.TimestampOrd

  var recorded = SortedSet.empty[Measurement]

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(20)

  override def receive: Receive = {
    case ActorSubscriberMessage.OnNext(m: Measurement) =>
      recorded = (recorded + m).filter(recordedAfter(oldestAllowed(System.currentTimeMillis())))

    case Get =>
      sender() ! recorded

    case LogStats =>
      val stats = recorded.groupBy(_.serialNumber).foreach { e =>
        val (SerialNumber(sn), measurements) = e
        log.info(s"sensor $sn, recorded ${measurements.size}, last: ${measurements.last.value} deg C")
      }



  }

  def recordedAfter(earliestAllowed: Timestamp)(elem: Measurement) = {
    elem.timestamp > earliestAllowed
  }

  def oldestAllowed(mostRecent: Timestamp ) =
    mostRecent - keepLast.toMillis

}

object TimedBuffer {

  def props(keepLast: FiniteDuration) = Props(classOf[TimedBuffer], keepLast)

  case object Get

  case object LogStats

}