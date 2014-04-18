package org.dita.dost.invoker

import java.io.File

import org.dita.dost.module._

object Cli {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println("Usage: scala Cli <transtype> <input>")
    } else {
      val ditaDir = System.getenv("DITA_HOME") match {
        case null => new File(".")
        case d => new File(d)
      }
      //val xhtml = new XHTML(ditaDir)
      val processor = args(0) match {
        case "test" => new Test(ditaDir)
        case "xhtml" => new XHTML(ditaDir)
        case "rtf" => new WordRTF(ditaDir)
      }
      processor.$("args.input") = new File(args(1)).getAbsolutePath()
      processor.run()
    }
  }

}