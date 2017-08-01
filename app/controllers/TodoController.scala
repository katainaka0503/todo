package controllers

import javax.inject._

import model.Todo
import model.Todo.autoSession
import play.api._
import play.api.mvc._
import scalikejdbc.DBSession

@Singleton
class TodoController @Inject()(todoDao: TodoDao, cc: ControllerComponents) extends AbstractController(cc) {

}


trait TodoDao {
  def findAll()(implicit session: DBSession = autoSession): Seq[Todo]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo]
}