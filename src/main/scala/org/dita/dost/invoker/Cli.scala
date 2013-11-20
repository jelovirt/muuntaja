package org.dita.dost.invoker

import java.io.File

import org.dita.dost.module._

object Cli {

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
        System.err.println("Usage: scala Cli <input>")
    } else {
        val ditaDir = System.getenv("DITA_HOME") match {
          case null => new File(".")
          case d => new File(d)
        }
        val xhtml = new XHTML(ditaDir)
        xhtml.$("args.input") = new File(args(0)).getAbsolutePath()
        xhtml.$("transtype") = "xhtml"
        xhtml.run()
    }
  }

}