package controllers

import model.{Id, Todo}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test._
import scalikejdbc.config.DBs

import scala.concurrent.Future

class TodoControllerSpec extends FlatSpec with BeforeAndAfter with Matchers with MockitoSugar {
  behavior of "TodoController"

  DBs.setupAll()

  val stubCC: ControllerComponents = Helpers.stubControllerComponents()
  val mockDao: TodoDao = mock[TodoDao]

  val controller = new TodoController(mockDao, stubCC)
  import controller.todoFormat

  after{
    reset(mockDao)
  }

  it should "findAllTodos" in {
    val todos = List(Todo(Id(1), "title1", "desc1"), Todo(Id(2), "title2", "desc2"))
    when(mockDao.findAll()(any())).thenReturn(todos)

    val result: Future[Result] = controller.findAll().apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(todos)
  }

  it should "findNothing when todo is not registered" in {
    when(mockDao.findAll()(any())).thenReturn(Seq.empty)

    val result: Future[Result] = controller.findAll().apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(Seq.empty)
  }

  it should "findByKeyword" in {
    val keyword = "keyword"
    val found = List(Todo(Id(1), keyword, "desc1"), Todo(Id(2), "title2", keyword))
    when(mockDao.findAllByKeyword(ArgumentMatchers.eq(keyword))(any())).thenReturn(found)

    val result: Future[Result] = controller.findAllByKeyword(keyword).apply(FakeRequest())

    status(result) should be (200)
    contentAsJson(result).as[List[Todo]] should be(found)
  }
}
