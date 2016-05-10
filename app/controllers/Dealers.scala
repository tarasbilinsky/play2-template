package controllers

import javax.inject.Inject

import base.controllers.{ControllerBase, EnvironmentAll, Secure}
import models.{User, UserRoles, ModelPlaceholders => PH}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc._

class Dealers @Inject()(implicit env: EnvironmentAll) extends ControllerBase {

  def index: EA = Action { implicit request =>
    Ok(views.html.dealers.index())
  }


}