import models.User
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerTest, PlaySpec}
import play.api.Play
import play.api.inject.guice.GuiceApplicationBuilder
import net.oltiv.scalaebean.Shortcuts._
import play.twirl.api.Html
import play.test.Helpers._
import play.twirl.api.Content

import org.scalacheck.Properties
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
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.PropertyChecks

import org.scalatest.junit.JUnitSuite
import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._

import org.scalacheck.Prop.forAll

trait ATestInterface{
  def aTrue:Boolean
}

class ATestWithMock extends FlatSpec with MockFactory //with PropertyChecks
{

  "Test" should "mock" in {
    val aMock: ATestInterface = mock[ATestInterface]

    (aMock.aTrue _).expects().returning(true)
    assert(aMock.aTrue)

  /*
    forAll { (a: String, b:String) =>
      assert(a.length+b.length==(a+b).length)
    }
    */


    // ...
  }
}

object StringSpecification extends Properties("String") {

  property("startsWith") = forAll { (a: String, b: String) =>
    (a+b).startsWith(a)
  }

  property("concatenate") = forAll { (a: String, b: String) =>
    (a+b).length >= b.length
  }

  property("substring") = forAll { (a: String, b: String, c: String) =>
    (a+b+c).substring(a.length, a.length+b.length) == b
  }

}