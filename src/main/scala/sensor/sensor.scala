package sensor

import java.io.File
import java.time.ZonedDateTime

import akka.stream.scaladsl.Flow
import com.typesafe.config.Config

import scala.util.Try


case class SerialNumber(serial: String) extends AnyVal

case class Sensor(serialNumber: SerialNumber, device: String)

case class Measurement(serialNumber: SerialNumber, value: Double, timestamp: ZonedDateTime, sensorName: Option[String] = None)


object Sensor {

  /**
   * Find all 1-wire sensors in given directory, with serial numbers matching given prefix
   */
  def find(w1BusDir: File = new File("/sys/bus/w1/devices/"), prefix: String = ""): List[Sensor] = {

    def listFiles(f: File): List[File] = Option(f.listFiles()).map(_.toList).getOrElse(List.empty)

    for {
      deviceDir <- listFiles(w1BusDir) if deviceDir.getName.startsWith(prefix)
      slaveFile = new File(deviceDir, "w1_slave") if slaveFile.exists()
      sensor = Sensor(SerialNumber(deviceDir.getName), slaveFile.getAbsolutePath)
    } yield sensor

  }

  def name(serial: SerialNumber)(implicit config: Config): Option[String] = {
    Try(config.getConfig("sensor.names").getString(serial.serial)).toOption
  }

}



