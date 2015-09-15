import java.io.File

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import sensor.SerialNumber
import streaming.FileReloader

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("reactive-temperature")
  implicit val mat = ActorMaterializer()


  def fileReloadSource(period: FiniteDuration, path: String): Source[String, Unit] = {
    Source(ActorPublisher[String](system.actorOf(FileReloader.props(period, path))))
  }


  import akka.stream.scaladsl.FlowGraph
  import system.log

  val sensors = sensor.Sensor.find(new File("/Users/patryk/Src/temperature_sensor/src/main/resources/exampleData/"))




  import FlowGraph.Implicits._



  val g = FlowGraph.closed() { implicit b =>
    val sensorSources =
      sensors.map { sensor =>
        fileReloadSource(5.seconds, sensor.device.getPath)
          .filter(_.contains("t=")).map(_.split("\\s").last.replace("t=", "").toDouble)
          .map((sensor.serialNumber, _))
      }

    val merge = b.add(Merge[(SerialNumber, Double)](sensorSources.size))
    sensorSources.zipWithIndex.foreach { case (src, n) => b.addEdge(b.add(src), merge.in(n)) }

    val consoleSink = b.add(Sink.foreach[(SerialNumber, Double)](st => log.info(s"sensor: ${st._1}, temp: ${st._2} C")))

    merge.out ~> consoleSink
  }

  g.run()
  
}
