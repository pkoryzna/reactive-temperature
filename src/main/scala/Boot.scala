import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Merge, Sink, Source}
import temperature.FileRefresher
import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("reactive-temperature")
  implicit val materializer = ActorMaterializer()

  def fileReloadSource(period: FiniteDuration, path: String): Source[String, Unit] = {
    Source(ActorPublisher[String](system.actorOf(FileRefresher.props(period, path))))
  }
  
  val (period, path) = (10.seconds, "/exampleData/sensorData.txt")


  val goodSensor = fileReloadSource(period, path)

  val validTemperature =
    goodSensor
      .grouped(2)
      .collect {
        case Seq(crcLine, tempLine) if crcLine.contains("YES") => tempLine
      }

  val sensorReadouts =
    validTemperature
      .map { tempLine =>
        val split = tempLine.split("\\s+")
        val temp = split.last.replace("t=", "").toDouble
        val celsius = temp / 1000

        val serial = split.dropRight(1).mkString

        (serial, celsius)
      }

  val sink = Sink.foreach[(String, Double)]( st => println(s"sensor: ${st._1}, temp: ${st._2} C") )

  sensorReadouts.runWith(sink)

}
