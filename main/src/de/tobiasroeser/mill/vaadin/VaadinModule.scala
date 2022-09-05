package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.{Logger, PathRef}
import mill.define.{Command, Source, Task, Worker}
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, PreparedFrontend, VaadinToolsConfig, VaadinToolsWorker}
import os.Path

import java.net.{URL, URLClassLoader}

trait VaadinModule extends JavaModule {

  def millVaadinConfig(outputPath: os.SubPath): Task[MillVaadinConfig] = T.task {
    new MillVaadinConfig {
      override def projectBasePath: Path = millSourcePath
      override def buildPath: Path = T.dest
      override def classpath: Seq[Path] = runClasspath().map(_.path)
      override def log: Logger = T.ctx.log
    }
  }

  def vaadinPrepareFrontend(): Command[Unit] = T.command {
    val config = millVaadinConfig(os.sub / "classes")()
    val worker = frontendToolsWorker()
    worker.prepareFrontend(config)
    ()
  }

//  /**
//   * The node.js version to be used when node.js is installed automatically by
//   * Vaadin, for example `"v14.15.4"`. If `None` the Vaadin-default node version
//   * is used.
//   */
//  def nodeVersion: T[Option[String]] = None

//  def vaadinProductionMode: T[Boolean] = false

  def frontendToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(
      ivy"${Versions.millVaadinWorkerImplIvyDep}",
      ivy"${Versions.vaadinFlowPluginBaseDep}"
    )
  }
//
//  def frontendDir: Source = T.source(millSourcePath / "frontend")
//
//  def pnpmEnable: T[Boolean] = false
//
//  def prepareFrontend: T[PreparedFrontend] = T {
//    val dest = T.dest
//    frontendToolsWorker().prepareFrontend(vaadinToolsConfig(), dest, T.ctx)
//  }
//
//  def vaadinToolsConfig: T[VaadinToolsConfig] = T {
//    VaadinToolsConfig(
//      moduleDir = millSourcePath,
//      npmFolder = millSourcePath,
//      nodeVersion = nodeVersion(),
//      nodeDownloadRoot = None,
//      frontendDir = frontendDir(),
//      moduleCompileClasspath = compileClasspath().toSeq,
//      moduleRuntimeClasspath = runClasspath(),
//      useDeprecatedV14Bootstrapping = true,
//      requireHomeNodeExec = false,
//      productionMode = vaadinProductionMode(),
//      // TODO: should be the outcome of the task, but to stay compatible with vaadin tools,
//      // we sill need to have it there
//      generatedFrontendDir = millSourcePath / "target" / "frontend",
//      javaSourceDir = sources(),
//      pnpmEnable = pnpmEnable()
//    )
//  }

  def frontendToolsClasspath: T[Agg[PathRef]] = T {
    resolveDeps(frontendToolsIvyDeps)()
  }

  def frontendToolsWorker: Worker[VaadinToolsWorker] = T.worker {
//    val workerDir = generatorWorkDir().path

    val cl =
      new URLClassLoader(frontendToolsClasspath().map(_.path.toIO.toURI().toURL()).toArray[URL], getClass().getClassLoader())
    val className =
      classOf[VaadinToolsWorker].getPackage().getName() + ".impl." + classOf[VaadinToolsWorker].getSimpleName() + "Impl"
    val impl = cl.loadClass(className)
    val ctr = impl.getConstructor()
    val worker = ctr.newInstance().asInstanceOf[VaadinToolsWorker]
    if (worker.getClass().getClassLoader() != cl) {
      T.ctx()
        .log
        .error("""Worker not loaded from worker classloader.
                 |You should not add the mill-vaadin-worker JAR to the mill build classpath""".stripMargin)
    }
    if (worker.getClass().getClassLoader() == classOf[VaadinToolsWorker].getClassLoader()) {
      T.ctx().log.error("Worker classloader used to load interface and implementation")
    }
    worker
  }

}
