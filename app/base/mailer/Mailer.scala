package base.mailer

import javax.inject.Inject
import javax.inject.Singleton

import org.apache.commons.mail.{HtmlEmail, MultiPartEmail}
import play.api.inject.Module
import play.api.{Environment, Configuration}
import play.api.libs.mailer.{MailerClient => PlayMailer, MockMailer, SMTPMailer, Email}

trait Mailer extends PlayMailer

@Singleton
class MailerImpl @Inject()(mailerConfig: Configuration) extends Mailer{

  private lazy val instance: PlayMailer = {
    val path = "play.mailer."
    val mock = mailerConfig.getBoolean(path+"mock").get
    if (mock) {
      new MockMailer()
    } else {
      val smtpHost:String = mailerConfig.getString(path+"host").getOrElse(throw new RuntimeException("Mailer config error play.mailer.host is required"))
      val smtpPort = mailerConfig.getInt(path+"port").getOrElse(25)
      val smtpSsl = mailerConfig.getBoolean(path+"ssl").getOrElse(false)
      val smtpTls = mailerConfig.getBoolean(path+"tls").getOrElse(false)
      val smtpUser = mailerConfig.getString(path+"user")
      val smtpPassword = mailerConfig.getString(path+"password")
      val debugMode = mailerConfig.getBoolean(path+"debug").getOrElse(false)
      val smtpTimeout = mailerConfig.getInt(path+"timeout")
      val smtpConnectionTimeout = mailerConfig.getInt(path+"connectiontimeout")
      new SMTPMailer(smtpHost, smtpPort, smtpSsl, smtpTls, smtpUser, smtpPassword, debugMode, smtpTimeout, smtpConnectionTimeout) {
        override def send(email: MultiPartEmail): String = email.send()
        override def createMultiPartEmail(): MultiPartEmail = new MultiPartEmail()
        override def createHtmlEmail(): HtmlEmail = new HtmlEmail()
      }
    }
  }

  override def send(data: Email): String = instance.send(data)
}

class MailerModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[Mailer].to[MailerImpl]
  )
}

