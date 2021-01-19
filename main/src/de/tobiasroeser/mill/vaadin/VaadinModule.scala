package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Source, Worker}
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import de.tobiasroeser.mill.vaadin.worker.{PreparedFrontend, VaadinToolsConfig, VaadinToolsWorker}

import java.net.{URL, URLClassLoader}

trait VaadinModule extends JavaModule {

  def flowServerVersion: T[String] = Versions.buildTimeFlowServerVersion

  /**
   * The node.js version to be used when node.js is installed automatically by
   * Vaadin, for example `"v14.15.4"`. If `None` the Vaadin-default node version
   * is used.
   */
  def nodeVersion: T[Option[String]] = None

  def frontendToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(
      ivy"${Versions.millVaadinWorkerImplIvyDep}",
      ivy"com.vaadin:flow-server:${flowServerVersion()}"
    )
  }

//  def tokenFile: T[PathRef] = T {
//
//
//    val tokenFile = T.dest / ""
//    os.write()
//
//  }

//  case class VaadinFrontend(webpackDir: PathRef)
//  object VaadinFrontend {
//    implicit val jsonFormatter: upickle.default.ReadWriter[VaadinFrontend] = upickle.default.macroRW
//  }

  def frontendDir: Source = T.source(millSourcePath / "frontend")

  def generatorWorkDir: T[PathRef] = T.persistent { PathRef(T.dest) }

  def prepareFrontend: T[PreparedFrontend] = T {
    val dest = T.dest
    frontendToolsWorker().prepareFrontend(vaadinToolsConfig(), dest, T.ctx)
  }

  def vaadinToolsConfig: T[VaadinToolsConfig] = T {
    VaadinToolsConfig(
      moduleDir = millSourcePath,
      npmFolder = millSourcePath,
      nodeVersion = nodeVersion(),
      nodeDownloadRoot = None,
      frontendDir = frontendDir(),
      moduleCompileClasspath = compileClasspath().toSeq,
      moduleRuntimeClasspath = runClasspath(),
      useDeprecatedV14Bootstrapping = true,
      requireHomeNodeExec = false
    )
  }

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
