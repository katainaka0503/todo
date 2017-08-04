package model

import java.util.NoSuchElementException

import org.scalatest.{Matchers, fixture}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc.{DBSession, NamedDB, SQL}

import scala.concurrent.{Await, Future, duration}
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class TodoSpec extends fixture.FlatSpec with Matchers with AutoRollback with GuiceOneAppPerSuite {

  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  override def db = NamedDB('default).toDB
  
  def await[A](future: Future[A]): A = Await.result(future, 500.millis)
  def awaitException[A](future: Future[A]): Throwable = await{
    future.transform{
      case Failure(e: Throwable) => Success(e)
      case Success(_) => Failure(new NoSuchElementException())
    }
  }

  behavior of "Todo"

  DBs.setupAll()

  override def fixture(implicit session: DBSession) {
    await {
      Todo.create("KeywordContains", "description")
      Todo.create("hogehoge", "containsKeyword")
      Todo.create("donotContains", "test")
    }
  }

  it should "search with Keyword" in { implicit session =>
    val keyword = "Keyword"
    val all = await(Todo.findAll())
    val expected = all.filter(todo => todo.title.contains(keyword) || todo.description.contains(keyword))

    await(Todo.findAllByKeyword("Keyword")) should be(expected)
  }

  it should "list all todos" in { implicit session =>
    val all = await(Todo.findAll())
    all.length should be(3)
  }

  it should "create todo" in {implicit session =>
    await(Todo.create("newOne", "This is new Todo item."))

    val all = await(Todo.findAll())

    all.length should be(4)
  }

  it should "update todo" in { implicit session =>
    val created = await(Todo.create("newOne", "This is new Todo item."))
    val modified = created.copy(title = "ModifiedOne")

    await(Todo.save(modified)) should be (modified)

    await(Todo.findAllByKeyword("Modified")).length should be (1)
  }

  it should "return error when update todo not exists" in { implicit session =>
    awaitException(Todo.save(Todo(-1, "not Exist", "hoge"))) shouldBe a [NoSuchElementException]
  }

  it should "delete todo" in { implicit session =>
    val created = await(Todo.create("newOne", "This is new Todo item."))

    await(Todo.delete(created.id)) should be (())
  }

  it should "return error when delete todo not exists" in { implicit session =>
    awaitException(Todo.delete(-1)) shouldBe a [NoSuchElementException]
  }
}
