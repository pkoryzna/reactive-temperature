import java.time.ZoneOffset

import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Route
import sensor.Measurement
import streaming.CurrentReadings

import scala.concurrent.{ExecutionContext, Future}

package object web {

  trait Routes {
    def route: Route
  }

  trait FrontendRoutes extends Routes {
    self: CurrentReadings =>
    import akka.http.scaladsl.server.Directives._

    implicit val marshaller = PredefinedToEntityMarshallers.stringMarshaller(MediaTypes.`text/html`)

    def currentReadings: Future[Map[String, Measurement]]

    def route = pathEndOrSingleSlash {
      get {
        complete {
          currentReadings map { FrontendView.overview(_).render }
        }
      }
    }
  }

  object FrontendView {

    import scalatags.Text.all._

    def readingsFragment(current: Map[String, Measurement]) = {
      for {
        (name, reading) <- current.toList
      } yield div(h2(name), p(s"${reading.value} deg C"), p(small(s"Last reading: ${reading.timestamp}")))
    }

    def overview(current: Map[String, Measurement]) = html(
      head(
        scalatags.Text.tags2.title("test")
      ),
      body(
        h1("Sensor status"),
        div(
          readingsFragment(current): _*
        )
      )
    )
  }

}
