package com.github.muuntaja

import org.scalatest.{Suite, SuperSuite}
import java.io.{File, FilenameFilter}
import java.util.logging.{ConsoleHandler, Level, SimpleFormatter}

class MuuntajaSuiteRunner(src: File, tmp: File) extends SuperSuite(
  List (
     new DitaSuite,
     new DitaURISuite,
     new DitaTypeSuite,
     new DitaElementSuite,
     new XOMSuite,
     //new XMLUtilsSuite(src)
     new XMLUtilsSuite
   )
)

object MuuntajaSuiteMain {
  def main(args : Array[String]) {
    val src = new File("/Users/jelovirt/Work/personal/muuntaja")
    val tmp = new File("/Users/jelovirt/Temp/muuntaja/work")
    (new MuuntajaSuiteRunner(src, tmp)).execute
  }
}
