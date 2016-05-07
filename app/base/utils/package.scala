package base

import java.io.{PrintWriter, StringWriter}

import base.controllers.EnvironmentAll
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import org.apache.commons.lang3.StringEscapeUtils
import base.MyConfigImplicit.MyConfig

package object utils {
  def printStackTrace(e: Exception) = {
    val stack = new StringWriter()
    e.printStackTrace(new PrintWriter(stack))
    stack.toString
  }
  def formatNlToBr(s: String) = s.replace("\n","<br>")

  def formatJs(s: String) = StringEscapeUtils.escapeEcmaScript(s)

  def getS3Client(implicit env: EnvironmentAll) =
    new AmazonS3Client(new BasicAWSCredentials(env.config.aws.keyId, env.config.aws.key))

  def sqlString(s: String) = s""" '${s.replace("'","\\'")}' """

  def withExceptionLogging[A](a: =>A)(implicit env: EnvironmentAll = null):A = {
    try{
      a
    } catch {
      case e:Throwable =>
        play.api.Logger.error("Error",e)
        throw e
    }
  }
}
