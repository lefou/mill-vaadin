// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import de.tobiasroeser.mill.vcs.version.{Vcs, VcsState}
import mill.define.Cross

// imports
import mill.api.Loose

import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}
import mill.contrib.buildinfo.{BuildInfo}
import mill.define.{Module, Target, Task, TaskModule}
import mill.scalalib._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.publish._

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String = "2.13.13"
  def testWithMill: Seq[String]

  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  def osLib: Dep
  val scoverageVersion = "2.1.0"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.17"
  val lambdaTest = ivy"de.tototec:de.tobiasroeser.lambdatest:0.7.1"

  trait Vaadin {
    def logbackClassic: Dep
    def reflections: Dep
    def slf4j: Dep
    def slf4jSimple: Dep
    def vaadinFlowVersion: String
    def vaadinFlowServer: Dep
    def vaadinFlowPluginBase: Dep
    def vaadinFlowPolymer2lit: Dep
  }
  object Vaadin_23 extends Vaadin {
    def vaadinFlowVersion: String = "23.3.29"
    def vaadinFlowServer = ivy"com.vaadin:flow-server:${vaadinFlowVersion}"
    def vaadinFlowPluginBase = ivy"com.vaadin:flow-plugin-base:${vaadinFlowVersion}"
    def vaadinFlowPolymer2lit: Dep = ivy"com.vaadin:flow-polymer2lit:${vaadinFlowVersion}"
    def reflections = ivy"org.reflections:reflections:0.10.2"
    def slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
    def slf4jSimple = ivy"org.slf4j:slf4j-simple:1.7.25"
    def logbackClassic = ivy"ch.qos.logback:logback-classic:1.1.3"
  }
  object Vaadin_24 extends Vaadin {
    def vaadinFlowVersion: String = "24.3.5"
    def vaadinFlowServer = ivy"com.vaadin:flow-server:${vaadinFlowVersion}"
    def vaadinFlowPluginBase = ivy"com.vaadin:flow-plugin-base:${vaadinFlowVersion}"
    def vaadinFlowPolymer2lit: Dep = ivy"com.vaadin:flow-polymer2lit:${vaadinFlowVersion}"
    def reflections = ivy"org.reflections:reflections:0.10.2"
    def slf4j = ivy"org.slf4j:slf4j-api:2.0.12"
    def slf4jSimple = ivy"org.slf4j:slf4j-simple:2.0.12"
    def logbackClassic = ivy"ch.qos.logback:logback-classic:1.5.1"
  }
  val vaadin = Map(
    "23" -> Vaadin_23,
    "24" -> Vaadin_24
  )
}

object Deps_0_11 extends Deps {
  override def millVersion = "0.11.0" // scala-steward:off
  override def millPlatform = "0.11"
  override def testWithMill = Seq("0.11.7", millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.9.1" // scala-steward:off
}
object Deps_0_10 extends Deps {
  override def millVersion = "0.10.0" // scala-steward:off
  override def millPlatform = "0.10"
  override def testWithMill = Seq("0.10.15", millVersion)
  override def osLib = ivy"com.lihaoyi::os-lib:0.8.0" // scala-steward:off
}

val millApiVersions = Seq(Deps_0_11, Deps_0_10).map(x => x.millPlatform -> x)

val millItestVersions = millApiVersions.flatMap { case (_, d) => d.testWithMill.map(_ -> d) }

lazy val baseDir = build.millSourcePath

object MyVcsVersion extends VcsVersion {
  override def vcsBasePath: os.Path = baseDir
  override def vcsState: Input[VcsState] = T{
    super.vcsState().copy(
      currentRevision = "",
      lastTag = Some("0.0.4-SNAPSHOT"),
      commitsSinceLastTag = 0,
      dirtyHash = None,
      vcs = Some(Vcs("manual"))
    )
//    VcsState("SNAPSHOT", state.lastTag, 0, None, None)
  }
}

trait MillVaadinModule extends ScalaModule with PublishModule with ScoverageModule {
  def millPlatform: String
  def deps: Deps = millApiVersions.toMap.apply(millPlatform)

  override def scalaVersion: T[String] = T(deps.scalaVersion)
  override def publishVersion: T[String] = MyVcsVersion.vcsState().format()
  override def artifactSuffix: T[String] = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"
  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-deprecation")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-deprecation")
  override def scoverageVersion = deps.scoverageVersion

  override def sources: T[Seq[PathRef]] = T.sources {
    super.sources() ++
      ZincWorkerUtil.versionRanges(millPlatform, millApiVersions.map(_._1))
        .map(p => PathRef(millSourcePath / s"src-${p}"))
  }

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

  def skipIdea = millPlatform != Deps_0_11.millPlatform
}

object main extends Cross[MainCross](millApiVersions.map(_._1))
trait MainCross extends MillVaadinModule with Cross.Module[String] with BuildInfo { main =>
  override def millPlatform = crossValue

  override def artifactName = "de.tobiasroeser.mill.vaadin"
  override def moduleDeps: Seq[PublishModule] = Seq(worker)
  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  override def buildInfoPackageName = "de.tobiasroeser.mill.vaadin"
  override def buildInfoObjectName: String = "Versions"
  override def buildInfoStaticCompiled: Boolean = true
  override def buildInfoMembers: T[Seq[BuildInfo.Value]] = Seq(
    BuildInfo.Value("millVaadinVersion", publishVersion(), "The mill-vaadin plugin version"),
    BuildInfo.Value("buildTimeMillVersion", deps.millVersion, "The Mill API version used to build mill-vaadin plugin"),
    BuildInfo.Value(
      "workerIvyDeps23",
      Seq(
        formatDep(worker.impl("23").publishSelfDependency()),
        formatDep(deps.Vaadin_23.vaadinFlowPluginBase),
        formatDep(deps.Vaadin_23.slf4j),
        formatDep(deps.Vaadin_23.slf4jSimple)
      ).mkString(","),
      "The dependencies to load the Worker for Vaadin 23"
    ),
    BuildInfo.Value(
      "workerIvyDeps24",
      Seq(
        formatDep(worker.impl("24").publishSelfDependency())
//        formatDep(deps.Vaadin_24.vaadinFlowPluginBase),
//        formatDep(deps.Vaadin_24.slf4j),
//        formatDep(deps.Vaadin_24.slf4jSimple)
      ).mkString(","),
      "The dependencies to load the Worker for Vaadin 24"
    )
  )

