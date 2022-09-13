package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.{Logger, PathRef}
import mill.define.{Command, Source, Task, Worker}
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, PreparedFrontend, VaadinToolsConfig, VaadinToolsWorker}
import os.Path

import java.net.{URL, URLClassLoader}

trait VaadinModule extends JavaModule {

  private val buildPath = millSourcePath / "target"

  def vaadinBuildPath: T[PathRef] = T.persistent {
    val dest = buildPath
    T.log.info(s"vaadin build path: ${dest}")
    PathRef(dest)
  }

  def millVaadinConfig(): Task[MillVaadinConfig] = T.task {
    val dest = vaadinBuildPath().path
    val config = new MillVaadinConfig {
      override def compatTargetDir: Path = dest
      override def projectBasePath: Path = millSourcePath
      override def buildFolder: String = "target"
      override def classpath: Seq[Path] = runClasspath().map(_.path)
      override def log: Logger = T.ctx.log

    }
    config
  }

  def vaadinClean(): Command[Unit] = T.command {
    val delPath = buildPath.relativeTo(millSourcePath)
    if(os.exists(buildPath) && delPath.ups == 0 && buildPath != millSourcePath) {
      T.log.errorStream.println(s"Removing ${buildPath}")
      os.remove.all(buildPath)
    }
  }

  def vaadinPrepareFrontend(): Command[Unit] = T.command {
    val config = millVaadinConfig()()
    val worker = vaadinToolsWorker()
    worker.prepareFrontend(config)
    ()
  }

  def vaadinBuildFrontend(): Command[Unit] = T.command {
    vaadinPrepareFrontend()()
    val config = millVaadinConfig()()
    val worker = vaadinToolsWorker()
    worker.buildFrontend(config)
    ()
  }

//  /**
//   * The node.js version to be used when node.js is installed automatically by
//   * Vaadin, for example `"v14.15.4"`. If `None` the Vaadin-default node version
//   * is used.
//   */
//  def nodeVersion: T[Option[String]] = None

//  def vaadinProductionMode: T[Boolean] = false

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

  def vaadinToolsDebug: T[Boolean] = T { true }

}
