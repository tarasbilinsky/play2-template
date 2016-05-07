package base

import javax.inject._

import base.controllers.EnvironmentAll
import base.utils.Throttle
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.mailer.Email
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import MyConfigImplicit.MyConfig

class ErrorHandler @Inject() (env: EnvironmentAll, sourceMapper: OptionalSourceMapper, router: Provider[Router]) extends DefaultHttpErrorHandler(env.env, env.config, sourceMapper, router) {


  val logger = Logger(this.getClass)


  private def emailWithThrottle(throttle: Throttle)(email: => Email) = mailer.emailWithThrottle(env.mailerClient, throttle)(email)

  val emailServerErrorThrottle = new Throttle(env.config.errorMonitoring.throttle.e500, 1 hour)

  private def emailServerError(request: RequestHeader, exception: UsefulException) = {
    emailWithThrottle(emailServerErrorThrottle) {
      Email(
        s"${env.config.errorMonitoring.subject} 500 $exception.id",
        env.config.errorMonitoring.from,
        Seq(env.config.errorMonitoring.to),
        bodyHtml = Some(
          s"""
              ${request.uri} <br>
              Cookies: ${utils.formatNlToBr(request.cookies.toString)} <br>
              Headers: ${utils.formatNlToBr(request.headers.toString)} <br>
              Exception: <small> ${exception.description} <br>
              ${utils.formatNlToBr(utils.printStackTrace(exception))}
              </small>
           """
        )
      )
    }
  }

  val emailClientErrorThrottle = new Throttle(env.config.errorMonitoring.throttle.e404, 1 hour)

  private def emailClientError(request: RequestHeader, statusCode: Int, message: String) = {
    emailWithThrottle(emailClientErrorThrottle) {
      Email(
        s"${env.config.errorMonitoring.subject} $statusCode",
        env.config.errorMonitoring.from,
        Seq(env.config.errorMonitoring.to),
        bodyHtml = Some(s"${request.uri} <br> $message")
      )
    }
  }


  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    emailServerError(request, exception)
    super.onProdServerError(request, exception)
  }


  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if(env.env.mode==Mode.Prod) {
      emailClientError(request, statusCode, message)
    }
    super.onClientError(request,statusCode,message)
  }
}