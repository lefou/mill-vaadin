package de.tobiasroeser.mill.vaadin.worker.impl

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, VaadinToolsWorker}
import mill.api.Ctx

class VaadinToolsWorkerImpl() extends VaadinToolsWorker {

  def prepareFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit = {
    val adapter = new MillVaadinPluginAdapter(config) {

    }
    ctx.log.errorStream.println(s"adapter: ${adapter}")
    // propagate info via System properties and token file
    val tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter);
    BuildFrontendUtil.prepareFrontend(adapter)
  }

  def buildFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit = {
    os.makeDir.all(config.generatedPath)
    val adapter = new MillVaadinPluginAdapter(config)
    ctx.log.errorStream.println(s"adapter: ${adapter}")
    BuildFrontendUtil.runNodeUpdater(adapter)
    BuildFrontendUtil.runFrontendBuild(adapter)
  }

  def cleanFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit = {

  }

}
