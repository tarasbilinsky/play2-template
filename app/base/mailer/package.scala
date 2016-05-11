package base

import base.utils.Throttle
import play.api.Logger
import play.api.libs.mailer.{Email, MailerClient}

import scala.util.{Failure, Success, Try}

package object mailer {
  def emailWithThrottle(mailerClient: MailerClient, throttle: Throttle)(email: => Email) = {
    import Throttle.Result._
    val send = (e: Email) => Try(mailerClient.send(e))
    (throttle.get match {
      case Proceed => send(email)
      case Stop =>
        val emailUpdSubject = email.copy(subject = s"${email.subject} Throttled Last")
        send(emailUpdSubject)

      case OverLimit => Success("no email sent")
    }) match {
      case Success(msgId) =>
      case Failure(e) => Logger.error("Error sending email", e)
    }
  }
}
