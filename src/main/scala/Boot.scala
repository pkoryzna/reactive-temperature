import java.io.File

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import sensor.{W1ThermSource, SerialNumber}
import streaming.FileReloader

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("reactive-temperature")
  implicit val mat = ActorMaterializer()
  import FlowGraph.Implicits._
  import system.log
  

  val sensors = sensor.Sensor.find(new File(args(0)))
  log.info(s"Found ${sensors.length} sensors: ${sensors.mkString(", ")}")


  val g = FlowGraph.closed() { implicit b =>
    val sensorSources =
    sensors.map { sensor =>
      W1ThermSource(FileReloader.source(5.seconds, sensor.device.getPath), sensor)
      }

    val merge = b.add(Merge[(SerialNumber, Double)](sensorSources.size))

    val consoleSink = b.add(Sink.foreach[(SerialNumber, Double)](st => log.info(s"sensor: ${st._1}, temp: ${st._2} C")))

    sensorSources.foreach { _ ~> merge }
                                 merge ~> consoleSink
  }

  log.info("Starting up flow")
  g.run()
  
}
