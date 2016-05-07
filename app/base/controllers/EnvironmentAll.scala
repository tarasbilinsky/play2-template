package base.controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import base.mailer.Mailer
import play.api.cache.CacheApi
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment, Mode}
import play.cache.NamedCache

class EnvironmentAll @Inject() (
  val env: Environment,
  val config: Configuration,
  val mailerClient: Mailer,
  val lifecycle: ApplicationLifecycle,
  val akka: ActorSystem,
  val wsClient: WSClient,
  @NamedCache("user-cache") val userCache: CacheApi
){
  val isDevMode = env.mode == Mode.Dev
}