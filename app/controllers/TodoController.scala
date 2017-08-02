package controllers

import javax.inject._

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import model.Todo.autoSession
import model.{Todo, TodoDaoImpl}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Results.EmptyContent
import play.api.mvc._
import scalikejdbc.{DB, DBSession}

import scala.util.{Failure, Success, Try}

@Singleton
@Api(value = "Todo API")
class TodoController @Inject()(todoDao: TodoDao, cc: ControllerComponents) extends AbstractController(cc) {

  import TodoController.{todoFormat, createDtoFormmat}

  @ApiOperation(
    value = "TodoのAPI",
    produces = "application/json",
    response = classOf[Seq[Todo]]
  )
  def findAll() = Action { implicit request =>
    val all = DB.readOnly { implicit session =>
      todoDao.findAll()
    }
    Ok(Json.toJson(all))
  }

  @ApiOperation(
    httpMethod = "GET",
    value = "Todoをキーワードで検索",
    produces = "application/json",
    response = classOf[Seq[Todo]]
  )
  def findAllByKeyword(@ApiParam(value = "検索対象のTodoが含むキーワード") keyword: String) =
    Action { implicit request =>
      val found = DB.readOnly { implicit session =>
        todoDao.findAllByKeyword(keyword)
      }
      Ok(Json.toJson(found))
    }

  @ApiOperation(
    value = "Todoを新規作成",
    consumes = "application/json",
    produces = "application/json",
    response = classOf[Todo]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "todoDto", value = "作成するTodoのデータ", required = true, dataType = "controllers.TodoDataDto", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid json", response = classOf[MessageDto])
  ))
  def newTodo() = Action(parse.json) { implicit request =>

    val parsed = request.body.validate[TodoDataDto]

    parsed match {
      case JsSuccess(TodoDataDto(title, description), _) => {
        DB.localTx { implicit session =>
          Ok(Json.toJson(todoDao.create(title, description)))
        }
      }
      case JsError(_) => BadRequest(Json.obj("message" -> "Invalid Json"))
    }
  }

  @ApiOperation(
    value = "Todoを更新",
    consumes = "application/json",
    produces = "application/json",
    response = classOf[Todo]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "todoDto", value = "Todoの更新データ", required = true, dataType = "controllers.TodoDataDto", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid json", response = classOf[MessageDto]),
    new ApiResponse(code = 404, message = "Not found", response = classOf[MessageDto])
  ))
  def updateTodo(@ApiParam(value = "更新対象のTodoのId") id: Long) =
    Action(parse.json) { implicit request =>

      val parsed = request.body.validate[TodoDataDto]

      parsed match {
        case JsSuccess(TodoDataDto(title, description), _) => {
          DB.localTx { implicit session =>
            todoDao.save(Todo(id, title, description)) match {
              case Failure(e: NoSuchElementException) => NotFound(Json.obj("message" -> "Not found"))
              case Failure(_) => InternalServerError(Json.obj("message" -> "Some error occured"))
              case Success(todo) => Ok(Json.toJson(todo))
            }
          }
        }
        case JsError(_) => BadRequest(Json.obj("message" -> "Invalid Json"))
      }
    }

  @ApiOperation(
    value = "Todoを削除",
    produces = "application/json",
    response = classOf[EmptyContent]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Not found", response = classOf[MessageDto])
  ))
  def deleteTodo(@ApiParam(value = "削除対象のTodoのId") id: Long) = Action { implicit request =>
    DB.localTx { implicit session =>
      todoDao.delete(id) match {
        case Failure(e: NoSuchElementException) => NotFound(Json.obj("message" -> "Not found"))
        case Failure(_) => InternalServerError(Json.obj("message" -> "Some error occured"))
        case Success(()) => Ok(Json.obj())
      }
    }
  }

}

object TodoController {

  implicit val todoFormat: Format[Todo] = (
    (JsPath \ "id").format[Long] and
      (JsPath \ "title").format[String](maxLength[String](30)) and
      (JsPath \ "description").format[String]
    ) (Todo.apply, unlift(Todo.unapply))

  implicit val createDtoFormmat: Format[TodoDataDto] = (
    (JsPath \ "title").format[String](maxLength[String](30)) and
      (JsPath \ "description").format[String]
    ) (TodoDataDto.apply, unlift(TodoDataDto.unapply))
}

case class MessageDto(message: String)
case class TodoDataDto(title: String, description: String)

@ImplementedBy(classOf[TodoDaoImpl])
trait TodoDao {
  def findAll()(implicit session: DBSession = autoSession): Seq[Todo]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = autoSession): Seq[Todo]

  def create(title: String, description: String)(implicit session: DBSession = autoSession): Todo

  def save(todo: Todo)(implicit session: DBSession = autoSession): Try[Todo]

  def delete(id: Long)(implicit session: DBSession = autoSession): Try[Unit]
}