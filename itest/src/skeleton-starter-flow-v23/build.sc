import $exec.plugins
import $exec.shared

import mill._
import mill.define.Command
import mill.scalalib._

import de.tobiasroeser.mill.vaadin._

object Deps {
  def vaadinVersion = "23.1.7"
}


object main extends MavenModule with VaadinModule {
  def millSourcePath = super.millSourcePath / os.up
  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"com.vaadin:vaadin:${Deps.vaadinVersion}"
  )
}

def verify(): Command[Unit] = T.command {
  val res = main.vaadinPrepareFrontend()()
  println(s"res: ${res}")
  ()
}