package model

import javax.inject.Singleton

import controllers.TodoDao
import model.Todo.autoSession
import scalikejdbc._

case class Todo(id: Id[Todo], title: String, description: String)

object Todo extends SQLSyntaxSupport[Todo]{
  override def schemaName: Option[String] = Some("public")

  override val tableName = "todos"

  override val columns = Seq("id", "title", "description")

  def apply(c: SyntaxProvider[Todo])(rs: WrappedResultSet): Todo = apply(c.resultName)(rs)
  def apply(c: ResultName[Todo])(rs: WrappedResultSet): Todo = new Todo(
    id = new Id(rs.get(c.id)),
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

    Todo(Id(id.toLong), title, description)
  }

  def save(todo: Todo)(implicit session: DBSession = autoSession): Todo = {
    withSQL {
      update(Todo).set(
        column.title -> todo.title,
        column.description -> todo.description
      ).where.eq(column.id, todo.id.value)
    }.update.apply()

    todo
  }
}

@Singleton
class TodoDaoImpl extends TodoDao {
  override def findAll()(implicit session: DBSession): Seq[Todo] = Todo.findAll()

  override def findAllByKeyword(keyword: String)(implicit session: DBSession): Seq[Todo] = Todo.findAllByKeyword(keyword)

  override def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo = Todo.create(title, description)
}
