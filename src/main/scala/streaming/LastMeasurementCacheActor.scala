package streaming

import akka.actor.Actor.Receive
import akka.actor._
import sensor.Measurement
import streaming.LastMeasurementCacheActor.GetLast

/**
 * Actor caching the last measurement, per-sensor.
 */
class LastMeasurementCacheActor extends Actor with ActorLogging {

  private var measurements: Map[sensor.SerialNumber, Measurement] = Map.empty

  override def receive: Receive = {
    case m@Measurement(sn, _, _, _) =>
      measurements += (sn -> m)

    case GetLast =>
      sender() ! measurements
  }
}

object LastMeasurementCacheActor {
  case object GetLast

  def props = Props(classOf[LastMeasurementCacheActor])
}
