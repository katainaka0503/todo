package model

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import controllers.TodoDao
import scalikejdbc._

import scala.concurrent.{ExecutionContext, Future, blocking}

@Singleton
class TodoDaoImpl @Inject()(actorSystem: ActorSystem) extends SQLSyntaxSupport[Todo] with TodoDao {
  Todos =>

  implicit def myExecutionContext: ExecutionContext = actorSystem.dispatchers.lookup("db-access-context")

  override def schemaName: Option[String] = Some("public")

  override val tableName = "todos"

  override val columns = Seq("id", "title", "description")

  def apply(c: SyntaxProvider[Todo])(rs: WrappedResultSet): Todo = apply(c.resultName)(rs)

  def apply(c: ResultName[Todo])(rs: WrappedResultSet): Todo = new Todo(
    id = rs.get(c.id),
    title = rs.get(c.title),
    description = rs.get(c.description))

  val t = syntax("t")

  override val autoSession = AutoSession

  def findAll()(implicit session: DBSession): Future[Seq[Todo]] = {
    Future {
      blocking {
        withSQL {
          select.from(Todos as t)
        }.map(Todos(t.resultName)).list.apply()
      }
    }

  }

  def findAllByKeyword(keyword: String)(implicit session: DBSession): Future[Seq[Todo]] = {
    val escaped = keyword.replaceAll("%", """\\%""").replaceAll("_", """\\_""")

    val like = s"""%$escaped%"""

    Future {
      blocking {
        withSQL {
          select.from(Todos as t)
            .where.like(t.title, like)
            .or.like(t.description, like)
        }.map(Todos(t.resultName)).list.apply()
      }
    }
  }

  def create(title: String, description: String)(implicit session: DBSession): Future[Todo] = {
    Future {
      blocking {
        val id = withSQL {
          insertInto(Todos).namedValues(
            column.title -> title,
            column.description -> description
          )
        }.updateAndReturnGeneratedKey.apply()

        model.Todo(id, title, description)
      }
    }
  }

  def save(todo: Todo)(implicit session: DBSession): Future[Todo] = {
    Future {
      blocking {
        val num = withSQL {
          update(Todos).set(
            column.title -> todo.title,
            column.description -> todo.description
          ).where.eq(column.id, todo.id)
        }.update.apply()

        if (num == 0) {
          throw new NoSuchElementException()
        } else {
          todo
        }
      }
    }
  }

  def delete(id: Long)(implicit session: DBSession): Future[Unit] = {
    Future {
      blocking {
        val num = withSQL {
          deleteFrom(Todos).where.eq(column.id, id)
        }.update.apply()

        if (num == 0) {
          throw new NoSuchElementException()
        }
      }
    }
  }
}
