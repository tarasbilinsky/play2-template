package base.jobs

import com.google.inject.AbstractModule
import com.google.inject.name.Names

abstract class JobModule(jobs: List[Class[_ <:Job]]) extends AbstractModule{
  def configure(): Unit = jobs.foreach(j => bind(classOf[Job]).annotatedWith(Names.named(j.getName)).to(j).asEagerSingleton)
}

