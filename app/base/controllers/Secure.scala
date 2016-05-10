package base.controllers

import base.models.{PermissionBase, UserBase, UserRoleBase}
import models.User
import net.oltiv.scalaebean.Shortcuts._
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc.{ActionBuilder, _}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait Secure extends ControllerBase{


  private val idInSession = "id"
  def loggedInSession(user: UserBase):(String,String) = {
    env.userCache.set(user.id.toString,user,userCachingDuration);
    (idInSession,user.id.toString)
  }

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

  def getUser(request: RequestHeader):Option[UserBase] = request.session.get(idInSession).flatMap{
    id => Try(id.toLong) match {
      case Success(idL) =>
        env.userCache.getOrElse(id,userCachingDuration){
          val u = new User
          query(u,u.id==id,u.roles,u.permissions).one
        }
      case Failure(_) => None
    }
  }

  object SecureAction extends ActionBuilder[SecureRequest] {
    def invokeBlock[A](request: Request[A], block: (SecureRequest[A]) => Future[Result]) = {
      AuthenticatedBuilder(getUser(_), _ => Results.Unauthorized(notAuthorizedPage)).authenticate(request, { authRequest: AuthenticatedRequest[A, UserBase] =>
          block(new SecureRequest[A](authRequest.user, request))
      })
    }
  }

}

class SecureRequest[A](val user: UserBase, request: Request[A]) extends WrappedRequest[A](request)


