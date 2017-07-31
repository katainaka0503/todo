package model

import org.scalatest.{Matchers, fixture}
import scalikejdbc.config.DBs
import scalikejdbc.{DBSession, NamedDB, SQL}
import scalikejdbc.scalatest.AutoRollback

class TodoSpec extends fixture.FlatSpec with Matchers with AutoRollback{
  DBs.setupAll()

  override def db = NamedDB('h2).toDB

  behavior of "Todo"

  override def fixture(implicit session: DBSession) {
    SQL("create table todos( id serial primary key, title varchar(30) not null, description text not null)").execute.apply()
    SQL("insert into todos(title, description) values (?, ?)").bind("KeywordContains", "description").update.apply()
    SQL("insert into todos(title, description) values (?, ?)").bind("hogehoge", "containsKeyword").update.apply()
    SQL("insert into todos(title, description) values (?, ?)").bind("donotContains", "test").update.apply()
  }

  it should "search with Keyword" in { implicit session =>
    Todo.findAllByKeyword("Keyword").length should be(2)
  }
}
