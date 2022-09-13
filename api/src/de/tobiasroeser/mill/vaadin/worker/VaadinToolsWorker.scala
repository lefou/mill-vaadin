package de.tobiasroeser.mill.vaadin.worker

import mill.api.Ctx

trait VaadinToolsWorker {
  def prepareFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit

  def buildFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit

  def cleanFrontend(config: MillVaadinConfig)(implicit ctx: Ctx): Unit
}
