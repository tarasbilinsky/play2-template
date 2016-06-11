package base.jobs

import java.time.{Clock, LocalTime}
import java.util.concurrent.TimeUnit

import base.controllers.EnvironmentAll
import base.utils

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}


class Job(nextRunIn: FiniteDuration, every: Duration = Duration.Inf, val key: (AnyRef,Any) = null)(run: =>Unit)(implicit val env: EnvironmentAll){
  private val scheduler = {
    implicit val dispatcher:ExecutionContextExecutor = env.akka.dispatcher
    val s = env.akka.scheduler
    every match {
      case eveyFinite: FiniteDuration => s.schedule(nextRunIn,eveyFinite)(_)
      case _ => s.scheduleOnce(nextRunIn)(_)
    }
  }

  protected def beforeRun():Unit = ()
  protected def afterRun():Unit = ()

  val scheduled = scheduler{

    utils.withExceptionLogging{
      beforeRun
      this.run
      afterRun
    }

  }

}

object CancelPrevious{
  import akka.actor.Cancellable
  lazy val mm :TrieMap[AnyRef,TrieMap[Any,Cancellable]] = new TrieMap[AnyRef, TrieMap[Any,Cancellable]]()
  def putNewAndCancelPrevious(key: (AnyRef,Any), scheduled: Cancellable) = {
    val x = mm.getOrElseUpdate(key._1,new TrieMap[Any,Cancellable])
    x.put(key._2,scheduled).foreach{xx=>
      xx.cancel()
    }
  }
  def remove(key: (AnyRef,Any)) = mm.get(key._1).map(_.remove(key._2))
}

trait CancelPrevious{self:Job =>
  if(key==null) throw new Exception("Job Cancel Previous - Key is required")
  CancelPrevious.putNewAndCancelPrevious(key, scheduled)
  override def afterRun(): Unit = {
    CancelPrevious.remove(key)
  }
}

trait CancelOnAppStop{self:Job =>
  env.lifecycle.addStopHook {() => Future.successful(scheduled.cancel())}
}

class DailyJob(h: Int, m: Int, s:Int, cancelOnAppStop:Boolean = false)(run: =>Unit)(implicit env: EnvironmentAll) extends Job(
  nextRunIn = {
    @inline def secondsFromMidninght(h: Int, m: Int, s:Int)=h*60*60+m*60+s
    val ct = LocalTime.now(Clock.systemUTC())
    val secondsInDay = secondsFromMidninght(24,0,0)
    val currentSecondsFromMidnight = secondsFromMidninght(ct.getHour,ct.getMinute,ct.getSecond)
    val targetSecondsFromMidninght = secondsFromMidninght(h,m,s)
    val diff = targetSecondsFromMidninght - currentSecondsFromMidnight
    val resSeconds = if (diff>0) diff else secondsInDay+diff
    Duration(resSeconds,TimeUnit.SECONDS)
  },
  every = Duration(24,TimeUnit.HOURS)
)(run)

