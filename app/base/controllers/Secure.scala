package base.controllers

import base.models.{PermissionBase, UserBase, UserRoleBase}
import models.{Permission, User}
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc.{ActionBuilder, _}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import net.oltiv.scalaebean.EbeanShortcuts._
import net.oltiv.scalaebean.EbeanImplicits._
import net.oltiv.scalaebean.EbeanShortcutsNonMacro._
import play.twirl.api.{BaseScalaTemplate, Format}
import scala.concurrent.duration._

trait Secure extends ControllerBase{


  def SecureActionUser(action: => UserBase => Request[AnyContent] => Result): EssentialAction =
    Security.Authenticated(getUser(_),  _ => Results.Unauthorized(notAuthorizedPage))
    { user => Action(request => action(user)(request)) }

  val notAuthorizedPage: play.twirl.api.HtmlFormat.Appendable = views.html.defaultpages.unauthorized()
  val userCachingDuration: Duration  = 5 minutes

  def SecureActionByRole(roles: UserRoleBase*)(action: => SecureRequest[AnyContent] => Result): EssentialAction = {
    SecureActionByPermissions(roles: _*)()(action)
  }

  def SecureActionByPermissions(roles: UserRoleBase*)(permissions: PermissionBase*)(action: => SecureRequest[AnyContent] => Result): EssentialAction = {
    val ab = new ActionBuilder[SecureRequest] {
      def invokeBlock[A](request: Request[A], block: (SecureRequest[A]) => Future[Result]) = {
        AuthenticatedBuilder(getUser(roles: _*)(permissions: _*), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, UserBase] =>
          block(new SecureRequest[A](authRequest.user, request))
        })
      }
    }
    ab(action)
  }

  def getUser(requiredRole: UserRoleBase*)(requiredPermission: PermissionBase*)(request: RequestHeader):Option[UserBase] = {
    val user = getUser(request)
    val userWithRolePass = user.filter {user => requiredRole.exists(user.getRoles.contains(_))}
    val userWithRoleAndPermissionsPass = userWithRolePass.filter{user => user.getPermissions.containsAll(scala.collection.JavaConversions.seqAsJavaList(requiredPermission))}
    userWithRoleAndPermissionsPass
  }

  def getUser(request: RequestHeader):Option[UserBase] = request.session.get("id").flatMap{
    id => Try(id.toLong) match {
      case Success(idL) =>
        env.userCache.getOrElse(id,userCachingDuration){
          val u = new User();
          query(u,u.id==id).fetch(props(u,u.roles)).fetch(props(u,u.permissions)).one();
        }
      case Failure(_) => None
    }
  }

  class SecureRequest[A](val user: UserBase, request: Request[A]) extends WrappedRequest[A](request)

  object SecureAction extends ActionBuilder[SecureRequest] {
    def invokeBlock[A](request: Request[A], block: (SecureRequest[A]) => Future[Result]) = {
      AuthenticatedBuilder(getUser(_), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, UserBase] =>
          block(new SecureRequest[A](authRequest.user, request))
      })
    }
  }


}
