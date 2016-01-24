package streaming

import java.io.{IOException, FileNotFoundException, File}

import akka.actor.{ActorSystem, ActorLogging, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Random

/**
 * Actor continually refreshing a text file at given path,
 * stashing lines not yet delivered in a buffer. 
 * Warning: not suitable for huge files!
 * @param reloadEvery period between consecutive reloads
 * @param path path to open
 */
class FileReloader(val reloadEvery: FiniteDuration, val path: String)
  extends ActorPublisher[String]
  with ActorLogging {

  import FileReloader._
  import akka.stream.actor.ActorPublisherMessage._

  implicit val executionCtx = context.dispatcher

  val refreshMsgSchedule = context.system.scheduler.schedule(
    Random.nextInt(50).millis, // in case of multiple instances they shouldn't start at the same moment
    reloadEvery,
    self,
    ReloadFile)

  import context.dispatcher
  private var linesBuffer: Vector[String] = Vector.empty

  override def receive: Receive = {
    case LineRead(line) if linesBuffer.isEmpty && totalDemand > 0 =>
      onNext(line)

    case LineRead(line) =>
      linesBuffer :+= line

    case Request(_) =>
      deliverBuf()

    case Cancel =>
      refreshMsgSchedule.cancel()
      context.stop(self)

    case ReloadFile =>
      try {
        log.debug(s"Reloading lines from $path")
        readInput(new File(path))
      }
      catch {
        case fe: FileNotFoundException =>
          log.warning(s"couldn't open path $path")
        case ioe: IOException =>
          log.error(s"IOException wile reading file $path")
      }
  }

  def readInput(f: File): Unit = {
    val source = io.Source.fromFile(f)
    val lines = source.getLines()
    lines.foreach(self ! LineRead(_))
    source.close()
  }

  @tailrec final def deliverBuf(): Unit = {
    if (totalDemand > 0) {
      if (totalDemand < Int.MaxValue) {
        val (use, keep) = linesBuffer.splitAt(totalDemand.toInt)
        linesBuffer = keep
        use foreach onNext
      } else {
        val (use, keep) = linesBuffer.splitAt(Int.MaxValue)
        linesBuffer = keep
        use foreach onNext

        deliverBuf()
      }
    }
  }

}

object FileReloader {

  def props(refreshEvery: FiniteDuration, path: String) = Props(classOf[FileReloader], refreshEvery, path)

  case class LineRead(line: String)

  case object ReloadFile

  def source(period: FiniteDuration, path: String)(implicit system: ActorSystem) = {
    Source.actorPublisher[String](FileReloader.props(period, path))
  }
}
