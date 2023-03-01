package de.tobiasroeser.mill.vaadin

import mill.api.PathRef
import upickle.default.macroRW
import upickle.default.ReadWriter

object VaadinResult {
  implicit val upickleRW: ReadWriter[VaadinResult] = macroRW[VaadinResult]
}

case class VaadinResult(classesDir: PathRef, productionMode: Boolean)
