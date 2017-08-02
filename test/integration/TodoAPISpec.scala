package integration

import model.Todo
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import scalikejdbc.config.DBs

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class TodoAPISpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterAll {
  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  import controllers.TodoController.todoFormat

  DBs.setupAll()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val baseURL = s"http://localhost:$port/todo"

  val todoListURL = s"$baseURL/list-all"

  def todoSearchURL(keyword: String) = s"$baseURL/list?keyword=$keyword"

  val newTodoURL = s"$baseURL/"

  def updateURL(id: Long) = s"http://localhost:$port/todo/$id"

  def deleteURL(id: Long) = s"$baseURL/$id"


  def createTodo(): WSResponse = Await.result(wsClient.url(newTodoURL).post(Json.obj("title" -> "newOne", "description" -> "This is new one.")), Duration.Inf)

  def deleteTodo(id: Long): WSResponse = Await.result(wsClient.url(deleteURL(id)).delete(), Duration.Inf)


  "TodoAPI" should {
    "list all Todo" in {
      val response = Await.result(wsClient.url(todoListURL).get(), Duration.Inf)
      response.status mustBe 200
    }

    "search Todo by keyword" in {
      val keyword = "keyword"
      val response = Await.result(wsClient.url(todoSearchURL(keyword)).get(), Duration.Inf)
      response.status mustBe 200
    }

    "create new Todo" in {
      val response = createTodo()
      response.status mustBe 200

      deleteTodo(response.json.as[Todo].id)
    }

    "update Todo" in {
      val responseCreated = createTodo()
      val created = responseCreated.json.as[Todo]

      val response = Await.result(wsClient.url(updateURL(created.id)).put(Json.obj("title" -> "modified", "description" -> "modified")), Duration.Inf)
      response.status mustBe 200

      deleteTodo(response.json.as[Todo].id)
    }

    "delete Todo" in {
      val responseCreated = createTodo()
      val id = responseCreated.json.as[Todo].id

      val response = Await.result(wsClient.url(deleteURL(id)).delete(), Duration.Inf)

      response.status mustBe 200
    }
  }
}
