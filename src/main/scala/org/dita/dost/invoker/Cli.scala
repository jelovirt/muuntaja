package org.dita.dost.invoker

import java.io.File

import org.dita.dost.module._

object Cli {

  def main(args: Array[String]): Unit = {    
    val xhtml = new XHTML(new File("."))
    Properties("args.input") = new File(args(0)).getAbsolutePath()
    Properties("transtype") = "xhtml"
    xhtml.run()
  }

}