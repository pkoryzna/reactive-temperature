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
    def find(w1BusDir: File = new File("/sys/bus/w1/"), prefix: String = "20"): List[Sensor] = {

      w1BusDir.listFiles().find(_.getName == "w1_master_slaves") match {
        case Some(slavesFile) =>
          val slavesNames = io.Source.fromFile(slavesFile).getLines()

          val sensors = for {
            serial <- slavesNames if serial.startsWith(prefix)
            slaveFile = new File(w1BusDir, s"devices/$serial/w1_slave")
          } yield Sensor(SerialNumber(serial), slaveFile)

          sensors.toList
        case None => List.empty
      }

    }
  }
}
