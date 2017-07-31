package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class TodoController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

}
