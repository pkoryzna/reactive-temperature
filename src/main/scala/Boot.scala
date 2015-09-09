import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object Boot extends App {
  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val fakeData = List(
    "11 22 33 44 55 66 77 88 : crc=d8 YES",
    "11 22 33 44 55 66 77 88 t=26125"
  )

  val theSource = Source(fakeData)

  val validTemperature =
    theSource
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
