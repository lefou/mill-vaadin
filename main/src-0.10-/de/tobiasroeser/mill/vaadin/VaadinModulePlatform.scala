package de.tobiasroeser.mill.vaadin

import mill.api.PathRef
import mill.scalalib.{Dep, JavaModule}
import mill.{Agg, T}

trait VaadinModulePlatform extends JavaModule {

  def vaadinToolsIvyDeps: T[Agg[Dep]]

  def vaadinToolsClasspath: T[Agg[PathRef]] = T {
    resolveDeps(vaadinToolsIvyDeps)()
  }

}
