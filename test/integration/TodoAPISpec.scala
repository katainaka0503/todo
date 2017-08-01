package integration

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import scalikejdbc.config.DBs

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TodoAPISpec extends PlaySpec with GuiceOneServerPerSuite {
  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  DBs.setupAll()

  "TodoAPI" should {
    "test server logic" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val todoListURL = s"http://localhost:$port/todo/list-all"
      val response = Await.result(wsClient.url(todoListURL).get(), Duration.Inf)
      response.status mustBe 200
    }

    "create new Todo" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val newTodoURL = s"http://localhost:$port/todo/"
      val response = Await.result(wsClient.url(newTodoURL).post(Json.obj("title" -> "newOne", "description" -> "This is new one.")), Duration.Inf)
      response.status mustBe 200
    }
  }
}
