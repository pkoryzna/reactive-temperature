package api

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import api.json.MeasurementProtocol
import streaming.CurrentReadings
import spray.json._

trait ApiRoutes { self: CurrentReadings =>
  import MeasurementProtocol._

  val apiRoute: Route = pathPrefix("api") {
    get {
      (path ("current") | pathEndOrSingleSlash ) {
        complete( currentReadings map {_.values.toJson } )
      }
    }
  }


}
