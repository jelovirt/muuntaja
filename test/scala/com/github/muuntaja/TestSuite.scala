package com.github.muuntaja

import org.scalatest.{Suite, SuperSuite}
import java.io.{File, FilenameFilter}
import java.util.logging.{ConsoleHandler, Level, SimpleFormatter}

class MuuntajaSuite(src: File, tmp: File) extends SuperSuite(
  List (
     new DitaSuite,
     new DitaTypeSuite,
     new DitaElementSuite,
     //new PreprocessorSuite(src, tmp),
     new XOMSuite,
     new XMLUtilsSuite(src)
   )
)

object MuuntajaSuiteMain {
  def main(args : Array[String]) {
    val src = new File("/Users/jelovirt/Work/personal/muuntaja")
    val tmp = new File("/Users/jelovirt/Temp/muuntaja/work")
    (new MuuntajaSuite(src, tmp)).execute
  }
}
