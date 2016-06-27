package base.utils

import com.avaje.ebean.EbeanServerFactory
import com.avaje.ebean.config.ServerConfig

package object test {

  def setUpTestORM(name: String = "mem") = {
    val config:ServerConfig = new ServerConfig
    config.setName(name)
    config.loadTestProperties
    config.setDefaultServer(true)
    config.setRegister(true)
    EbeanServerFactory.create(config)
  }

}
