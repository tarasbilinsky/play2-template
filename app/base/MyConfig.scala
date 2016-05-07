package base

import play.api.Configuration

object MyConfigImplicit {

  implicit class MyConfig(c: Configuration) {

    lazy val appName = c.getString("application.name").get

    lazy val appVersion = c.getString("application.autoversion").get

    class EmailMonitoringConfigThrottle(val js: Int, val e500: Int, val e404: Int)
    lazy val errorMonitoringThrottle = {
      val g = (s: String) => c.getString("errorMonitoring.throttle." + s).get.toInt
      new EmailMonitoringConfigThrottle(g("js"),g("e500"),g("e404"))
    }
    class ErrorMonitoringConfig(val subject: String, val from: String, val to: String, val throttle: EmailMonitoringConfigThrottle, val jsErrorMonitoringRoute: String)
    lazy val errorMonitoring = {
      val g = (s: String) => c.getString("errorMonitoring." + s).get
      new ErrorMonitoringConfig(g("subject"), g("from"), g("to"),errorMonitoringThrottle,g("jsErrorMonitoringRoute"))
    }

    class AwsConfig(
                     val keyId: String,
                     val key: String,
                     val s3BucketCode: String,
                     val s3BucketEB: String,
                     val s3BucketMain: String,
                     val ebEnvName: String,
                     val ebAppName: String,
                     val disableUploads: Boolean
                     )
    lazy val aws = {
      val g = (s: String) => c.getString("aws." + s).get
      new AwsConfig(
        g("access_key_id"),
        g("secret_access_key"),
        g("bucket.code"),
        g("bucket.EB"),
        g("bucket.main"),
        g("eb.envName"),
        g("eb.appName"),
        c.getBoolean("aws.disableUploads").get
      )
    }

    class GoogleAnalytics(val number: String, val domain: String)
    lazy val googleAnalytics = {
      val g = (s: String) => c.getString("googleAnalytics." + s).get
      new GoogleAnalytics(
        g("number"),
        g("domain")
      )
    }
  }

}
