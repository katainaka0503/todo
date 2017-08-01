package controllers

import javax.inject._

import model.Todo.autoSession
import model.{Id, Todo}
import play.api.libs.json.Json
import play.api.mvc._
import scalikejdbc.{DB, DBSession}

@Singleton
class TodoController @Inject()(todoDao: TodoDao, cc: ControllerComponents) extends AbstractController(cc) {
  implicit def idFormat[A] = Json.format[Id[A]]

  implicit val todoFormat = Json.format[Todo]


  def findAll() = Action { implicit request =>
    val all = DB.readOnly { implicit session =>
      todoDao.findAll()
    }
    Ok(Json.toJson(all))
  }

  def findAllByKeyword(keyword: String) = Action { implicit request =>
    val found = DB.readOnly { implicit session =>
      todoDao.findAllByKeyword(keyword)
    }
    Ok(Json.toJson(found))
  }
}


trait TodoDao {
  def findAll()(implicit session: DBSession = autoSession): Seq[Todo]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo]
}