  object worker extends MillVaadinModule {
    override def millPlatform = main.millPlatform
    override def artifactName = "de.tobiasroeser.mill.vaadin-worker"
    override def compileIvyDeps: T[Loose.Agg[Dep]] = T {
      Agg(
        deps.millMainApi,
        deps.osLib
      )
    }

    object impl extends Cross[Impl]("24", "23")
    trait Impl extends MillVaadinModule with Cross.Module[String] {
      override def millSourcePath = super.millSourcePath / os.up / s"impl-${crossValue}"
      override def millPlatform = main.millPlatform
      def vaadinDeps = deps.vaadin(crossValue)
      override def artifactName = s"de.tobiasroeser.mill.vaadin-worker-impl-${crossValue}"

      override def moduleDeps: Seq[PublishModule] = Seq(worker)
      override def compileIvyDeps: T[Loose.Agg[Dep]] = T {
        Agg(
          deps.osLib,
          deps.millMainApi
        )
      }

      override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
        vaadinDeps.reflections,
        vaadinDeps.vaadinFlowServer,
        vaadinDeps.vaadinFlowPluginBase,
        vaadinDeps.slf4j,
        vaadinDeps.logbackClassic
      ) ++
        Agg.when(crossValue == "24")(
//          ivy"com.vaadin:vaadin-dev:${vaadinDeps.vaadinFlowVersion}",
//          ivy"com.vaadin:copilot:${vaadinDeps.vaadinFlowVersion}"
          vaadinDeps.vaadinFlowPolymer2lit
        )
    }
  }

}

object itest extends Cross[ItestCross](millItestVersions.map(_._1))
trait ItestCross extends MillIntegrationTestModule with Cross.Module[String] {
  val millItestVersion = crossValue

  val deps: Deps = millItestVersions.toMap.apply(millItestVersion)
  val mainModule = main(deps.millPlatform)

  override def sources: T[Seq[PathRef]] = T.sources(
    this.millSourcePath / s"src-${millItestVersion}",
    this.millSourcePath / s"src-${deps.millPlatform}",
    this.millSourcePath / "src"
  )

  override def millTestVersion = millItestVersion

  override def pluginsUnderTest = Seq(mainModule)

  override def temporaryIvyModules =
    Seq(mainModule.worker) ++
      deps.vaadin.keys.map(v => mainModule.worker.impl(v))

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
        case _ => Seq(
            TestInvocation.Targets(Seq("-d", "v.vaadinPrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "validatePrepareFrontend")),
            TestInvocation.Targets(Seq("-d", "v.vaadinBuildFrontend")),
            TestInvocation.Targets(Seq("-d", "validateBuildFrontend"))
          )
      })
    }

  def useScoverageJars: T[Boolean] = false

  // Use scoverage enhanced jars to collect coverage data while running tests
  override def temporaryIvyModulesDetails: Task[Seq[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))]] =
    Target.traverse(temporaryIvyModules) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  // Use scoverage enhanced jars to collect coverage data while running tests
  override def pluginUnderTestDetails: Task[Seq[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))]] =
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
      s"""import $$file.plugins
         |import $$ivy.`org.scoverage::scalac-scoverage-runtime:${deps.scoverageVersion}`
         |import $$ivy.`${formatDep(deps.lambdaTest)}`
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
    val target = mill.util.Util.download("https://raw.githubusercontent.com/lefou/millw/master/millw")
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

private def formatDep(dep: Artifact): String = s"${dep.group}:${dep.id}:${dep.version}"
private def formatDep(dep: Dep): String = {
  val module = dep.dep.module
  s"${module.organization.value}:${module.name.value}:${dep.dep.version}"
}

// Check all dependencies
val dummyDeps: Seq[Dep] = Seq(
  Deps_0_11.lambdaTest,
  ivy"org.scoverage::scalac-scoverage-runtime:${Deps_0_11.scoverageVersion}",
  Deps_0_11.Vaadin_23.vaadinFlowPluginBase,
  Deps_0_11.Vaadin_23.logbackClassic,
  Deps_0_11.Vaadin_23.slf4j,
  Deps_0_11.Vaadin_23.slf4jSimple,
  Deps_0_11.Vaadin_24.vaadinFlowPluginBase,
  Deps_0_11.Vaadin_24.logbackClassic,
  Deps_0_11.Vaadin_24.slf4j,
  Deps_0_11.Vaadin_24.slf4jSimple
).distinct

implicit object DepSegment extends Cross.ToSegments[Dep]({ dep =>
      val depString = formatDep(dep)
      List(depString)
    })

/**
 * Dummy module(s) to let Dependency/showUpdates or Scala-Steward find
 * and bump dependency versions we use at runtime
 */
object dummy extends Cross[DependencyFetchDummy](dummyDeps)
trait DependencyFetchDummy extends ScalaModule with Cross.Module[Dep] {
  def scalaVersion = Deps_0_11.scalaVersion
  override def compileIvyDeps = Agg(crossValue)
}
