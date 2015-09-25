import java.io.File

import akka.actor._
import akka.http.ServerSettings
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import sensor.{Measurement, Sensor, SerialNumber, W1ThermSource}
import streaming.LastMeasurementCacheActor.GetLast
import streaming.{LastMeasurementCacheActor, FileReloader}
import web.FrontendRoutes

import scala.concurrent.Future
import scala.concurrent.duration._


object Boot extends App with FrontendRoutes {
  implicit val system = ActorSystem("reactive-temperature")
  implicit val mat = ActorMaterializer()

  import FlowGraph.Implicits._
  import system.log

  implicit val config = ConfigFactory.load()

  implicit val timeout = Timeout(10.seconds)

  val sensors = sensor.Sensor.find(new File(args(0)))
  log.info(s"Found ${sensors.length} sensors: ${sensors.mkString(", ")}")

  val lastMeasurement = system.actorOf(LastMeasurementCacheActor.props)

  val g = FlowGraph.closed() { implicit b =>
    val sensorSources =
      sensors.map { sensor =>
        W1ThermSource(FileReloader.source(5.seconds, sensor.device.getPath), sensor)
      }

    val merge = b.add(Merge[(SerialNumber, Double)](sensorSources.size))

    val toMeasurement = b.add {
      Flow[(SerialNumber, Double)].map { pair =>
        val (serial, temp) = pair
        Measurement(serial, temp, java.time.LocalDateTime.now(),
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
  }


  log.info("Starting up flow")
  g.run()

  implicit val executionContext = system.dispatcher

  override def currentReadings: Future[Map[String, Measurement]] =
    (lastMeasurement ? GetLast).mapTo[Map[SerialNumber, Measurement]].map(_.map { kv =>
      val serialNumber = kv._1
      val lastMeasurement = kv._2
      (Sensor.name(serialNumber).getOrElse(serialNumber.serial), lastMeasurement)
    })

  Http().bindAndHandle(route, "0.0.0.0", 8080)
}
