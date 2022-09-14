package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.{Logger, PathRef}
import mill.define.{Command, Input, Source, Task, Worker}
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, PreparedFrontend, VaadinToolsConfig, VaadinToolsWorker}
import os.Path
import upickle.default.{ReadWriter, macroRW}

import java.net.{URL, URLClassLoader}

case class VaadinResult(classesDir: PathRef, productionMode: Boolean)

object VaadinResult {
  implicit val upickleRW: ReadWriter[VaadinResult] = macroRW[VaadinResult]
}

trait VaadinModule extends JavaModule {

  private val buildPath = millSourcePath / "target"

  def vaadinBuildPath: T[PathRef] = T.persistent {
    val dest = buildPath
    T.log.info(s"vaadin build path: ${dest}")
    PathRef(dest)
  }

  def millVaadinConfig(prodMode: Task[Boolean]): Task[MillVaadinConfig] = T.task {
    val dest = vaadinBuildPath().path
    val config = new MillVaadinConfig {
      override def compatTargetDir: Path = dest
      override def projectBasePath: Path = millSourcePath
      override def buildFolder: String = "target"
      override def classpath: Seq[Path] = vaadinInputRunClasspath().map(_.path)
      override def log: Logger = T.ctx.log
      override def productionMode: Boolean = prodMode()
    }
    config
  }

  def vaadinCleanFrontend(): Command[Unit] = T.command {
    val delPath = buildPath.relativeTo(millSourcePath)
    if (os.exists(buildPath) && delPath.ups == 0 && buildPath != millSourcePath) {
      T.log.errorStream.println(s"Removing ${buildPath}")
      os.remove.all(buildPath)
    }
  }

  def vaadinPrepareFrontend(): Command[VaadinResult] = {
    val config = millVaadinConfig(vaadinProductionMode)
    T.command {
      vaadinPrepareFrontendTask(config)()
    }
  }

  def vaadinPrepareFrontendTask(millVaadinConfig: Task[MillVaadinConfig]): Task[VaadinResult] = T.task {
    val config = millVaadinConfig()
    val worker = vaadinToolsWorker()
    worker.prepareFrontend(config)
    VaadinResult(classesDir = PathRef(config.vaadinBuildOutputPath), config.productionMode)
  }

  def vaadinBuildFrontend(): Command[VaadinResult] = {
    val config = millVaadinConfig(vaadinProductionMode)
    T.command {
      vaadinPrepareFrontend()()
      vaadinBuildFrontendTask(config)()
    }
  }

  def vaadinBuildFrontendTask(millVaadinConfig: Task[MillVaadinConfig]): Task[VaadinResult] = T.task {
    val worker = vaadinToolsWorker()
    val config = millVaadinConfig()
    worker.buildFrontend(config)
    VaadinResult(classesDir = PathRef(config.vaadinBuildOutputPath), config.productionMode)
  }

//  /**
//   * The node.js version to be used when node.js is installed automatically by
//   * Vaadin, for example `"v14.15.4"`. If `None` the Vaadin-default node version
//   * is used.
//   */
//  def nodeVersion: T[Option[String]] = None

  def vaadinProductionMode: Input[Boolean] = T.input {
    Option(sys.props("vaadin.productionMode")).exists(p => p == "true")
  }

  def vaadinToolsIvyDeps: T[Agg[Dep]] = T {
    Agg.from(Versions.workerIvyDeps.map(d => ivy"${d}"))
  }

  def vaadinToolsClasspath: T[Agg[PathRef]] = T {
    resolveDeps(vaadinToolsIvyDeps)()
  }

  def vaadinToolsWorker: Worker[VaadinToolsWorker] = T.worker {
//    val workerDir = generatorWorkDir().path

    val cl =
      new URLClassLoader(
        vaadinToolsClasspath().map(_.path.toIO.toURI().toURL()).toArray[URL],
        getClass().getClassLoader()
      )
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

//  def vaadinToolsDebug: T[Boolean] = T { true }

  def vaadinInputLocalClasspath: T[Seq[PathRef]] = super.localClasspath

  override def localClasspath: T[Seq[PathRef]] = T {
    super.localClasspath() ++ Seq(vaadinBuildFrontend()().classesDir)
  }

  def vaadinInputRunClasspath: T[Seq[PathRef]] = T {
    vaadinInputLocalClasspath() ++ upstreamAssemblyClasspath()
  }

}
