package de.tobiasroeser.mill.vaadin.worker.impl

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, VaadinToolsWorker}

class VaadinToolsWorkerImpl() extends VaadinToolsWorker {

  def prepareFrontend(config: MillVaadinConfig): Unit = {
    val adapter = new MillVaadinPluginAdapter(config)
    BuildFrontendUtil.prepareFrontend(adapter)
  }

//
//  def defaultNodeVersion = FrontendTools.DEFAULT_NODE_VERSION
//  def defaultDownloadRoot = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT
//
//  def frontendTools(
//      baseDir: os.Path,
//      alternativeDir: Option[os.Path],
//      nodeVersion: String = defaultNodeVersion,
//      nodeDownloadRoot: String = defaultDownloadRoot
//  ) = new FrontendTools(
//    baseDir.toIO.getAbsolutePath(),
//    () => alternativeDir.map(_.toIO.getAbsolutePath()).orNull,
//    nodeVersion,
//    new URI(nodeDownloadRoot)
//  )
//
//  def prepareFrontend(config: VaadinToolsConfig, destDir: os.Path, ctx: Ctx.Log): Result[PreparedFrontend] = {
//
//    val classesDir = destDir / "classes"
//    val tokenFile = classesDir / "META-INF" / "VAADIN" / "config" / "flow-build-info.json"
//    ctx.log.debug(s"Writing token file: ${tokenFile}")
//    createTokenFile(tokenFile, config)
//
//    val nodeVersion: String = config.nodeVersion.getOrElse(FrontendTools.DEFAULT_NODE_VERSION)
//    val nodeDownloadRoot = URI.create(config.nodeDownloadRoot.getOrElse(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT))
//
//    {
//      // check npm and node setup
//      ctx.log.info("Validation Node and Npm Setup")
//
//      val baseDir: String = config.npmFolder.toIO.getAbsolutePath()
//      val alternativeDirGetter: Supplier[String] = () => FrontendUtils.getVaadinHomeDirectory().getAbsolutePath()
//
//      val tools = new FrontendTools(baseDir, alternativeDirGetter, nodeVersion, nodeDownloadRoot)
//      tools.validateNodeAndNpmVersion()
//    }
//
//    // TODO: THis should be
//    //  val generatedFrontendDir = destDir / "frontend"
//    val generatedFrontendDir = config.generatedFrontendDir
//    os.makeDir.all(generatedFrontendDir)
//
//    val flowResourcesDir = destDir / "flow-resources"
////      config.npmFolder / FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER
//
//    val classFinder = {
//      val regex = Pattern.compile(INCLUDE_FROM_COMPILE_DEPS_REGEX)
//      val entries = config.moduleRuntimeClasspath ++
//        config.moduleCompileClasspath.filter(p => regex.matcher(p.path.last).matches())
//      val urls = entries.map(_.path).distinct.map(_.toNIO.toUri.toURL)
//      new ReflectionsClassFinder(urls: _*)
//    }
//
//    {
//      val builder = new NodeTasks.Builder(
//        classFinder,
//        config.npmFolder.toIO,
//        generatedFrontendDir.toIO,
//        config.frontendDir.path.toIO
//      )
//        .useV14Bootstrap(config.useDeprecatedV14Bootstrapping)
//        .withFlowResourcesFolder(flowResourcesDir.toIO)
//        .createMissingPackageJson(true)
//        .enableImportsUpdate(false)
//        .enablePackagesUpdate(false)
//        .runNpmInstall(false)
//        .withNodeVersion(nodeVersion)
//        .withNodeDownloadRoot(nodeDownloadRoot)
//        .withHomeNodeExecRequired(config.requireHomeNodeExec)
//        .copyResources(
//          config.moduleRuntimeClasspath.map(_.path).filter(p => p.ext.equalsIgnoreCase("jar")).map(_.toIO).toSet.asJava
//        )
//
//      val nodeTasks = builder.build()
//      ctx.log.debug(s"Executing node tasks: ${nodeTasks}")
//      nodeTasks.execute()
//
//    }
//
//    Result.Success(PreparedFrontend(
//      generatedFrontendDir = PathRef(generatedFrontendDir),
//      classesDir = PathRef(classesDir)
//
//    ))
//  }
//
//  /** Additionally include compile-time-only dependencies matching the pattern. */
//  val INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";
//
//  def createTokenFile(file: os.Path, config: VaadinToolsConfig): Unit = {
//    val json = ujson.Obj(
//      InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE -> config.productionMode,
//      InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP -> config.useDeprecatedV14Bootstrapping,
//      InitParameters.SERVLET_PARAMETER_INITIAL_UIDL -> false, // TDOO
//      Constants.NPM_TOKEN -> config.npmFolder.toIO.getAbsolutePath(),
//      Constants.GENERATED_TOKEN -> config.generatedFrontendDir.toIO.getAbsolutePath(),
//      Constants.FRONTEND_TOKEN -> config.frontendDir.path.toIO.getAbsolutePath(),
//      Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN -> config.javaSourceDir.head.path.toIO.getAbsolutePath(),
//      //      Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN
////      Constants.CONNECT_OPEN_API_FILE_TOKEN
////      Constants.CONNECT_GENERATED_TS_DIR_TOKEN
//      InitParameters.SERVLET_PARAMETER_ENABLE_PNPM -> config.pnpmEnable,
//      InitParameters.REQUIRE_HOME_NODE_EXECUTABLE -> config.requireHomeNodeExec
//    )
//    os.write(file, ujson.write(json, indent = 2), createFolders = true)
//
//  }

}
