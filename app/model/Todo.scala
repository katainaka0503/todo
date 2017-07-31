package model

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
}
