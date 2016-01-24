package streaming

import java.io.File

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.{Config, ConfigFactory}
import sensor.{Sensor, Measurement, SerialNumber, W1ThermSource}

trait SensorGraph {
  implicit def system: ActorSystem
  implicit val mat = ActorMaterializer()

  val log = system.log

  implicit val config: Config

  implicit def timeout: Timeout
  def sensorDevicePath: String
  def sensorReadPeriod: FiniteDuration = 5.seconds

  lazy val sensors = sensor.Sensor.find(new File(sensorDevicePath))

  val lastMeasurement = system.actorOf(LastMeasurementCacheActor.props)

  /**
   * Graph with Source for each 1-wire device, merged into one flow, connected to side-effecting
   * Sink sending each measurement to a LastMeasurementCacheActor and logging them.
   */
  lazy val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[Unit] =>
    import GraphDSL.Implicits._
    val sensorSources =
    sensors.map { sensor =>
        W1ThermSource(FileReloader.source(sensorReadPeriod, sensor.device.getPath), sensor)
      }

    val merge = b.add(Merge[(SerialNumber, Double)](sensorSources.size))

    val toMeasurement = b.add {
      Flow[(SerialNumber, Double)].map { pair =>
        val (serial, temp) = pair
        Measurement(serial, temp, java.time.ZonedDateTime.now(),
        Sensor.name(serial))
      }
    }

    sensorSources.foreach {
      _ ~> merge
    }
    merge ~> toMeasurement ~> Sink.foreach { m:Measurement =>
      log.info(m.toString)
      lastMeasurement ! m
    }

    ClosedShape
  })
}
