package controllers

import model.Todo
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test._
import scalikejdbc.config.DBs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TodoControllerSpec extends FlatSpec with BeforeAndAfter with Matchers with MockitoSugar {

  behavior of "TodoController"

  DBs.setupAll()

  val stubCC: ControllerComponents = Helpers.stubControllerComponents()
  val mockDao: TodoDao = mock[TodoDao]

  val controller = new TodoController(mockDao, stubCC)
  import TodoController.todoFormat

  after{
    reset(mockDao)
  }

  it should "findAllTodos" in {
    val todos = List(Todo(1, "title1", "desc1"), Todo(2, "title2", "desc2"))
    when(mockDao.findAll()(any())).thenReturn(Future.successful(todos))

    val result: Future[Result] = controller.findAll().apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(todos)
  }

  it should "findNothing when todo is not registered" in {
    when(mockDao.findAll()(any())).thenReturn(Future.successful(Seq.empty))

    val result: Future[Result] = controller.findAll().apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(Seq.empty)
  }

  it should "findByKeyword" in {
    val keyword = "keyword"
    val found = List(Todo(1, keyword, "desc1"), Todo(2, "title2", keyword))
    when(mockDao.findAllByKeyword(ArgumentMatchers.eq(keyword))(any())).thenReturn(Future.successful(found))

    val result: Future[Result] = controller.findAllByKeyword(keyword).apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(found)
  }

  it should "createTodo" in {
    val created = Todo(1, "new Todo", "This is new Todo.")
    when(mockDao.create(
      ArgumentMatchers.eq(created.title),
      ArgumentMatchers.eq(created.description))(any())).thenReturn(Future.successful(created))

    val bodyJson = Json.obj(
      "title" -> created.title,
      "description" -> created.description
    )

    val result: Future[Result] = controller.newTodo().apply(FakeRequest().withBody(bodyJson))

    status(result) should be (200)
    contentAsJson(result).as[Todo] should be(created)
  }

  it should "return error when createTodo with empty Json" in {
    val result: Future[Result] = controller.newTodo().apply(FakeRequest().withBody(Json.obj()))

    status(result) should be (400)
    contentAsJson(result) should be(Json.obj("message" -> "Invalid Json"))
  }

  it should "updateTodo" in {
    val modifing = Todo(1, "modify", "This is Modifying.")
    when(mockDao.save(ArgumentMatchers.eq(modifing))(any())).thenReturn(Future.successful(modifing))

    val bodyJson = Json.obj(
      "title" -> modifing.title,
      "description" -> modifing.description
    )

    val result: Future[Result] = controller.updateTodo(modifing.id).apply(FakeRequest().withBody(bodyJson))

    status(result) should be (200)
    contentAsJson(result).as[Todo] should be(modifing)
  }

  it should "return error when updateTodo with empty Json" in {
    val result: Future[Result] = controller.updateTodo(1).apply(FakeRequest().withBody(Json.obj()))

    status(result) should be (400)
    contentAsJson(result) should be(Json.obj("message" -> "Invalid Json"))
  }

  it should "return 404 when todo to update not exists" in {
    val modifing = Todo(1, "modify", "This is Modifying.")
    when(mockDao.save(ArgumentMatchers.eq(modifing))(any())).thenReturn(Future.failed(new NoSuchElementException()))

    val bodyJson = Json.obj(
      "title" -> modifing.title,
      "description" -> modifing.description
    )

    val result: Future[Result] = controller.updateTodo(modifing.id).apply(FakeRequest().withBody(bodyJson))

    status(result) should be (404)
    contentAsJson(result) should be(Json.obj("message" -> "Not found"))
  }

  it should "delete todo" in {
    val id = 1
    when(mockDao.delete(anyLong())(any())).thenReturn(Future.successful(()))

    val result: Future[Result] = controller.deleteTodo(id).apply(FakeRequest())

    status(result) should be (200)
  }

  it should "return 404 when todo to delete not exists" in {
    val id = -1
    when(mockDao.delete(anyLong())(any())).thenReturn(Future.failed(new NoSuchElementException()))

    val result: Future[Result] = controller.deleteTodo(id).apply(FakeRequest())

    status(result) should be (404)
    contentAsJson(result) should be(Json.obj("message" -> "Not found"))
  }

}
