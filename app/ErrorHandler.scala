import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.{InternalServerError, Status}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.obj("message" -> s"A client error occurred: $message"))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError(Json.obj("message" -> exception.getMessage))
    )
  }
}

