import java.io.File

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl._
import sensor.{Measurement, SerialNumber, W1ThermSource}
import streaming.FileReloader
import streaming.TimedBuffer.LogStats

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("reactive-temperature")
  implicit val mat = ActorMaterializer()
  import FlowGraph.Implicits._
  import system.log
  

  val sensors = sensor.Sensor.find(new File(args(0)))
  log.info(s"Found ${sensors.length} sensors: ${sensors.mkString(", ")}")

  val lastTwoDays = system.actorOf(streaming.TimedBuffer.props(2.days))

  val g = FlowGraph.closed() { implicit b =>
    val sensorSources =
    sensors.map { sensor =>
      W1ThermSource(FileReloader.source(5.seconds, sensor.device.getPath), sensor)
      }

    val merge = b.add(Merge[(SerialNumber, Double)](sensorSources.size))

    val toMeasurement = b.add {
      Flow[(SerialNumber, Double)].map(pair => Measurement(pair._1, pair._2, System.currentTimeMillis()))
    }

    sensorSources.foreach {
      _ ~> merge }
           merge ~> toMeasurement ~> Sink(ActorSubscriber[Measurement](lastTwoDays))
  }

  log.info("Starting up flow")
  g.run()

  implicit val ctx = system.dispatcher
  system.scheduler.schedule(10.seconds, 10.seconds, lastTwoDays, LogStats)
  
}
