package de.tobiasroeser.mill.vaadin.worker

import mill.api.{Ctx, Logger, PathRef, Result}
import os.Path

trait VaadinToolsWorker {
  //  def prepareFrontend(config: VaadinToolsConfig, destDir: os.Path, ctx: Ctx.Log): Result[PreparedFrontend]
  def prepareFrontend(config: MillVaadinConfig): Unit
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
//  implicit val jsonFormatter: upickle.default.ReadWriter[VaadinToolsConfig] = upickle.default.macroRW
//  implicit val pathFormatter: upickle.default.ReadWriter[Path] = upickle.default
//    .readwriter[String]
//    .bimap(
//      path => path.toIO.getAbsolutePath(),
//      string => Path(string, os.pwd)
//    )
}

case class PreparedFrontend(
    generatedFrontendDir: PathRef,
    classesDir: PathRef
)
object PreparedFrontend {
  implicit val jsonFormatter: upickle.default.ReadWriter[PreparedFrontend] = upickle.default.macroRW
}

trait MillVaadinConfig {
  def debugEnabled: Boolean = false
  def eagerServerLoad: Boolean = false
  def projectBasePath: os.Path
  def buildPath: os.Path
  def vaadinBuildOutputPath: os.Path = buildPath / "classes"
  def resourcePath: os.Path = projectBasePath / "src" / "main" / "resources"
  def applicationPropertiesPath: os.Path = resourcePath / "application.properties"
  def sourcePath: os.Path = projectBasePath / "src" / "main" / "java"
  def frontendPath: os.Path = projectBasePath / "frontend"
  def generatedPath: os.Path = vaadinBuildOutputPath / "generated"
  def generatedTsPath: os.Path = frontendPath / "generated"
  def npmWorkPath: os.Path = projectBasePath
  def productionMode: Boolean = false
  def classpath: Seq[os.Path]
  def log: Logger
  def webpackOutPath: os.Path = vaadinBuildOutputPath / "META_INF" / "VAADIN" / "webapp"
}
