package model

import org.scalatest.{BeforeAndAfterAll, Matchers, fixture}
import scalikejdbc.config.DBs
import scalikejdbc.{DB, DBSession, NamedDB, SQL}
import scalikejdbc.scalatest.AutoRollback

class TodoSpec extends fixture.FlatSpec with Matchers with AutoRollback {

  override def db = NamedDB('default).toDB

  behavior of "Todo"

  DBs.setupAll()

  db autoCommit { implicit session =>
    SQL("create table todos( id serial primary key, title varchar(30) not null, description text not null)").execute.apply()
  }

  override def fixture(implicit session: DBSession) {
    SQL("insert into todos(title, description) values (?, ?)").bind("KeywordContains", "description").update.apply()
    SQL("insert into todos(title, description) values (?, ?)").bind("hogehoge", "containsKeyword").update.apply()
    SQL("insert into todos(title, description) values (?, ?)").bind("donotContains", "test").update.apply()
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
}
