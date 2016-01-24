package streaming

import java.io.{IOException, FileNotFoundException, File}
import java.time.ZonedDateTime

import akka.actor._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import sensor.{Measurement, Sensor}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Random

/**
 * Actor continually refreshing a text file at given sensor's device path,
 * stashing measurements not yet delivered in a buffer.
 * @param reloadEvery period between consecutive reloads
 * @param sensor sensor to monitor
 */
class SensorReaderActor(val reloadEvery: FiniteDuration, val sensor: Sensor) extends Actor with ActorLogging {

  import SensorReaderActor._

  implicit val executionCtx = context.dispatcher

  val refreshMsgSchedule = context.system.scheduler.schedule(
    Random.nextInt(50).millis, // in case of multiple instances they shouldn't start at the same moment
    reloadEvery,
    self,
    ReloadFile)

  private var linesBuffer: Vector[Measurement] = Vector.empty
  private var subscriber: Option[ActorRef] = None

  override def receive: Receive = {

    case r: ActorRef =>
      subscriber = Some(r)
      linesBuffer.foreach(measurement => subscriber.foreach(_ ! measurement))
      linesBuffer = Vector.empty

    case LineRead(line) if subscriber.isDefined =>
      for {
        s <- subscriber
        m <- parseMeasurement(line)
      } s ! m
      
    case LineRead(line) =>
      linesBuffer ++= parseMeasurement(line)

    case ReloadFile =>
      val path: String = sensor.device
      try {
        log.debug(s"Reloading lines from $path")
        readInput(new File(path))
      }
      catch {
        case fe: FileNotFoundException =>
          log.warning(s"couldn't open path $path")
        case ioe: IOException =>
          log.error(s"IOException while reading file $path")
      }
  }

  def readInput(f: File): Unit = {
    val source = io.Source.fromFile(f)
    val lines = source.getLines()
    lines.foreach(self ! LineRead(_))
    source.close()
  }


  def parseMeasurement(l: String): Option[Measurement] = {
    Option(l).filter(_.contains("t="))
      .map(_.split("\\s").last.replace("t=", "").toDouble / 1000)
      .map(temp => Measurement(sensor.serialNumber, temp, ZonedDateTime.now(), None))
  }
}

object SensorReaderActor {

  def props(refreshEvery: FiniteDuration, sensor: Sensor) = Props(classOf[SensorReaderActor], refreshEvery, sensor)

  case class LineRead(line: String)

  case object ReloadFile

}

