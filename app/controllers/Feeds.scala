package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure}
import models.{ModelPlaceholders => PH, _}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

class Feeds @Inject()(implicit env: EnvironmentAll) extends ControllerBase with Secure[UserSession,User,UserRole,UserPermission] with UserRoles{

  def index: EA = SecureActionByRole(Admin) { implicit request =>
    Ok(views.html.feeds.index())
  }

  def status(id: Long): EA = SecureActionByRole(Admin, Dealer) { implicit request =>
    Ok(views.html.feeds.status())
  }


}