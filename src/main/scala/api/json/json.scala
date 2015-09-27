package api

import java.time.ZonedDateTime

//import api.json.SerialNumberProtocol._
import sensor.{Measurement, SerialNumber}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

package object json {

  object MeasurementJsonProtocol extends DefaultJsonProtocol {

    implicit val serialNumberFormat = jsonFormat1(SerialNumber.apply)

    implicit val measurementFormat = jsonFormat4(Measurement.apply)

    implicit object TimestampJsonFormat extends RootJsonFormat[ZonedDateTime] {
      override def read(json: JsValue): ZonedDateTime = json match {
        case JsString(str) => ZonedDateTime.parse(str)
        case wtf => throw new IllegalArgumentException(s"Expected a string, got ${wtf.getClass}")
      }

      override def write(obj: ZonedDateTime): JsValue = JsString(obj.toString)
    }

  }

}