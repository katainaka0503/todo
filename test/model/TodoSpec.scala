package model

import java.util.NoSuchElementException

import akka.actor.ActorSystem
import org.scalatest.{Matchers, fixture}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc.{DBSession, NamedDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class TodoSpec extends fixture.FlatSpec with Matchers with AutoRollback with GuiceOneAppPerSuite {

  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  override def db = NamedDB('default).toDB

  def await[A](future: Future[A]): A = Await.result(future, 500.millis)

  def awaitException[A](future: Future[A]): Throwable = await {
    future.transform {
      case Failure(e: Throwable) => Success(e)
      case Success(_) => Failure(new NoSuchElementException())
    }
  }

  val todoDaoImpl = new TodoDaoImpl(ActorSystem.create("test"))

  behavior of "Todo"

  DBs.setupAll()

  override def fixture(implicit session: DBSession) {
    await {
      Future.sequence {
        List(
          todoDaoImpl.create("Keyword%_Contains", "description"),
          todoDaoImpl.create("hogehoge", "contains%_Keyword"),
          todoDaoImpl.create("donotContains", "test"))
      }

    }
  }

  it should "search with Keyword" in { implicit session =>
    val keyword = "%_"
    val all = await(todoDaoImpl.findAll())
    val expected = all.filter(todo => todo.title.contains(keyword) || todo.description.contains(keyword))

    await(todoDaoImpl.findAllByKeyword("%_")) should be(expected)
  }

  it should "list all todos" in { implicit session =>
    val all = await(todoDaoImpl.findAll())
    all.length should be(3)
  }

  it should "create todo" in { implicit session =>
    await(todoDaoImpl.create("newOne", "This is new Todo item."))

    val all = await(todoDaoImpl.findAll())

    all.length should be(4)
  }

  it should "create todo with title 30 charcters" in { implicit session =>
    val created = await(todoDaoImpl.create("あいうえおかきくけこあいうえおかきくけこあいうえおかきくけこ", "This is new Todo item."))
    val found = await(todoDaoImpl.findAllByKeyword("あいうえおかきくけこあいうえおかきくけこあいうえおかきくけこ"))

    Seq(created) should equal(found)
  }

  it should "update todo" in { implicit session =>
    val created = await(todoDaoImpl.create("newOne", "This is new Todo item."))
    val modified = created.copy(title = "ModifiedOne")

    await(todoDaoImpl.save(modified)) should be(modified)

    await(todoDaoImpl.findAllByKeyword("Modified")).length should be(1)
  }

  it should "return error when update todo not exists" in { implicit session =>
    awaitException(todoDaoImpl.save(Todo(-1, "not Exist", "hoge"))) shouldBe a[NoSuchElementException]
  }

  it should "delete todo" in { implicit session =>
    val created = await(todoDaoImpl.create("newOne", "This is new Todo item."))

    await(todoDaoImpl.delete(created.id)) should be(())
  }

  it should "return error when delete todo not exists" in { implicit session =>
    awaitException(todoDaoImpl.delete(-1)) shouldBe a[NoSuchElementException]
  }
}
