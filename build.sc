// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.6.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.3.0`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`

// imports
import mill.api.Loose
import mill.define.Sources

import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}
import mill.define.{Module, Target, Task}
import mill.scalalib._
import mill.scalalib.publish._

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String
  def testWithMill: Seq[String]

  val vaadinVersion = "23.2.0"

  val logbackClassic = ivy"ch.qos.logback:logback-classic:1.1.3"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  val osLib = ivy"com.lihaoyi::os-lib:0.8.0"
  val reflections = ivy"org.reflections:reflections:0.10.2"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.3"
  val scoverageVersion = "2.0.5"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
  val slf4jSimple = ivy"org.slf4j:slf4j-simple:1.7.25"
  val utilsFunctional = ivy"de.tototec:de.tototec.utils.functional:2.0.1"
  val vaadinFlowServer = ivy"com.vaadin:flow-server:${vaadinVersion}"
  val vaadinFlowPluginBase = ivy"com.vaadin:flow-plugin-base:${vaadinVersion}"
}
object Deps_0_10 extends Deps {
  override def millVersion = "0.10.0"
  override def millPlatform = "0.10"
  override def scalaVersion = "2.13.9"
  override def testWithMill = Seq("0.10.7", millVersion)
}

val millApiVersions = Seq(Deps_0_10).map(x => x.millPlatform -> x)

val millItestVersions = millApiVersions.flatMap { case (_, d) => d.testWithMill.map(_ -> d) }

val baseDir = build.millSourcePath

object vaadin extends Cross[VaadinCross](millApiVersions.map(_._1): _*)
class VaadinCross(val millPlatform: String) extends Module {

  def deps: Deps = millApiVersions.toMap.apply(millPlatform)

  trait MillVaadinModule extends CrossScalaModule with PublishModule with ScoverageModule {
    override def millSourcePath = baseDir / millOuterCtx.segment.pathSegments.last
    override def crossScalaVersion = deps.scalaVersion
    override def publishVersion: T[String] = VcsVersion.vcsState().format()
    override def artifactSuffix: T[String] = s"_mill${millPlatform}_${artifactScalaVersion()}"

    override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8")
    override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8")
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

    override def skipIdea: Boolean = millApiVersions.head._2.scalaVersion != crossScalaVersion
  }

  object api extends MillVaadinModule {
    override def artifactName = "de.tobiasroeser.mill.vaadin-api"
    override def compileIvyDeps: T[Loose.Agg[Dep]] = T {
      Agg(
        deps.millMainApi,
        deps.osLib
      )
    }
  }

  object worker extends MillVaadinModule {
    override def artifactName = "de.tobiasroeser.mill.vaadin-worker"
    override def moduleDeps: Seq[PublishModule] = Seq(api)
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

  object main extends MillVaadinModule {
    override def artifactName = "de.tobiasroeser.mill.vaadin"
    override def moduleDeps: Seq[PublishModule] = Seq(api)
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
           |  val millVaadinWorkerImplIvyDep = "${worker.pomSettings().organization}:${worker.artifactId()}:${worker
            .publishVersion()}"
           |  val buildTimeFlowServerVersion = "${deps.vaadinFlowServer.dep.version}"
           |  val vaadinFlowPluginBaseDep ="${formatIvyDep(deps.vaadinFlowPluginBase)}"
           |  val workerIvyDeps = Seq(
           |    "${worker.pomSettings().organization}:${worker.artifactId()}:${worker.publishVersion()}",
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

  object itest extends Cross[ItestCross](deps.testWithMill: _*)
  class ItestCross(millItestVersion: String) extends MillIntegrationTestModule {
    override def millSourcePath: os.Path = baseDir / "itest"
    override def sources: Sources = T.sources(
      millSourcePath / s"src-${millItestVersion}",
      millSourcePath / s"src-${millPlatform}",
      millSourcePath / "src"
    )
    override def millTestVersion = millItestVersion
    override def pluginsUnderTest = Seq(main)
    override def temporaryIvyModules = Seq(api, worker)

    override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
      testCases().map { tc =>
        tc -> (tc.path.last match {
//          case "skeleton-starter-flow-v23" => Seq(TestInvocation.Targets(targets = Seq("-d", "verifyPrepareFrontend")))
//          case "skeleton-starter-flow-v23_2" => Seq(TestInvocation.Targets(Seq("-d", "verifyBuildFrontend")))
          case "skeleton-starter-flow-spring-v23.2" => Seq(
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
    override def pluginUnderTestDetails
        : Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
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
