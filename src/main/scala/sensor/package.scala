import java.io.File
import java.nio.file.{Files, Path}

import akka.stream.scaladsl.Source

package object sensor {

  case class SerialNumber(serial: String) extends AnyVal

  case class Sensor(serialNumber: SerialNumber, device: File)

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
  }

  object W1ThermSource {
    def apply[Mat](stringSource: Source[String, Mat], sensor: Sensor): Source[(SerialNumber, Double), Mat] = {
      stringSource.filter(_.contains("t="))
        .map(_.split("\\s").last.replace("t=", "").toDouble / 1000)
        .map((sensor.serialNumber, _))
    }
  }

}
