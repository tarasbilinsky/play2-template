package base.controllers

import base.models.{PermissionBase, UserBase, UserRoleBase, UserSessionBase}
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc.{ActionBuilder, _}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait Secure[S<:UserSessionBase[U], U<:UserBase, R<: UserRoleBase, P<:PermissionBase] extends ControllerBase{

  /***
    *  Constants
    */

  val notAuthorizedPage: play.twirl.api.HtmlFormat.Appendable = views.html.defaultpages.unauthorized()
  val userCachingDuration: Duration  = 15 seconds
  private val idInSession = "id"

  /****
    * Secure Actions
    */
  def SecureAction(action: => SecureRequest[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]):EssentialAction = {
    val ab = new ActionBuilder[SecureRequest] {
      def invokeBlock[A](request: Request[A], block: (SecureRequest[A]) => Future[Result]) = {
        AuthenticatedBuilder(getUser(_), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, U] =>
          block(new SecureRequest[A](authRequest.user, request))
        })
      }
    }
    ab(action)
  }

  def SecureActionByRole(roles: R*)(action: => SecureRequest[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]): EssentialAction = {
    SecureActionByPermissions(roles: _*)()(action)
  }

  def SecureActionByPermissions(roles: R*)(permissions: P*)(action: => SecureRequest[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]): EssentialAction = {
    val ab = new ActionBuilder[SecureRequest] {
      def invokeBlock[A](request: Request[A], block: (SecureRequest[A]) => Future[Result]) = {
        AuthenticatedBuilder(getUser(roles: _*)(permissions: _*), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, U] =>
          block(new SecureRequest[A](authRequest.user, request))
        })
      }
    }
    ab(action)
  }

  def SecureActionUser(action: => U => Request[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]): EssentialAction =
    Security.Authenticated(getUser(_),  _ => Results.Unauthorized(notAuthorizedPage))
    { user => Action(request => action(user)(request)) }


  /***
    *
    * Helpers
    */
  def initSession(user: U)(implicit cS: ClassTag[S], cU: ClassTag[U]):(String,String) = {
    val session:S = cS.runtimeClass.getDeclaredConstructor(cU.runtimeClass).newInstance(user) match {case s:S => s; case _ => throw new RuntimeException("unexpected")}
    env.userCache.set(session.getIdString,session,userCachingDuration)
    (idInSession,session.getIdString)
  }

  def getSession(request: RequestHeader)(implicit cS: ClassTag[S]):Option[S] = request.session.get(idInSession).map{id:String =>
    val s:Option[S] = env.userCache.get(id)
    val s2:S  = s.fold{
      val m = cS.runtimeClass.getDeclaredMethod("restore",classOf[String])
      val resS = m.invoke(null,id) match{case s: S=> s; case _ => throw new RuntimeException}//= UserSession.restore(id)
      env.userCache.set(resS.getIdString,resS,userCachingDuration)
      resS
    } (x=>x)
    s2
  }

  def getUser(requiredRole: R*)(requiredPermission: P*)(request: RequestHeader)(implicit cS: ClassTag[S], cU: ClassTag[U]):Option[U] = {
    val user = getUser(request)
    val userWithRolePass = user.filter {user => requiredRole.exists(user.getRoles.contains(_))}
    val userWithRoleAndPermissionsPass = userWithRolePass.filter{user => user.getPermissions.containsAll(scala.collection.JavaConversions.seqAsJavaList(requiredPermission))}
    userWithRoleAndPermissionsPass
  }

  def getUser(request: RequestHeader)(implicit cS: ClassTag[S]):Option[U] = {
    getSession(request).flatMap { s2 =>
      val res: Option[U] = Option(s2.getUser).flatMap { case u: U => Some(u); case _ => None }
      res
    }
  }

}

class SecureRequest[A](val user: UserBase, request: Request[A]) extends WrappedRequest[A](request)


