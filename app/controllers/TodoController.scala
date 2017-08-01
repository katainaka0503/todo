package controllers

import javax.inject._

import com.google.inject.ImplementedBy
import model.Todo.autoSession
import model.{Id, Todo, TodoDaoImpl}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
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

  def newTodo() = Action(parse.json) { implicit request =>

    implicit val dtoRead: Reads[(String, String)] = (
      (JsPath \ "title").read[String](maxLength[String](30)) and
        (JsPath \ "description").read[String]
      ) ((a: String, b: String) => (a, b))

    val parsed: JsResult[(String, String)] = request.body.validate[(String, String)]

    parsed match {
      case JsSuccess(tuple, _) => {
        DB.localTx { implicit session =>
          Ok(Json.toJson(todoDao.create(tuple._1, tuple._2)))
        }
      }
      case JsError(_) => BadRequest(Json.obj("message" -> "Invalid Json"))
    }
  }

}

@ImplementedBy(classOf[TodoDaoImpl])
trait TodoDao {
  def findAll()(implicit session: DBSession = autoSession): Seq[Todo]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo]

  def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo
}