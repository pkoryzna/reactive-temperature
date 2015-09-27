package streaming

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import sensor.{Sensor, SerialNumber, Measurement}
import streaming.LastMeasurementCacheActor.GetLast

import scala.concurrent.{ExecutionContext, Future}

trait CurrentReadings {
  val lastMeasurementRef: ActorRef
  implicit val timeout: Timeout
  implicit val config: Config
  implicit val executionContext: ExecutionContext

  def currentReadings: Future[Map[String, Measurement]] = {
    (lastMeasurementRef ? GetLast).mapTo[Map[SerialNumber, Measurement]].map(_.map { kv =>
      val serialNumber = kv._1
      val lastMeasurement = kv._2
      (Sensor.name(serialNumber).getOrElse(serialNumber.serial), lastMeasurement)
    })
  }
}
