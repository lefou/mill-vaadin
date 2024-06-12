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
  def frontendResourcePath: os.Path = resourcePath / "META-INF" / "resources" / "frontend"
  def applicationPropertiesPath: os.Path = resourcePath / "application.properties"
  def sourcePath: os.Path = projectBasePath / "src" / "main" / "java"
  def frontendPath: os.Path = projectBasePath / "frontend"
  def generatedPath: os.Path = buildOutputPath / "frontend"
  def generatedTsPath: os.Path = frontendPath / "generated"
  def npmWorkPath: os.Path = projectBasePath
  def productionMode: Boolean = false
  def classpath: Seq[os.Path]
  def webpackOutPath: os.Path = vaadinBuildOutputPath / "META-INF" / "VAADIN" / "webapp"
  def resourceOutputPath: os.Path = vaadinBuildOutputPath / "META-INF" / "VAADIN"
  def pnpmEnabled: Boolean = false

  override def toString(): String = Seq(
    "applicationPropertiesPath" -> applicationPropertiesPath,
    "buildFolder" -> buildFolder,
    "classpath" -> classpath,
    "debugEnabled" -> productionMode,
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
}
