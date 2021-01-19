package de.tobiasroeser.mill.vaadin.worker.impl

import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.installer.NodeInstaller
import de.tobiasroeser.mill.vaadin.worker.VaadinToolsWorker

import java.net.URI

class VaadinToolsWorkerImpl(workerDir: os.Path) extends VaadinToolsWorker {

  def defaultNodeVersion = FrontendTools.DEFAULT_NODE_VERSION
  def defaultDownloadRoot = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT

  def frontendTools(
      baseDir: os.Path,
      alternativeDir: Option[os.Path],
      nodeVersion: String = defaultNodeVersion,
      nodeDownloadRoot: String = defaultDownloadRoot
  ) = new FrontendTools(
    baseDir.toIO.getAbsolutePath(),
    () => alternativeDir.map(_.toIO.getAbsolutePath()).orNull,
    nodeVersion,
    new URI(nodeDownloadRoot)
  )

  def validateNodeAndNpmVersion(nodeVersion: String, npmVersion: String) = {}

}
