import akka.actor.ActorSystem
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.util.Timeout
import org.scalatest.concurrent.{ScalaFutures, Futures}
import org.scalatest.{FlatSpec,Matchers}
import sensor._
import streaming.TimedBuffer
import streaming.TimedBuffer.Get
import concurrent.duration._
import akka.pattern._
import collection.immutable.Seq

import scala.collection.SortedSet

class TimedBufferTest extends FlatSpec with Matchers with ScalaFutures {
  val sys = ActorSystem("test-sys")
  implicit val timeout = Timeout(1.second)
  implicit val patience = PatienceConfig(timeout.duration)


  "TimedBuffer" should "keep all elements within specified time period" in {
    val bufferActor = sys.actorOf(TimedBuffer.props(1.day))

    val measurements = for (n <- 1 to 10) yield Measurement(SerialNumber(s"123-$n"), 1.0, now() + n)
    measurements.foreach(bufferActor ! OnNext(_))

    val elements = (bufferActor ? Get).mapTo[SortedSet[Measurement]]

    whenReady(elements) { elems =>

      elems should equal (SortedSet(measurements:_*))
    }

  }

  it should "drop elements older than specified period" in {
    val bufferActor = sys.actorOf(TimedBuffer.props(1.day))

    val veryOld: Measurement = Measurement(SerialNumber("DROP-ME"), -999.0, now() - 48.hours.toMillis)

    val valid = for (n <- 1 to 10) yield Measurement(SerialNumber(s"123-$n"), 1.0, now() + n)

    valid.foreach(bufferActor ! OnNext(_))
    bufferActor ! OnNext(veryOld)

    val elements = (bufferActor ? Get).mapTo[SortedSet[Measurement]]
    whenReady(elements) { elems =>

      elems should equal (SortedSet(valid:_*))
      elems should not contain veryOld
    }

  }

  def now: () => Timestamp = {
    System.currentTimeMillis
  }
}
