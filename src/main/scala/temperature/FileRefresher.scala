package temperature


import java.io.InputStream

import akka.actor.{Props, ActorLogging}
import akka.stream.actor.ActorPublisher

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Random

class FileRefresher(val refreshEvery: FiniteDuration, val path: String)
  extends ActorPublisher[String]
  with ActorLogging {

  import akka.stream.actor.ActorPublisherMessage._
  import FileRefresher._

  private var linesBuffer: Vector[String] = Vector.empty

  import context.dispatcher

  val refreshMsgSchedule = context.system.scheduler.schedule(
    Random.nextInt(50).millis, // not everyone at once eh?
    refreshEvery,
    self,
    ReloadFile)




  override def receive: Receive = {
    case LineRead(line) if linesBuffer.isEmpty && totalDemand > 0 =>
      onNext(line)

    case LineRead(line) =>
      linesBuffer :+= line

    case Request(_) =>
      deliverBuf

    case Cancel =>
      refreshMsgSchedule.cancel()
      context.stop(self)

    case ReloadFile =>
      val stream = Option(this.getClass.getResourceAsStream(path))
      stream match {
        case Some(inputStream) =>
          log.info(s"Reloading lines from $path")
          readInput(inputStream)
        case None =>
          log.info(s"couldn't open path $path")
      }
  }

  def readInput(inputStream: InputStream): Unit = {
    val source = io.Source.fromInputStream(inputStream)
    val lines = source.getLines()
    lines.foreach(self ! LineRead(_))
    source.close()
  }

  @tailrec final def deliverBuf: Unit = {
    if (totalDemand > 0) {
      if (totalDemand < Int.MaxValue) {
        val (use, keep) = linesBuffer.splitAt(totalDemand.toInt)
        linesBuffer = keep
        use foreach onNext
      } else {
        val (use, keep) = linesBuffer.splitAt(Int.MaxValue)
        linesBuffer = keep
        use foreach onNext

        deliverBuf
      }
    }
  }

}

object FileRefresher {

  case class LineRead(line: String)

  case object ReloadFile

  def props(refreshEvery: FiniteDuration, path: String) = Props(classOf[FileRefresher], refreshEvery, path)

}
