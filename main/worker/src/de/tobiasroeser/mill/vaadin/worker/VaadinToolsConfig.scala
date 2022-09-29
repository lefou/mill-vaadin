package de.tobiasroeser.mill.vaadin.worker

import mill.api.PathRef
import os.Path

case class VaadinToolsConfig(
    moduleDir: Path, // moduleDir
    npmFolder: Path, // moduleDir
    nodeVersion: Option[String],
    nodeDownloadRoot: Option[String],
    frontendDir: PathRef, // moduleDir / frontend
    moduleCompileClasspath: Seq[PathRef],
    moduleRuntimeClasspath: Seq[PathRef],
    useDeprecatedV14Bootstrapping: Boolean,
    requireHomeNodeExec: Boolean,
    productionMode: Boolean,
    generatedFrontendDir: Path,
    javaSourceDir: Seq[PathRef],
    pnpmEnable: Boolean
)

object VaadinToolsConfig {
//  implicit val jsonFormatter: upickle.default.ReadWriter[VaadinToolsConfig] = upickle.default.macroRW
//  implicit val pathFormatter: upickle.default.ReadWriter[Path] = upickle.default
//    .readwriter[String]
//    .bimap(
//      path => path.toIO.getAbsolutePath(),
//      string => Path(string, os.pwd)
//    )
}
