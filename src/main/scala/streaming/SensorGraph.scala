package streaming

import java.io.File

import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.util.Timeout
import com.typesafe.config.Config
import sensor.Measurement

import scala.concurrent.duration._

trait SensorGraph {
  implicit def system: ActorSystem
  implicit val mat = ActorMaterializer()

  val log = system.log

  implicit val config: Config

  implicit def timeout: Timeout
  def sensorDevicePath: String
  def sensorReadPeriod: FiniteDuration = 2.seconds

  lazy val sensors = sensor.Sensor.find(new File(sensorDevicePath))

  val lastMeasurement = system.actorOf(LastMeasurementCacheActor.props)

  /**
   * Graph spawning an actor for each 1-wire device, each sending to actorRef source,
   * connected to side-effecting Sink sending each measurement to a LastMeasurementCacheActor.
   */
  lazy val graph = {
    val sensorActors = sensors.map(s => system.actorOf(SensorReaderActor.props(sensorReadPeriod, s)))

    Source.actorRef[Measurement](200, OverflowStrategy.dropTail)
      .mapMaterializedValue(ref => sensorActors.foreach(_ ! ref))
      .alsoTo(Sink.foreach(m => log.info(m.toString)))
      .to(Sink.actorRef(lastMeasurement, PoisonPill))
  }
}
