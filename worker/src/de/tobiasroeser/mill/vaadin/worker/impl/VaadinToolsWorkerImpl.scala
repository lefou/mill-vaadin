package de.tobiasroeser.mill.vaadin.worker.impl

import com.vaadin.flow.server.frontend.{FrontendTools, FrontendUtils, NodeTasks}
import com.vaadin.flow.server.frontend.installer.NodeInstaller
import de.tobiasroeser.mill.vaadin.worker.{PreparedFrontend, VaadinToolsConfig, VaadinToolsWorker}
import mill.api.{Ctx, PathRef, Result}

import java.net.URI
import java.util.function.Supplier
import java.util.regex.Pattern
import scala.jdk.CollectionConverters._

class VaadinToolsWorkerImpl() extends VaadinToolsWorker {

  def defaultNodeVersion = FrontendTools.DEFAULT_NODE_VERSION
  def defaultDownloadRoot = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT

  def frontendTools(
      baseDir: os.Path,
      alternativeDir: Option[os.Path],
      nodeVersion: String = defaultNodeVersion,
      nodeDownloadRoot: String = defaultDownloadRoot
  ) = new FrontendTools(
    baseDir.toIO.getAbsolutePath(),
    () => alternativeDir.map(_.toIO.getAbsolutePath()).orNull,
    nodeVersion,
    new URI(nodeDownloadRoot)
  )

  def prepareFrontend(config: VaadinToolsConfig, destDir: os.Path, ctx: Ctx.Log): Result[PreparedFrontend] = {
    val nodeVersion: String = config.nodeVersion.getOrElse(FrontendTools.DEFAULT_NODE_VERSION)
    val nodeDownloadRoot = URI.create(config.nodeDownloadRoot.getOrElse(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT))

    {
      // check npm and node setup
      ctx.log.info("Validation Node and Npm Setup")

      val baseDir: String = config.npmFolder.toIO.getAbsolutePath()
      val alternativeDirGetter: Supplier[String] = () => FrontendUtils.getVaadinHomeDirectory().getAbsolutePath()

      val tools = new FrontendTools(baseDir, alternativeDirGetter, nodeVersion, nodeDownloadRoot)
      tools.validateNodeAndNpmVersion()
    }

    val generatedFrontendDir = destDir / "frontend"
    os.makeDir.all(generatedFrontendDir)

    val flowResourcesDir = destDir / "flow-resources"
//      config.npmFolder / FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER

    val classFinder = {
      val regex = Pattern.compile(INCLUDE_FROM_COMPILE_DEPS_REGEX)
      val entries = config.moduleRuntimeClasspath ++
        config.moduleCompileClasspath.filter(p => regex.matcher(p.path.last).matches())
      val urls = entries.map(_.path).distinct.map(_.toNIO.toUri.toURL)
      new ReflectionsClassFinder(urls: _*)
    }

    {
      val builder = new NodeTasks.Builder(
        classFinder,
        config.npmFolder.toIO,
        generatedFrontendDir.toIO,
        config.frontendDir.path.toIO
      )
        .useV14Bootstrap(config.useDeprecatedV14Bootstrapping)
        .withFlowResourcesFolder(flowResourcesDir.toIO)
        .createMissingPackageJson(true)
        .enableImportsUpdate(false)
        .enablePackagesUpdate(false)
        .runNpmInstall(false)
        .withNodeVersion(nodeVersion)
        .withNodeDownloadRoot(nodeDownloadRoot)
        .withHomeNodeExecRequired(config.requireHomeNodeExec)
        .copyResources(
          config.moduleRuntimeClasspath.map(_.path).filter(p => p.ext.equalsIgnoreCase("jar")).map(_.toIO).toSet.asJava
        )

      val nodeTasks = builder.build()
      ctx.log.debug(s"Executing node tasks: ${nodeTasks}")
      nodeTasks.execute()

    }

    Result.Success(PreparedFrontend(generatedFrontendDir = PathRef(generatedFrontendDir)))
  }

  /** Additionally include compile-time-only dependencies matching the pattern. */
  val INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

}
