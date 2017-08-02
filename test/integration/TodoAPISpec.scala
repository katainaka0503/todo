package integration

import model.Todo
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

  import controllers.TodoController.todoFormat

  DBs.setupAll()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val baseURL = s"http://localhost:$port/todo"

  "TodoAPI" should {
    "list all Todo" in {
      val todoListURL = s"$baseURL/list-all"
      val response = Await.result(wsClient.url(todoListURL).get(), Duration.Inf)
      response.status mustBe 200
    }

    "search Todo by keyword" in {
      val keyword = "keyword"
      val todoListURL = s"$baseURL/list?keyword=$keyword"
      val response = Await.result(wsClient.url(todoListURL).get(), Duration.Inf)
      response.status mustBe 200
    }

    "create new Todo" in {
      val newTodoURL = s"$baseURL/"
      val response = Await.result(wsClient.url(newTodoURL).post(Json.obj("title" -> "newOne", "description" -> "This is new one.")), Duration.Inf)

      response.status mustBe 200

      delete(response.json.as[Todo].id.value)
    }

    "update Todo" in {
      val newTodoURL = s"$baseURL/"
      val responseCreated = Await.result(wsClient.url(newTodoURL).post(Json.obj("title" -> "newOne", "description" -> "This is new one.")), Duration.Inf)
      val created = responseCreated.json.as[Todo]

      val updateURL = s"http://localhost:$port/todo/${created.id.value}"

      val response = Await.result(wsClient.url(updateURL).put(Json.obj("title" -> "modified", "description" -> "modified")), Duration.Inf)
      response.status mustBe 200

      delete(response.json.as[Todo].id.value)
    }

    "delete Todo" in {
      val newTodoURL = s"$baseURL/"
      val responseCreated = Await.result(wsClient.url(newTodoURL).post(Json.obj("title" -> "newOne", "description" -> "This is new one.")), Duration.Inf)
      val id = responseCreated.json.as[Todo].id.value
      val deleteTodoURL = s"$baseURL/$id"

      val response = Await.result(wsClient.url(deleteTodoURL).delete(), Duration.Inf)

      response.status mustBe 200
    }

    def delete(id: Long): Unit = {
      val deleteTodoURL = s"$baseURL/$id"

      Await.result(wsClient.url(deleteTodoURL).delete(), Duration.Inf)
    }
  }
}
