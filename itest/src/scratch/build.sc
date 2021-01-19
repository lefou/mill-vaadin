import $exec.plugins
import $ivy.`org.scoverage::scalac-scoverage-runtime:1.4.1`
import mill._
import mill.define.Command
import mill.scalalib._

import de.tobiasroeser.mill.vaadin._


object main extends VaadinModule {
  def vaadinVersion = "14.0.0"

}

def verify(): Command[Unit] = T.command {
  val res = main.prepareFrontend()
  ()
}