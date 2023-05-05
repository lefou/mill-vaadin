import $file.plugins
import $file.shared

//import $ivy.`de.tototec::de.tobiasroeser.mill.vaadin::0.0.1-SNAPSHOT`

import mill._
import mill.define.Command
import mill.scalalib._
import mill.api.Result

import de.tobiasroeser.mill.vaadin._

object Deps {
  def springBootVersion = "2.7.8"
  def vaadinVersion = "23.3.10"

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
    nonExistantFiles.flatMap(helper.checkNonexistantFile) ++
      emptyDirs.flatMap(helper.checkEmptyDir)

  if (assertions.nonEmpty) {
    Result.Failure(s"${assertions.size} invalid!\n" + assertions.mkString("\n"))
  } else {
    println("All clean:\n" + (nonExistantFiles ++ emptyDirs).mkString("\n"))
    Result.Success(())
  }
}

val filesPrepare = Seq(
  os.sub / "types.d.ts",
  os.sub / "tsconfig.json",
  os.sub / "package-lock.json",
  os.sub / "vite.generated.ts",
  os.sub / "frontend" / "generated" / "vaadin-featureflags.ts",
  os.sub / "frontend" / "generated" / "vite-devmode.ts",
  os.sub / "frontend" / "generated" / "jar-resources" / "comboBoxConnector.js",
  os.sub / "frontend" / "generated" / "jar-resources" / "index.js",
  os.sub / "frontend" / "generated" / "jar-resources" / "index.d.ts",
  os.sub / "frontend" / "generated" / "jar-resources" / "FlowClient.js",
  os.sub / "frontend" / "generated" / "jar-resources" / "Flow.js",
  os.sub / "frontend" / "generated" / "jar-resources" / "Flow.js.map",
  os.sub / "frontend" / "generated" / "jar-resources" / "Flow.d.ts",
  os.sub / "frontend" / "generated" / "jar-resources" / "FlowClient.d.ts",
  os.sub / "frontend" / "generated" / "jar-resources" / "vaadin-dev-tools.d.ts",
  os.sub / "frontend" / "generated" / "jar-resources" / "vaadin-dev-tools.js",
  os.sub / "frontend" / "generated" / "jar-resources" / "vaadin-map" / "synchronization" / "index.js",
  os.sub / "target" / "vaadin-dev-server-settings.json",
  os.sub / "target" / "frontend",
  os.sub / "target" / "classes" / "META-INF" / "VAADIN" / "config" / "flow-build-info.json"
)

val filesBuild = Seq(
  os.sub / "node_modules",
  os.sub / "target" / "frontend" / "versions.json",
  os.sub / "target" / "frontend" / "generated-flow-imports-fallback.js",
  os.sub / "target" / "frontend" / "generated-flow-imports.js",
  os.sub / "target" / "frontend" / "generated-flow-imports.d.ts",
  os.sub / "target" / "classes" / "META-INF" / "VAADIN" / "config" / "stats.json",
  os.sub / "target" / "classes" / "META-INF" / "VAADIN" / "webapp" / "VAADIN" / "build",
  os.sub / "target" / "classes" / "META-INF" / "VAADIN" / "webapp" / "index.html",
  os.sub / "target" / "plugins" / "theme-loader" / "theme-loader.js",
  os.sub / "target" / "plugins" / "application-theme-plugin" / "package.json",
  os.sub / "target" / "plugins" / "theme-live-reload-plugin" / "package.json"
)

def validatePrepareFrontend(): Command[Unit] = T.command {
  val base = T.workspace

  val content = Map(
    base / "target" / "classes" / "META-INF" / "VAADIN" / "config" / "flow-build-info.json" ->
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
    filesPrepare.map(base / _).flatMap(helper.checkExistingFile) ++
      filesBuild.map(base / _).flatMap(helper.checkNonexistantFile) ++
      content.flatMap { case (f, c) => helper.checkFileContents(f, c) }
  if (assertions.nonEmpty) {
    Result.Failure(s"${assertions.size} invalid!\n" + assertions.mkString("\n"))
  } else {
    println("All exist:\n" + filesPrepare.mkString("\n"))
    println("All do not exist:\n" + filesBuild.mkString("\n"))
    Result.Success(())
  }
}

def validateBuildFrontend(): Command[Unit] = T.command {
  val base = T.workspace

  val contains = Map(
    base / "target" / "classes" / "META-INF" / "VAADIN" / "config" / "flow-build-info.json" ->
      Seq(
        """"productionMode": false""",
        """"useDeprecatedV14Bootstrapping": false"""
        //        """"enableDevServer": false"""
      )
  )

  val files = filesPrepare ++ filesBuild

  val assertions =
    files.map(base / _).flatMap(helper.checkExistingFile) ++
      contains.flatMap { case (f, cs) => cs.flatMap(c => helper.checkFileContains(f, c)) }
  if (assertions.nonEmpty) {
    Result.Failure(s"${assertions.size} invalid!\n" + assertions.mkString("\n"))
  } else {
    println("All exist:\n" + files.mkString("\n"))
    Result.Success(())
  }
}
