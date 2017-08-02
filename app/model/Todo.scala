package model

import javax.inject.Singleton

import controllers.TodoDao
import model.Todo.autoSession
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Todo(id: Long, title: String, description: String)

object Todo extends SQLSyntaxSupport[Todo]{
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

  def findAll()(implicit session: DBSession = autoSession): Seq[Todo] = {
    withSQL {
      select.from(Todo as t)
    }.map(Todo(t.resultName)).list.apply()
  }

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo] = {
    val like = s"""%$keyword%"""   //Todo: Escape % and _ !!!
    withSQL{
      select.from(Todo as t)
        .where.like(t.title, like)
        .or.like(t.description, like)
    }.map(Todo(t.resultName)).list.apply()
  }

  def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo = {
    val id = withSQL {
      insertInto(Todo).namedValues(
        column.title -> title,
        column.description ->  description
      )
    }.updateAndReturnGeneratedKey.apply()

    Todo(id, title, description)
  }

  def save(todo: Todo)(implicit session: DBSession = autoSession): Try[Todo] = {
    val num = withSQL {
      update(Todo).set(
        column.title -> todo.title,
        column.description -> todo.description
      ).where.eq(column.id, todo.id)
    }.update.apply()

    if(num == 0){
      Failure(new NoSuchElementException())
    } else {
      Success(todo)
    }
  }

  def delete(id: Long)(implicit session: DBSession = autoSession): Try[Unit] = {
    val num = withSQL {
      deleteFrom(Todo).where.eq(column.id, id)
    }.update.apply()

    if(num == 0){
      Failure(new NoSuchElementException())
    } else {
      Success(())
    }
  }
}

@Singleton
class TodoDaoImpl extends TodoDao {
  override def findAll()(implicit session: DBSession): Seq[Todo] = Todo.findAll()

  override def findAllByKeyword(keyword: String)(implicit session: DBSession): Seq[Todo] = Todo.findAllByKeyword(keyword)

  override def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo = Todo.create(title, description)

  override def save(todo: Todo)(implicit session: DBSession = autoSession): Try[Todo]  = Todo.save(todo)

  override def delete(id: Long)(implicit session: DBSession = autoSession): Try[Unit] = Todo.delete(id)

}
