package controllers

import javax.inject._

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import model.{Todo, TodoDaoImpl}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Results.EmptyContent
import play.api.mvc._
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
@Api(
  value = "Todo API",
  protocols = "http"
)
class TodoController @Inject()(todoDao: TodoDao, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  import TodoController.{todoFormat, createDtoFormmat}

  @ApiOperation(
    value = "TodoのAPI",
    produces = "application/json",
    response = classOf[Seq[Todo]]
  )
  def findAll() = Action.async { implicit request =>
    for {
      all <- todoDao.findAll()
    } yield Ok(Json.toJson(all))
  }


  @ApiOperation(
    httpMethod = "GET",
    value = "Todoをキーワードで検索",
    produces = "application/json",
    response = classOf[Seq[Todo]]
  )
  def findAllByKeyword(@ApiParam(value = "検索対象のTodoが含むキーワード") keyword: String) =
    Action.async { implicit request =>
      for {
        found <- todoDao.findAllByKeyword(keyword)
      } yield Ok(Json.toJson(found))
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
  def newTodo() = Action(parse.json).async { implicit request =>

    val parsed = request.body.validate[TodoDataDto]

    parsed match {
      case JsSuccess(TodoDataDto(title, description), _) => {
        for {
          created <- todoDao.create(title, description)
        } yield Ok(Json.toJson(created))

      }
      case JsError(_) => Future.successful(BadRequest(Json.obj("message" -> "Invalid Json")))
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
    Action(parse.json).async { implicit request =>

      val parsed = request.body.validate[TodoDataDto]

      parsed match {
        case JsSuccess(TodoDataDto(title, description), _) => {
          todoDao.save(Todo(id, title, description))
            .map { todo => Ok(Json.toJson(todo)) }
            .recover {
              case e: NoSuchElementException => NotFound(Json.obj("message" -> "Not found"))
            }

        }
        case JsError(_) => Future.successful(BadRequest(Json.obj("message" -> "Invalid Json")))
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
  def deleteTodo(@ApiParam(value = "削除対象のTodoのId") id: Long) = Action.async { implicit request =>
    todoDao.delete(id)
      .map { _ => Ok(Json.obj()) }
      .recover { case e: NoSuchElementException => NotFound(Json.obj("message" -> "Not found")) }

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

  def findAll()(implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[Todo]]

  def findAllByKeyword(keyword: String)(implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[Todo]]

  def create(title: String, description: String)(implicit session: DBSession = AutoSession): Future[Todo]

  def save(todo: Todo)(implicit session: DBSession = AutoSession): Future[Todo]

  def delete(id: Long)(implicit session: DBSession = AutoSession): Future[Unit]
}