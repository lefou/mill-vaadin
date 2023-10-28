// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`

// imports
import mill.api.Loose
import mill.define.Sources

import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}
import mill.define.{Module, Target, Task, TaskModule}
import mill.scalalib._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.publish._

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String = "2.13.12"
  def testWithMill: Seq[String]

  val vaadinFlowVersion = "24.2.1"

  val logbackClassic = ivy"ch.qos.logback:logback-classic:1.1.3"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  def osLib: Dep
  val reflections = ivy"org.reflections:reflections:0.10.2"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.17"
  val scoverageVersion = "2.0.11"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
  val slf4jSimple = ivy"org.slf4j:slf4j-simple:1.7.25"
  val utilsFunctional = ivy"de.tototec:de.tototec.utils.functional:2.0.1"
  val vaadinFlowServer = ivy"com.vaadin:flow-server:${vaadinFlowVersion}"
  val vaadinFlowPluginBase = ivy"com.vaadin:flow-plugin-base:${vaadinFlowVersion}"
}
object Deps_0_11 extends Deps {
  override def millVersion = "0.11.0" // scala-steward:off
  override def millPlatform = "0.11"
  override def testWithMill = Seq(millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.9.1" // scala-steward:off
}
object Deps_0_10 extends Deps {
  override def millVersion = "0.10.0" // scala-steward:off
  override def millPlatform = "0.10"
  override def testWithMill = Seq("0.10.11", millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.8.0" // scala-steward:off
}

val millApiVersions = Seq(Deps_0_11, Deps_0_10).map(x => x.millPlatform -> x)

val millItestVersions = millApiVersions.flatMap { case (_, d) => d.testWithMill.map(_ -> d) }

val baseDir = build.millSourcePath

trait MillVaadinModule extends ScalaModule with PublishModule with ScoverageModule {
  def deps: Deps

  override def scalaVersion: T[String] = T(deps.scalaVersion)
  override def publishVersion: T[String] = VcsVersion.vcsState().format()
  override def artifactSuffix: T[String] = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"
  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-deprecation")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-deprecation")
  override def scoverageVersion = deps.scoverageVersion

  def pomSettings = T {
    PomSettings(
      description = "Vaadin support for mill",
      organization = "de.tototec",
      url = "https://github.com/lefou/mill-vaadin",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("lefou", "mill-vaadin"),
      developers = Seq(Developer("lefou", "Tobias Roeser", "https.//github.com/lefou"))
    )
  }

  override def skipIdea: Boolean = millApiVersions.head._2.scalaVersion != deps.scalaVersion
}

object main extends Cross[MainCross](millApiVersions.map(_._1): _*)
class MainCross(val millPlatform: String) extends MillVaadinModule { vaadin =>
  override def deps: Deps = millApiVersions.toMap.apply(millPlatform)

  override def millSourcePath: os.Path = super.millSourcePath / os.up

  override def sources: Sources = T.sources {
    super.sources() ++
      ZincWorkerUtil.versionRanges(millPlatform, millApiVersions.map(_._1))
        .map(p => PathRef(millSourcePath / s"src-${p}"))
  }

  object worker extends MillVaadinModule {
    def deps = vaadin.deps
    override def artifactName = "de.tobiasroeser.mill.vaadin-worker"
    override def compileIvyDeps: T[Loose.Agg[Dep]] = T {
      Agg(
        deps.millMainApi,
        deps.osLib
      )
    }

    object impl extends MillVaadinModule {
      def deps = vaadin.deps

      override def artifactName = "de.tobiasroeser.mill.vaadin-worker-impl"

      override def moduleDeps: Seq[PublishModule] = Seq(worker)

      override def compileIvyDeps: T[Loose.Agg[Dep]] = T {
        Agg(
          deps.osLib,
          deps.millMainApi,
          deps.vaadinFlowServer,
          deps.vaadinFlowPluginBase
        )
      }

      override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
        deps.reflections
      )
    }
  }

