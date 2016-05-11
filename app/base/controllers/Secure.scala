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
import scala.language.implicitConversions


trait Secure[S<:UserSessionBase[U], U<:UserBase, R<: UserRoleBase, P<:PermissionBase] extends ControllerBase{

  /***
    *  Constants
    */

  val notAuthorizedPage: play.twirl.api.HtmlFormat.Appendable = views.html.defaultpages.unauthorized()
  val userCachingDuration: Duration  = 5 minutes
  private val idInSession = "id"

  /****
    * Secure Actions
    */

  type MRQ[A] = MayBeSecureRequest[A,U]
  type SRQ[A] = SecureRequest[A,U]

  def Action(action: => MRQ[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]):EssentialAction ={
    val ab = new ActionBuilder[MRQ] with ActionTransformer[Request, MRQ] {
      def transform[A](request: Request[A]) = Future.successful {
        new MayBeSecureRequest(getUser(request), request)
      }
    }
    ab(action)
  }
  def SecureAction(action: => SRQ[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]):EssentialAction = {
    val ab = new ActionBuilder[SRQ] {
      def invokeBlock[A](request: Request[A], block: (SRQ[A]) => Future[Result]) = {
        AuthenticatedBuilder(getUser(_), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, U] =>
          block( (new SecureRequest[A,U](authRequest.user, request)).asInstanceOf[SRQ[A]])
        })
      }
    }
    ab(action)
  }

  def SecureActionByRole(roles: R*)(action: => SRQ[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]): EssentialAction = {
    SecureActionByPermissions(roles: _*)()(action)
  }

  def SecureActionByPermissions(roles: R*)(permissions: P*)(action: => SRQ[AnyContent] => Result)(implicit cS: ClassTag[S], cU: ClassTag[U]): EssentialAction = {
    val ab = new ActionBuilder[SRQ] {
      def invokeBlock[A](request: Request[A], block: (SRQ[A]) => Future[Result]) = {
        AuthenticatedBuilder(getUser(roles: _*)(permissions: _*), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, U] =>
          block( (new SecureRequest[A,U](authRequest.user, request)).asInstanceOf[SRQ[A]])
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
    getSession(request).flatMap{s2 => Option(s2.getUser)}
  }

}

class SecureRequest[A, U<:UserBase](val user: U, request: Request[A]) extends WrappedRequest[A](request)

class MayBeSecureRequest[A, U<:UserBase](val user: Option[U], request: Request[A]) extends WrappedRequest[A](request)

object RequestWrapperForTemplates{
  class GenericRequest[A,U](request: Request[A]){
    def getUser[U]:Option[U] = request match {
      case sr:SecureRequest[A,U] => Some(sr.user)
      case mr:MayBeSecureRequest[A,U] => mr.user
      case _ => None
    }
  }
  implicit def requestToGenericRequest[AnyContent,U](request: Request[AnyContent]):GenericRequest[AnyContent,U] = new GenericRequest[AnyContent,U](request)
}





