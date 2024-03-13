package de.tobiasroeser.mill.vaadin.worker

import mill.api.Logger

trait MillVaadinConfig {
  def compatTargetDir: os.Path

  def debugEnabled: Boolean = false
  def eagerServerLoad: Boolean = false
  def projectBasePath: os.Path
  def buildFolder: String
  def buildOutputPath: os.Path = compatTargetDir
  def vaadinBuildOutputPath: os.Path = buildOutputPath / "classes"
  def resourcePath: os.Path = projectBasePath / "src" / "main" / "resources"
  /** Defines the project frontend directory from where resources should be copied from for use with webpack. */
  def frontendResourcePath: os.Path = resourcePath / "META-INF" / "resources" / "frontend"
  def applicationPropertiesPath: os.Path = resourcePath / "application.properties"
  def sourcePath: os.Path = projectBasePath / "src" / "main" / "java"
  def frontendPath: os.Path = projectBasePath / "frontend"
  @deprecated("Not used since Vaadin 24", "mill-vaadin after 0.0.4")
  def generatedPath: os.Path = buildOutputPath / "frontend"
  def generatedTsPath: os.Path = frontendPath / "generated"
  def npmWorkPath: os.Path = projectBasePath
  @deprecated("Not used since Vaadin 24.", "mill-vaadin after 0.0.4")
  def productionMode: Boolean = false
  def classpath: Seq[os.Path]
  def webpackOutPath: os.Path = vaadinBuildOutputPath / "META-INF" / "VAADIN" / "webapp"
  def resourceOutputPath: os.Path = vaadinBuildOutputPath / "META-INF" / "VAADIN"
  def pnpmEnabled: Boolean = false

  /** Instructs to use globally installed pnpm tool or the default supported pnpm version. */
  def bunEnabled: Boolean = false
//  /** Enable skip of dev bundle rebuild if a dev bundle exists. */
//  def skipDevBundleRebuild = false
//  def isFrontendHotdeploy: Boolean = false

  override def toString(): String = Seq(
    "applicationPropertiesPath" -> applicationPropertiesPath,
    "buildFolder" -> buildFolder,
    "classpath" -> classpath,
    "debugEnabled" -> debugEnabled,
    "productionMode" -> productionMode,
    "eagerServerLoad" -> eagerServerLoad,
    "frontendPath" -> frontendPath,
    "frontendResourcePath" -> frontendResourcePath,
    "generatedPath" -> generatedPath,
    "generatedTsPath" -> generatedTsPath,
    "npmWorkPath" -> npmWorkPath,
    "productionMode" -> productionMode,
    "projectBasePath" -> projectBasePath,
    "sourcePath" -> sourcePath,
    "resourceOutputPath" -> resourceOutputPath,
    "resourcePath" -> resourcePath,
    "vaadinBuildOutputPath" -> vaadinBuildOutputPath,
    "webpackOutPath" -> webpackOutPath
  ).mkString(
    s"""${getClass.getSimpleName}(
       |  """.stripMargin,
    ",\n  ",
    "\n)"
  )

  /** Setting this to true will run npm ci instead of npm install when using npm. If using `pnpm`` or `bun`, the installation will be run with `--frozen-lockfile`` parameter. This makes sure that the package lock file will not be overwritten. */
  def ciBuild: Boolean = false

}
