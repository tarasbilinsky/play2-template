package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure, SecureRequest}
import models.{User, UserRoles, ModelPlaceholders => PH}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

class Application @Inject() (implicit env: EnvironmentAll) extends ControllerBase with Secure with UserRoles {

  override val notAuthorizedPage = views.html.defaultpages.todo() //TODO

  def index:EA = SecureAction{implicit request =>
    val x = request match{
      case r: SecureRequest[AnyContent] => true
      case _ => false
    }
    Ok(views.html.application.index(query(PH.user).seq))
  }

  def login:EA = Action{implicit request =>
    Ok(views.html.application.login())

  }

  def logout:EA = Action{implicit request => Ok(views.html.application.login()).withSession()}

  def auth(userName: String, password: String):EA = Action{ implicit request =>
    query(PH.user,PH.user.name==userName && PH.user.password == password,PH.user,PH.user.permissions,PH.user.roles).one().fold(
      Ok(views.html.application.login())
    ){user=>
      env.userCache.set(user.id.toString,user,userCachingDuration)
      Redirect("/").withSession(loggedInSession(user))
    }
  }

  def reportJSError:EA = TODO //TODO

  def index2:EA = SecureActionByPermissions(Admin)(ViewAll) { implicit request =>
         Ok("Hello " + request.user.id)}

  def index3:EA = SecureActionUser { implicit user => implicit request =>
    Ok("Hello " + user.id)}


}
