package model

import org.scalatest.{Matchers, fixture}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc.{DBSession, NamedDB, SQL}

class TodoSpec extends fixture.FlatSpec with Matchers with AutoRollback with GuiceOneAppPerSuite {

  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  override def db = NamedDB('default).toDB

  behavior of "Todo"

  DBs.setupAll()

  override def fixture(implicit session: DBSession) {
    Todo.create("KeywordContains", "description")
    Todo.create("hogehoge", "containsKeyword")
    Todo.create("donotContains", "test")
  }

  it should "search with Keyword" in { implicit session =>
    val keyword = "Keyword"
    val all = Todo.findAll()
    val expected = all.filter(todo => todo.title.contains(keyword) || todo.description.contains(keyword))

    Todo.findAllByKeyword("Keyword") should be(expected)
  }

  it should "list all todos" in { implicit session =>
    Todo.findAll().length should be(3)
  }

  it should "create todo" in {implicit session =>
    Todo.create("newOne", "This is new Todo item.")

    Todo.findAll().length should be(4)
  }

  it should "update todo" in { implicit session =>
    val created = Todo.create("newOne", "This is new Todo item.")
    val modified = created.copy(title = "ModifiedOne")

    Todo.save(modified)

    Todo.findAllByKeyword("Modified").length should be (1)
  }
}
