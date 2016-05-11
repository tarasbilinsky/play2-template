package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure, SecureRequest}
import models.{ModelPlaceholders => PH, _}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

import scala.concurrent.duration.Duration

class Application @Inject() (implicit env: EnvironmentAll) extends ControllerBase with Secure[UserSession,User,UserRole,UserPermission] with UserRoles{

  override val notAuthorizedPage = views.html.defaultpages.todo() //TODO
  override val userCachingDuration  = Duration.Inf

  def index:EA = SecureAction{implicit request =>

    import base.controllers.RequestWrapperForTemplates.requestToGenericRequest;
    val u = request.getUser

    Ok(views.html.application.index(query(PH.user).seq))
  }

  def login:EA = Action{implicit request =>
    Ok(views.html.application.login())

  }

  def logout:EA = Action{implicit request =>
    getSession(request).map{_.close()}
    Ok(views.html.application.login()).withSession()
  }

  def auth(userName: String, password: String):EA = Action{ implicit request =>
    query(PH.user,PH.user.name==userName && PH.user.password == password,PH.user,PH.user.permissions,PH.user.roles).one().fold(
      Ok(views.html.application.login())
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
