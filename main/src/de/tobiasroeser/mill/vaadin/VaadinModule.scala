package de.tobiasroeser.mill.vaadin

import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Source, Worker}
import mill.scalalib.{Dep, DepSyntax, JavaModule}
import de.tobiasroeser.mill.vaadin.worker.VaadinToolsWorker

import java.net.{URL, URLClassLoader}

trait VaadinModule extends JavaModule {

  def vaadinVersion: T[String]

  def flowServerVersion: T[String] = Versions.buildTimeMillVersion

  def nodeVersion: T[Option[String]] = None

  def frontendToolsIvyDeps: T[Agg[Dep]] = T {
    Agg(
      ivy"${Versions.millVaadinWorkerImplIvyDep}",
      ivy"com.vaadin:flow-server:${flowServerVersion()}"
    )
  }

  case class VaadinFrontend(webpackDir: PathRef)
  object VaadinFrontend {
    implicit val jsonFormatter: upickle.default.ReadWriter[VaadinFrontend] = upickle.default.macroRW
  }

  def frontendDir: Source = T.source(millSourcePath / "frontend")

  def generatorWorkDir: T[PathRef] = T.persistent { PathRef(T.dest) }

  def generateFrontend: T[VaadinFrontend] = T {
    val dest = T.dest

    VaadinFrontend(vaadinWorkDir())
  }

  def vaadinToolsWorker: T[Agg[PathRef]] = T {
    resolveDeps(frontendToolsIvyDeps)()
  }

  def frontendToolsWorker: Worker[VaadinToolsWorker] = T.worker {
    val workerDir = generatorWorkDir().path

    val cl =
      new URLClassLoader(vaadinToolsWorker().map(_.path.toIO.toURI().toURL()).toArray[URL], getClass().getClassLoader())
    val className =
      classOf[VaadinToolsWorker].getPackage().getName() + ".impl." + classOf[VaadinToolsWorker].getSimpleName() + "Impl"
    val impl = cl.loadClass(className)
    val ctr = impl.getConstructor(Seq(classOf[os.Path]): _*)
    val worker = ctr.newInstance(Seq(workerDir): _*).asInstanceOf[VaadinToolsWorker]
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

  def vaadinWorkDir: T[PathRef] = T.persistent { PathRef(T.dest) }

}
