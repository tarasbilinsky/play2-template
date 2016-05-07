package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure}
import models.{User, UserRoles}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

class Application @Inject() (implicit env: EnvironmentAll) extends ControllerBase with Secure with UserRoles {

  override val notAuthorizedPage = views.html.defaultpages.todo()

  def index:EA = Action{ request =>
    val m = new User()
    val users: Seq[User] = query(m).seq
    val u2: Option[Option[User]] = Some(env.userCache.get("1"))
    Ok(views.html.index(users))
  }

  def reportJSError:EA = Action{implicit request=>
    Ok}

  def index2:EA = SecureActionByPermissions(Admin, Manager)(ViewAll, EditAll) { implicit request =>
         Ok("Hello " + request.user.id)}

  def index3:EA = SecureActionUser { implicit user => implicit request =>
    Ok("Hello " + user.id)}


  def auth:EA = Action{ implicit request =>
    Ok.withSession(("id","1"))
  }
}
