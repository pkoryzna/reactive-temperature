package streaming

import akka.actor.{ActorLogging, Props}
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, RequestStrategy, WatermarkRequestStrategy}
import com.typesafe.config.ConfigFactory
import sensor.{Sensor, Measurement, SerialNumber, Timestamp}
import streaming.TimedBuffer.{Get, LogStats}

import scala.collection.immutable.SortedSet
import scala.concurrent.duration.FiniteDuration

class TimedBuffer(keepLast: FiniteDuration) extends ActorSubscriber with ActorLogging {

  import sensor.TimestampOrd

  var recorded = SortedSet.empty[Measurement]

  implicit val config = ConfigFactory.load()

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(20)

  override def receive: Receive = {
    case ActorSubscriberMessage.OnNext(m: Measurement) =>
      recorded = (recorded + m).filter(recordedAfter(oldestAllowed(System.currentTimeMillis())))

    case Get =>
      sender() ! recorded

    case LogStats =>
      val stats = recorded.groupBy(_.serialNumber).foreach { e =>
        val (SerialNumber(sn), measurements) = e
        log.info(s"sensor ${Sensor.name(e._1).getOrElse(sn)}, recorded ${measurements.size}, last: ${measurements.last.value} deg C")
      }


  }

  def recordedAfter(earliestAllowed: Timestamp)(elem: Measurement) = {
    elem.timestamp > earliestAllowed
  }

  def oldestAllowed(mostRecent: Timestamp) =
    mostRecent - keepLast.toMillis

}

object TimedBuffer {

  def props(keepLast: FiniteDuration) = Props(classOf[TimedBuffer], keepLast)

  case object Get

  case object LogStats

}