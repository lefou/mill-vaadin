package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.{Logger, PathRef, experimental}
import mill.define.{Command, Input, Source, Task, Worker}
import mill.scalalib.{Dep, DepSyntax}
import de.tobiasroeser.mill.vaadin.worker.{MillVaadinConfig, VaadinToolsWorker}
import os.Path

import java.net.{URL, URLClassLoader}

trait VaadinModule extends VaadinModulePlatform {

  private val buildPath = millSourcePath / "target"

  def vaadinFrontend: Source = T.source(millSourcePath / "frontend")
  def vaadinVersion: T[String]

  private def vaadinBuildPath: T[PathRef] = T.persistent {
    val dest = buildPath
    val stateFile = dest / ".mill-vaadin.json"
    val prevProdMode = if (os.exists(stateFile)) {
      val state = ujson.read(os.read(stateFile))
      Option(state.obj("productionMode").bool)
    } else None
    val prodMode = vaadinProductionMode()

    if (Some(prodMode) != prevProdMode) {
      T.log.errorStream.println(s"Cleaning vaadin build path ${dest} ...")
      os.remove.all(dest)
      os.makeDir.all(dest)
      os.write(stateFile, ujson.write(ujson.Obj("productionMode" -> ujson.Bool(prodMode)), indent = 2))
    } else {
      T.log.errorStream.println(s"Using vaadin build path: ${dest}")
    }

    PathRef(dest)
  }

  def millVaadinConfig(prodMode: Task[Boolean]): Task[MillVaadinConfig] = T.task {
    val frontend = vaadinFrontend().path
    val res = resources().map(_.path)
    if (res.size != 1) {
      T.log.error(s"Not exactly one resource location defined. Using just the first: ${res.headOption.getOrElse("")}")
    }
    val dest = vaadinBuildPath().path
    val config = new MillVaadinConfig {
      override val compatTargetDir: Path = dest
      override val projectBasePath: Path = millSourcePath
      override val buildFolder: String = "target"
      override val classpath: Seq[Path] = vaadinInputRunClasspath().map(_.path)
      override val productionMode: Boolean = prodMode()
      override val frontendPath: Path = frontend
      override val resourcePath: Path = res.headOption.getOrElse(super.resourcePath)
      override val pnpmEnabled: Boolean = vaadinPnpmEnabled()
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

  def vaadinProductionMode: Input[Boolean] = T.input {
    Option(sys.props("vaadin.productionMode")).exists(p => p == "true")
  }

  override def vaadinToolsIvyDeps: T[Agg[Dep]] = T {
    val version = vaadinVersion()
    val major = version.takeWhile(_.isDigit)

    Agg.from(
      Versions.toMap(s"workerIvyDeps${major}").split("[,]").map(d => ivy"${d.trim()}").toSeq
    ) ++
      Agg(
        // this seems redundant, since the worker itself already depends on this dependency, but that way,
        // we ensure we always use the newer of the both versions used at plugin-compile-time and at plugin-runtime
        ivy"com.vaadin:flow-plugin-base:${version}"
      )
  }

  def vaadinToolsWorker: Worker[VaadinToolsWorker] = T.worker {
    val classLoader =
      new URLClassLoader(
        vaadinToolsClasspath().map(_.path.toIO.toURI().toURL()).iterator.toArray[URL],
        getClass().getClassLoader()
      )
    T.log.debug(s"classLoader: ${classLoader}")
    val implClassName =
      classOf[VaadinToolsWorker].getPackage().getName() + ".impl." + classOf[VaadinToolsWorker].getSimpleName() + "Impl"
    val implClass = classLoader.loadClass(implClassName)
    val worker = implClass.getConstructor().newInstance().asInstanceOf[VaadinToolsWorker]
    if (worker.getClass().getClassLoader() != classLoader) {
      T.log.error(
        """Worker not loaded from worker classloader.
          |You should not add the mill-vaadin-worker JAR to the mill build classpath""".stripMargin
      )
    }
    if (worker.getClass().getClassLoader() == classOf[VaadinToolsWorker].getClassLoader()) {
      T.log.error("Worker classloader used to load interface and implementation")
    }
    worker
  }

  def vaadinInputLocalClasspath: T[Seq[PathRef]] = super.localClasspath

  @experimental
  def vaadinPrepareLocalClasspath: T[Seq[PathRef]] = T {
    vaadinInputLocalClasspath() ++ Seq(vaadinPrepareFrontend()().classesDir)
  }

  @experimental
  def vaadinBuildLocalClasspath: T[Seq[PathRef]] = T {
    vaadinInputLocalClasspath() ++ Seq(vaadinBuildFrontend()().classesDir)
  }

  override def localClasspath: T[Seq[PathRef]] = T {
    vaadinPrepareLocalClasspath()
  }

  def vaadinInputRunClasspath: T[Seq[PathRef]] = T {
    vaadinInputLocalClasspath() ++ upstreamAssemblyClasspath()
  }

  def vaadinPnpmEnabled: T[Boolean] = T(false)
}
