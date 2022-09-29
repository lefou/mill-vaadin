package de.tobiasroeser.mill.vaadin.worker

import mill.api.PathRef

case class PreparedFrontend(
    generatedFrontendDir: PathRef,
    classesDir: PathRef
)

object PreparedFrontend {
  implicit val jsonFormatter: upickle.default.ReadWriter[PreparedFrontend] = upickle.default.macroRW
}
