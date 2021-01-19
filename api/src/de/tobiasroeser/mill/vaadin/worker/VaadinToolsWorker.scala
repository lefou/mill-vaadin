package de.tobiasroeser.mill.vaadin.worker

import mill.api.{Ctx, PathRef, Result}
import os.Path

trait VaadinToolsWorker {
  def prepareFrontend(config: VaadinToolsConfig, destDir: os.Path, ctx: Ctx.Log): Result[PreparedFrontend]
}

case class VaadinToolsConfig(
    moduleDir: Path, // moduleDir
    npmFolder: Path, // moduleDir
    nodeVersion: Option[String],
    nodeDownloadRoot: Option[String],
    frontendDir: PathRef, // moduleDir / frontend
    moduleCompileClasspath: Seq[PathRef],
    moduleRuntimeClasspath: Seq[PathRef],
    useDeprecatedV14Bootstrapping: Boolean,
    requireHomeNodeExec: Boolean,
    productionMode: Boolean,
    generatedFrontendDir: Path,
    javaSourceDir: Seq[PathRef],
    pnpmEnable: Boolean
)

object VaadinToolsConfig {
  implicit val jsonFormatter: upickle.default.ReadWriter[VaadinToolsConfig] = upickle.default.macroRW
  implicit val pathFormatter: upickle.default.ReadWriter[Path] = upickle.default.readwriter[String].bimap(
    path => path.toIO.getAbsolutePath(),
    string => Path(string, os.pwd)
  )
}

case class PreparedFrontend(
    generatedFrontendDir: PathRef,
    classesDir: PathRef
)
object PreparedFrontend {
  implicit val jsonFormatter: upickle.default.ReadWriter[PreparedFrontend] = upickle.default.macroRW
}
