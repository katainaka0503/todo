
import javax.inject._

import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent._

@Singleton
class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {


  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(
      InternalServerError(Json.obj("message" -> s"A server error occurred: ${exception.getMessage}"))
    )
  }

  override def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(
      InternalServerError(Json.obj("message" -> s"A server error occurred"))
    )
  }

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(
      BadRequest(Json.obj("message" -> "Bad Request"))
    )
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(
      NotFound(Json.obj("message" -> "Not Found"))
    )
  }

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Results.Status(statusCode)("message" -> "A client error occured")
  }
}

