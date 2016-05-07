package base.controllers

import play.api.mvc.{Controller, EssentialAction}

abstract class ControllerBase (implicit val env: EnvironmentAll) extends Controller{
  type EA = EssentialAction
}
