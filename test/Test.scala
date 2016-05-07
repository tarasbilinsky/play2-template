import models.User
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerTest, PlaySpec}
import play.api.Play
import play.api.inject.guice.GuiceApplicationBuilder
import net.oltiv.scalaebean.EbeanShortcuts._
import net.oltiv.scalaebean.EbeanImplicits._
import net.oltiv.scalaebean.EbeanShortcutsNonMacro._
import play.twirl.api.Html
import play.test.Helpers._
import play.twirl.api.Content
import org.avaje.agentloader
import org.avaje.agentloader.AgentLoader
import org.scalatest.TestData
import play.api.cache.EhCacheModule
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.test.FutureAwaits
import play.mvc.Http.Response
import play.mvc.Result

import org.scalatest._
import org.scalatestplus.play._

import play.api.test._

class ExampleSpec extends PlaySpec with OneAppPerSuite {

  implicit override lazy val app = new GuiceApplicationBuilder().configure(Map("ehcacheplugin" -> "disabled")).build()

  val enhancerOn = AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent","debug=1;packages=base.models.**,models.**")

  "The OneAppPerSuite trait" should {
    "provide an Application" in {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "start the Application" in {
      Play.maybeApplication mustBe Some(app)
    }
  }


  "Ebean" should {
    "provide data" in {

      if (!enhancerOn) {
        fail
      }

      val m: User = new User()
      val v: Html = views.html.index.render(query(m).seq)
      contentAsString(v) must include ("3")
    }
  }
}

class ExampleSpec2 extends PlaySpec with OneServerPerTest with DefaultAwaitTimeout with FutureAwaits{

  override lazy val port: Int = 9888
  override def newAppForTest(testData: TestData) =
  new GuiceApplicationBuilder().disable[EhCacheModule]
    //.router(Router.from {
    //  case GET => Action { Results.Ok("ok") }
    //}
    //)
  .build()

  "The OneServerPerTest trait" must {
    "test server logic" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val myPublicAddress =  s"localhost:$port"
      val testPaymentGatewayURL = s"http://$myPublicAddress"
      // The test payment gateway requires a callback to this server before it returns a result...
      val callbackURL = s"http://$myPublicAddress/callback"
      // await is from play.api.test.FutureAwaits
      val response = await(wsClient.url(testPaymentGatewayURL).withQueryString("callbackURL" -> callbackURL).get())

      response.status mustBe (200)
    }
  }
}