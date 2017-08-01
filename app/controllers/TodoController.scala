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

  implicit val createDtoFormmat : Format[CreateDto] = (
    (JsPath \ "title").format[String](maxLength[String](30)) and
      (JsPath \ "description").format[String]
  )(CreateDto.apply, unlift(CreateDto.unapply))


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

    val parsed = request.body.validate[CreateDto]

    parsed match {
      case JsSuccess(CreateDto(title, description), _) => {
        DB.localTx { implicit session =>
          Ok(Json.toJson(todoDao.create(title, description)))
        }
      }
      case JsError(_) => BadRequest(Json.obj("message" -> "Invalid Json"))
    }
  }

}

case class CreateDto(title: String, description: String)

@ImplementedBy(classOf[TodoDaoImpl])
trait TodoDao {
  def findAll()(implicit session: DBSession = autoSession): Seq[Todo]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo]

  def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo
}