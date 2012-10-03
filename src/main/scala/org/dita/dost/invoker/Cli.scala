package org.dita.dost.invoker

import java.io.File

import org.dita.dost.module._

object Cli {

  def main(args: Array[String]): Unit = {    
    val wordrtf = new Dita2wordrtf(new File("."))
    Properties("args.input") = new File(args(0)).getAbsolutePath()
    Properties("transtype") = "xhtml"
    wordrtf.dita2wordrtf()
  }

}