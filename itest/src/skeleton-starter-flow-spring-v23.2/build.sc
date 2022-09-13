//import $ivy.`de.tototec::de.tobiasroeser.mill.vaadin::0.0.1-SNAPSHOT`
import $exec.plugins
import $exec.shared
import $ivy.`de.tototec:de.tobiasroeser.lambdatest:0.7.1`

import mill._
import mill.define.Command
import mill.scalalib._
import mill.api.Result

import de.tobiasroeser.mill.vaadin._
import de.tobiasroeser.lambdatest

object Deps {
  def springBootVersion = "2.7.3"
  def vaadinVersion = "23.2.0"

  val springBootStarterValidation = ivy"org.springframework.boot:spring-boot-starter-validation:${springBootVersion}"
  val vaadin = ivy"com.vaadin:vaadin:${vaadinVersion}"
  val vaadinSpringBootStarter = ivy"com.vaadin:vaadin-spring-boot-starter:${vaadinVersion}"
}

object v extends MavenModule with VaadinModule {
  override def millSourcePath = super.millSourcePath / os.up
  override def ivyDeps: T[Agg[Dep]] = Agg(
    Deps.vaadin,
    Deps.vaadinSpringBootStarter,
    Deps.springBootStarterValidation
  )
}

def validateCleanFrontend(): Command[Unit] = T.command {
  val base = T.workspace

  val nonExistantFiles = Seq(
    base / "vite.generated.ts"
  )

  val emptyDirs = Seq(
    base / "frontend" / "generated",
    base / "node_modules",
    base / "target" / "classes"
  )

  val assertions =
    nonExistantFiles.filter(f => os.exists(f))
      .map(f => s"File should not exist: ${f}") ++
    emptyDirs.filter(d => os.exists(d) && !os.list(d).isEmpty)
      .map(d => s"Directory present or non-empty: ${d}")

  if(assertions.nonEmpty) {
    Result.Failure(s"${assertions.size} invalid!\n" + assertions.mkString("\n"))
  } else {
    println("All clean:\n" + (nonExistantFiles ++ emptyDirs).mkString("\n"))
    Result.Success(())
  }
}

def validatePrepareFrontend(): Command[Unit] = T.command {
  val base = T.workspace
  val target = base / "target"

  val files = Seq(
    base / "vite.generated.ts",
    base / "frontend" / "generated" / "vaadin-featureflags.ts",
    base / "frontend" / "generated" / "vite-devmode.ts",
    target / "vaadin-dev-server-settings.json",
    target / "frontend",
    target / "flow-frontend",
    target / "flow-frontend"/"comboBoxConnector.js",
    target / "flow-frontend"/"index.js",
    target / "flow-frontend" / "package.json",
    target / "flow-frontend"/"vaadin-map"/"synchronization"/"index.js",
    target / "classes"/"META-INF" / "VAADIN" /"config" /"flow-build-info.json"
  )

  val content = Map(
    target / "classes"/"META-INF" / "VAADIN" /"config" /"flow-build-info.json" ->
    s"""{
    |  "productionMode": false,
    |  "useDeprecatedV14Bootstrapping": false,
    |  "eagerServerLoad": false,
    |  "npmFolder": "${base}",
    |  "node.version": "v16.16.0",
    |  "node.download.root": "https://nodejs.org/dist/",
    |  "generatedFolder": "${base}/target/frontend",
    |  "frontendFolder": "${base}/frontend",
    |  "connect.javaSourceFolder": "${base}/src/main/java",
    |  "javaResourceFolder": "${base}/src/main/resources",
    |  "connect.applicationProperties": "${base}/src/main/resources/application.properties",
    |  "connect.openApiFile": "${base}/target/generated-resources/openapi.json",
    |  "project.frontend.generated": "${base}/frontend/generated",
    |  "pnpm.enable": false,
    |  "require.home.node": false,
    |  "build.folder": "target"
    |}""".stripMargin
  )

  val assertions =
    files.filter(f => !os.exists(f))
      .map(f => s"File should exist: ${f}") ++
    content.filter{ case (f, c) =>
        os.read(f).trim() != c.trim()
    }
      .map{ case (f,c) =>
        s"File contents does not match expected: ${f}" + "\nExpected:\n" + c + "\nActual:\n" + os.read(f) + "\nDiff:\n"        +
          compare(os.read(f), c)
      }
  if (assertions.nonEmpty) {
    Result.Failure(s"${assertions.size} invalid!\n" + assertions.mkString("\n"))
  } else {
    println("All exist:\n" + files.mkString("\n"))
    Result.Success(())
  }
}

def compare(s: String, s2: String): String = {
  try {
    lambdatest.Assert.assertEquals(s, s2)
    ""
  } catch {
    case e: AssertionError => e.getMessage()
  }
}