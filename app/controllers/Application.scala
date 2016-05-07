package controllers

import javax.inject.Inject

import akka.util.ByteString
import base.controllers.{ControllerBase, EnvironmentAll, Secure}
import base.models.UserBase
import models.{User, UserRoles}
import net.oltiv.scalaebean.EbeanShortcuts._
import net.oltiv.scalaebean.EbeanImplicits._
import net.oltiv.scalaebean.EbeanShortcutsNonMacro._
import play.api.libs.streams.Accumulator
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._

import scala.concurrent.Future

class Application @Inject() (implicit env: EnvironmentAll) extends ControllerBase with Secure with UserRoles {

  override val notAuthorizedPage = views.html.defaultpages.todo()

  def index:EA = Action{ request =>
    val m = new User()
    val users: Seq[User] = query(m,m.id!=null,m.$).seq
    Ok(views.html.index(users))
  }

  def reportJSError:EA = Action{implicit request=>
    ???;Ok}

  def index2:EA = SecureActionByPermissions(Admin, Manager)(ViewAll, EditAll) { implicit request =>
         Ok("Hello " + request.user.id)}

  def index3:EA = SecureActionUser { implicit user => implicit request =>
    Ok("Hello " + user.id)}


  def auth:EA = Action{ implicit request =>
    Ok.withSession(("id","1"))
  }
}
