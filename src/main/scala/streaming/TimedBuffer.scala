package streaming

import akka.actor.Actor.Receive
import akka.stream.actor.{ActorSubscriberMessage, WatermarkRequestStrategy, RequestStrategy, ActorSubscriber}
import akka.stream.stage.{SyncDirective, Context, PushPullStage}

import scala.collection.immutable.SortedSet
import scala.concurrent.duration.FiniteDuration

import TimedBuffer._

class TimedBuffer[T](keepLast: FiniteDuration, now: () => Timestamp) extends ActorSubscriber {

  implicit val timestampedOrdering = Ordering.by[(Timestamp, T), Long](_._1)

  var recorded = SortedSet.empty[(Timestamp, T)]

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(20)

  override def receive: Receive = {
    case ActorSubscriberMessage.OnNext(elem: T) =>
      recorded = recorded.dropWhile(recordedBefore(oldestPossible())) + (now() -> elem)

    case TimedBuffer.Get =>
      sender() ! recorded

  }

  def recordedBefore(earliestAllowed: Timestamp)(elem: (Timestamp, T)) = {
    elem._1 < earliestAllowed
  }

  def oldestPossible(current: Timestamp = now): Timestamp =
    current - keepLast.toMillis

}

object TimedBuffer {
  type Timestamp = Long

  object Get

}