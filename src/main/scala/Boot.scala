import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.util.Timeout
import api.ApiRoutes
import com.typesafe.config.{ConfigFactory, Config}
import streaming.{CurrentReadings, SensorGraph}
import web.FrontendRoutes
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._



object Boot extends App
with SensorGraph
with CurrentReadings
with FrontendRoutes
with ApiRoutes
 {
  override implicit val config: Config = ConfigFactory.load()
  override implicit def system: ActorSystem = ActorSystem("reactive-temperature")
  override implicit val timeout: Timeout = Timeout(10.seconds)
  override val sensorDevicePath = Option(args(0)).getOrElse("/sys/dev/w1/devices/")

  val host = "0.0.0.0"
  val port = 8080

  log.info(s"Found ${sensors.length} sensors: ${sensors.mkString(", ")}")
  log.info("Starting up flow")
  graph.run()

  override implicit val executionContext = system.dispatcher

  override val measurement: ActorRef = lastMeasurement

  log.info(s"HTTP server bound to $host:$port")

  def route = apiRoute ~ frontendRoute

  Http().bindAndHandle(route, host, port)
}
