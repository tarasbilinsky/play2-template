package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure, SecureRequest}
import base.models.UserBase
import base.viewHelpers.FormView
import com.avaje.ebean.Ebean
import models.{ModelPlaceholders => PH, _}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

import scala.concurrent.duration.Duration

class Application @Inject() (implicit env: EnvironmentAll) extends ControllerBase with Secure[UserSession,User,UserRole,UserPermission] with UserRoles{

  import views.html.application._

  override val notAuthorizedPage = views.html.defaultpages.todo() //TODO
  override val userCachingDuration  = Duration.Inf

  def index:EA = Action{implicit request =>
    val user: User = query(PH.user,PH.user.id==1,PH.user,PH.user.x,PH.user.name,PH.user.id,PH.user.primaryUserRole,PH.user.roles).one.get
    val form = new FormView(user,"user",props(PH.user,PH.user.x,PH.user.name,PH.user.id,PH.user.primaryUserRole,PH.user.roles))
    Ok(home(form))
  }

  def saveUser:EA = Action{implicit request =>
    val u = FormView.loadFromParams(classOf[User],"user")
    save(u)
    Redirect("/")
  }

  def login:EA = Action{implicit request =>
    Ok("")

  }

  def logout:EA = Action{implicit request =>
    getSession(request).foreach{_.close()}
    Ok("").withSession()
  }

  def auth(userName: String, password: String):EA = Action{ implicit request =>
    query(PH.user,PH.user.name==userName && PH.user.password == password && PH.user.active,PH.user,PH.user.primaryUserRole,PH.user.permissions,PH.user.roles).one().fold(
      Ok("")
    ){user=>
      Redirect("/").withSession(initSession(user))
    }
  }

  def reportJSError:EA = TODO //TODO

  def index2:EA = SecureActionByPermissions(Admin)(ViewAll) { implicit request =>
         Ok("Hello " + request.user.id)}

  def index3:EA = SecureActionUser { implicit user => implicit request =>
    Ok("Hello " + user.id)}


}
