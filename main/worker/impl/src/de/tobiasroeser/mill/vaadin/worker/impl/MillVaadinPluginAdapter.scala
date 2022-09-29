package de.tobiasroeser.mill.vaadin.worker.impl

import com.vaadin.flow.plugin.base.{BuildFrontendUtil, PluginAdapterBase, PluginAdapterBuild}
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import de.tobiasroeser.mill.vaadin.worker.MillVaadinConfig
import mill.api.Ctx

import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util
import scala.jdk.CollectionConverters.{SeqHasAsJava, SetHasAsJava}

class MillVaadinPluginAdapter(config: MillVaadinConfig)(implicit ctx: Ctx.Log) extends PluginAdapterBase with PluginAdapterBuild {

  // PluginAdapterBase

  override def applicationProperties(): File = config.applicationPropertiesPath.toIO
  override def eagerServerLoad(): Boolean = config.eagerServerLoad
  override def frontendDirectory(): File = config.frontendPath.toIO
  override def generatedFolder(): File = config.generatedPath.toIO
  override def generatedTsFolder(): File = config.generatedTsPath.toIO
  override def getClassFinder(): ClassFinder = {
    val classpathElements = config.classpath
    BuildFrontendUtil.getClassFinder(classpathElements.map(_.toString()).asJava)
  }
  override def getJarFiles(): util.Set[File] = config.classpath.toSet.filter(_.ext == "jar").map(_.toIO).asJava
  override def isJarProject(): Boolean = true
  override def getUseDeprecatedV14Bootstrapping(): String = "false"
  override def isDebugEnabled(): Boolean = config.debugEnabled
  override def javaSourceFolder(): File = (config.sourcePath.toIO)
  override def javaResourceFolder(): File = config.resourcePath.toIO
  override def logDebug(debugMessage: CharSequence): Unit = ctx.log.debug(debugMessage.toString)
  override def logInfo(infoMessage: CharSequence): Unit = ctx.log.info(infoMessage.toString)
  override def logWarn(warningMessage: CharSequence): Unit = ctx.log.error(warningMessage.toString)
  override def logWarn(warningMessage: CharSequence, throwable: Throwable): Unit =
    ctx.log.error(warningMessage.toString + "\n" + throwable.toString)
  override def logError(warning: CharSequence, e: Throwable): Unit =
    ctx.log.error(warning.toString + "\n" + e.toString)
  override def nodeDownloadRoot(): URI = new URI("https://nodejs.org/dist/")
  override def nodeAutoUpdate(): Boolean = false
  override def nodeVersion(): String = "v16.16.0"
  override def npmFolder(): File = config.npmWorkPath.toIO
  override def openApiJsonFile(): File = (config.buildOutputPath / "generated-resources" / "openapi.json").toIO
  override def pnpmEnable(): Boolean = config.pnpmEnabled
  override def useGlobalPnpm(): Boolean = false
  override def productionMode(): Boolean = config.productionMode
  override def projectBaseDirectory(): Path = config.projectBasePath.toNIO
  override def requireHomeNodeExec(): Boolean = false
  override def servletResourceOutputDirectory(): File = config.resourceOutputPath.toIO
  override def webpackOutputDirectory(): File = config.webpackOutPath.toIO
  override def buildFolder(): String = config.buildFolder
  override def postinstallPackages(): util.List[String] = List().asJava

  // PluginAdapterBuild

  override def frontendResourcesDirectory(): File = config.frontendResourcePath.toIO
  override def generateBundle(): Boolean = true
  override def generateEmbeddableWebComponents(): Boolean = true
  override def optimizeBundle(): Boolean = true
  override def runNpmInstall(): Boolean = true

  override def toString: String = Map(
    "applicationProperties" -> applicationProperties(),
    "eagerServerLoad" -> eagerServerLoad(),
    "frontendDirectory" -> frontendDirectory(),
    "generatedFolder" -> generatedFolder(),
    "generatedTsFolder" -> generatedTsFolder(),
    "getJarFiles" -> getJarFiles(),
    "isJarProject" -> isJarProject(),
    "getUseDeprecatedV14Bootstrapping" -> getUseDeprecatedV14Bootstrapping(),
    "isDebugEnabled" -> isDebugEnabled(),
    "javaSourceFolder" -> javaSourceFolder(),
    "javaResourceFolder" -> javaResourceFolder(),
    "nodeDownloadRoot" -> nodeDownloadRoot(),
    "nodeAutoUpdate" -> nodeAutoUpdate(),
    "nodeVersion" -> nodeVersion(),
    "npmFolder" -> npmFolder(),
    "openApiJsonFile" -> openApiJsonFile(),
    "pnpmEnable" -> pnpmEnable(),
    "useGlobalPnpm" -> useGlobalPnpm(),
    "productionMode" -> productionMode(),
    "projectBaseDirectory" -> projectBaseDirectory(),
    "requireHomeNodeExec" -> requireHomeNodeExec(),
    "servletResourceOutputDirectory" -> servletResourceOutputDirectory(),
    "webpackOutputDirectory" -> webpackOutputDirectory(),
    "buildFolder" -> buildFolder(),
    "postinstallPackages" -> postinstallPackages(),
    "frontendResourcesDirectory" -> frontendResourcesDirectory(),
    "generateBundle" -> generateBundle(),
    "generateEmbeddableWebComponents" -> generateEmbeddableWebComponents(),
    "optimizeBundle" -> optimizeBundle(),
    "runNpmInstall" -> runNpmInstall()
  ).mkString(getClass().getSimpleName() + "(\n  ", ",\n  ", "\n)")
}