  override def artifactName = "de.tobiasroeser.mill.vaadin"
  override def moduleDeps: Seq[PublishModule] = Seq(worker)
  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  object test extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      deps.scalaTest
    )
  }

  override def generatedSources: Target[Seq[PathRef]] = T {
    super.generatedSources() :+ versionFile()
  }

  private def formatIvyDep(dep: Dep): String = {
    val module = dep.dep.module
    s"${module.organization.value}:${module.name.value}:${dep.dep.version}"
  }

  def versionFile: Target[PathRef] = T {
    val dest = T.ctx().dest
    val artifactMetadata = worker.impl.artifactMetadata()
    val body =
      s"""package de.tobiasroeser.mill.vaadin
         |
         |/**
         | * Build-time generated versions file.
         | */
         |object Versions {
         |  /** The mill-kotlin version. */
         |  val millVaadinVersion = "${publishVersion()}"
         |  /** The mill API version used to build mill-kotlin. */
         |  val buildTimeMillVersion = "${deps.millVersion}"
         |  /** The ivy dependency holding the mill kotlin worker impl. */
         |  val millVaadinWorkerImplIvyDep = "${artifactMetadata.group}:${artifactMetadata.id}:${artifactMetadata.version}"
         |  val buildTimeFlowServerVersion = "${deps.vaadinFlowServer.dep.version}"
         |  val vaadinFlowPluginBaseDep ="${formatIvyDep(deps.vaadinFlowPluginBase)}"
         |  val workerIvyDeps = Seq(
         |    "${artifactMetadata.group}:${artifactMetadata.id}:${artifactMetadata.version}",
         |    "${formatIvyDep(deps.vaadinFlowPluginBase)}",
         |    "${formatIvyDep(deps.slf4j)}",
         |    "${formatIvyDep(deps.slf4jSimple)}"
         |  )
         |}
         |""".stripMargin

    os.write(dest / "Versions.scala", body)
    PathRef(dest)
  }

}

object itest extends Cross[ItestCross](millItestVersions.map(_._1): _*) with TaskModule {
  override def defaultCommandName(): String = "test"
  def test(args: String*) = millModuleDirectChildren.collect { case m: ItestCross => m }.head.test(args: _*)
}
class ItestCross(millItestVersion: String) extends MillIntegrationTestModule {

  val deps: Deps = millItestVersions.toMap.apply(millItestVersion)
  val mainModule = main(deps.millPlatform)

  override def millSourcePath: os.Path = super.millSourcePath / os.up

  override def sources: Sources = T.sources(
    millSourcePath / s"src-${millItestVersion}",
    millSourcePath / s"src-${deps.millPlatform}",
    millSourcePath / "src"
  )

  override def millTestVersion = millItestVersion

  override def pluginsUnderTest = Seq(mainModule)

  override def temporaryIvyModules = Seq(mainModule.worker, mainModule.worker.impl)

  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    testCases().map { tc =>
      tc -> (tc.path.last match {
        case "skeleton-starter-flow-spring-v23.2" => Seq(
            TestInvocation.Targets(Seq("-d", "v.vaadinPrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "validatePrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "v.vaadinBuildFrontend")),
            TestInvocation.Targets(Seq("-d", "validateBuildFrontend"))
          )
        case "skeleton-starter-flow-spring-v23.3.6" => Seq(
            TestInvocation.Targets(Seq("-d", "v.vaadinPrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "validatePrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "v.vaadinBuildFrontend")),
            TestInvocation.Targets(Seq("-d", "validateBuildFrontend"))
          )
        case _ => Seq()
      })
    }

  def useScoverageJars: T[Boolean] = false

  // Use scoverage enhanced jars to collect coverage data while running tests
  override def temporaryIvyModulesDetails
      : Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(temporaryIvyModules) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  // Use scoverage enhanced jars to collect coverage data while running tests
  override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def perTestResources = T.sources {
    Seq(
      PathRef(millSourcePath / "resources"),
      generatedSharedSrc()
    )
  }

  def generatedSharedSrc = T {
    os.write(
      T.dest / "shared.sc",
      s"""import $$ivy.`org.scoverage::scalac-scoverage-runtime:${deps.scoverageVersion}`
         |import $$file.helper
         |""".stripMargin
    )
    PathRef(T.dest)
  }
}

object P extends Module {

  /**
   * Update the millw script.
   */
  def millw() = T.command {
    val target = mill.modules.Util.download("https://raw.githubusercontent.com/lefou/millw/master/millw")
    val millw = baseDir / "millw"
    os.copy.over(target.path, millw)
    os.perms.set(millw, os.perms(millw) + java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE)
    target
  }

}

object scoverage extends ScoverageReport {
  override def scoverageVersion = Deps_0_10.scoverageVersion
  override def scalaVersion: T[String] = Deps_0_10.scalaVersion
}
