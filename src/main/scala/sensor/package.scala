import java.io.File
import java.nio.file.{Files, Path}

import akka.stream.scaladsl.Source
import com.typesafe.config.Config

import scala.util.Try

package object sensor {
  type Timestamp = Long

  case class SerialNumber(serial: String) extends AnyVal

  case class Sensor(serialNumber: SerialNumber, device: File)

  case class Measurement(serialNumber: SerialNumber, value: Double, timestamp: Timestamp, sensorName: Option[String] = None)

  implicit val TimestampOrd = Ordering.by[Measurement, Timestamp](_.timestamp)

  object Sensor {

    /**
     * Find all 1-wire sensors in given directory, with serial numbers matching given prefix
     */
    def find(w1BusDir: File = new File("/sys/bus/w1/devices/"), prefix: String = ""): List[Sensor] = {

      def listFiles(f: File): List[File] = Option(f.listFiles()).map(_.toList).getOrElse(List.empty)

      for {
        deviceDir <- listFiles(w1BusDir) if deviceDir.getName.startsWith(prefix)
        slaveFile = new File(deviceDir, "w1_slave") if slaveFile.exists()
        sensor = Sensor(SerialNumber(deviceDir.getName), slaveFile)
      } yield sensor

    }

    def name(serial: SerialNumber)(implicit config: Config): Option[String] = {
      Try(config.getConfig("sensor.names").getString(serial.serial)).toOption
    }

  }

  object W1ThermSource {
    def apply[Mat](stringSource: Source[String, Mat], sensor: Sensor): Source[(SerialNumber, Double), Mat] = {
      stringSource.filter(_.contains("t="))
        .map(_.split("\\s").last.replace("t=", "").toDouble / 1000)
        .map((sensor.serialNumber, _))
    }
  }

}
