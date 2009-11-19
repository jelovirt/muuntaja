package com.github.muuntaja

import java.io.{File, FilenameFilter}
import java.util.logging.{ConsoleHandler, Level}

object Main {
  def main(args: Array[String]) {
    val temp = new File("/Users/jelovirt/Temp/muuntaja/work")
    val resource = new File("/Users/jelovirt/Work/personal/muuntaja/src/plugins/dita")
    val processor = new Processor(resource, temp, false)
    processor.logger.addHandler(new ConsoleHandler)
    processor.logger.setLevel(Level.FINE)
    processor.run((new File(args(0))).toURI)
  }
}

class ProcessTester(val src: File, val tmp: File) {
  val resource = new File(src, "src/plugins/dita")
  val tests = new File(src, "test/xml")

  val utils = new XMLUtils
  utils.catalogFiles(new File(resource, "dita" + File.separator + "catalog.xml"))
  
  def run(args : Array[String]) {
    val files = for {
                  n <- tests.listFiles
                  if n.isDirectory
                  if tests.length != 0 || args.exists(_ == n.getName)
                  val source = new File(n, "in" + File.separator + "test.ditamap")
                  if source.exists
                } yield source
    process(files)
  }
  
  private def process(sources: Array[File]) {
    for (source <- sources; val parent = source.getParentFile.getName) {
      val expected = new File(tests, parent + File.separator + "out")
      val actual = new File(tmp, parent + File.separator + "out")
      val actualOt = new File(tmp, parent + File.separator + "ot-out")
      val ditaot = new File(tests, parent + File.separator + "in" + File.separator + "temp")
      // convert
      val t = new File(tmp, parent + File.separator + "out")
      val processor = new Processor(resource, t, false)
      processor.logger.addHandler(new ConsoleHandler)
      processor.logger.setLevel(Level.FINE)
      processor.run(source.toURI)
      val tt = new File(tmp, parent + File.separator + "ot-out")
      val otProcessor = new Processor(resource, tt, true)
      otProcessor.logger.addHandler(new ConsoleHandler)
      otProcessor.logger.setLevel(Level.FINE)
      otProcessor.run(source.toURI)
      // compare
      compare(actual, expected, "expected OT compatible output", false)
      compare(actualOt, expected, "expected default output", true)
      compare(actual, ditaot, "DITA-OT output", true)
    }
  }
  
  private def compare(actual: File, expected: File, desc: String, otCompatibility: Boolean) {
    object Filter extends FilenameFilter {
      def accept(base: File, name: String): Boolean = {
        return (name.endsWith("xml") || name.endsWith("dita") || name.endsWith("ditamap"))
      }
    }
    
    for (act <- actual.listFiles(Filter)) {
        //if (exp.isFile) {
        if (act.isFile) {
          val a = XMLUtils.parse(act.toURI)
          //val act = new File(actual, exp.getName)
          val exp = new File(expected, act.getName)
          if (exp.exists) {
            (utils.parseResolving(exp.toURI, true), a) match {
              case (Some(dExp), Some(dAct)) => {
                println("Comparing to " + desc + ": " + act)
                assert(DitaComparer.compare(dExp, dAct, List(Preprocessor.MUUNTAJA_NS), otCompatibility))
              }
              case _ => println("ERROR: Failed to parse comparable files")
            }
          }
          /*
          val dot = new File(ditaot, act.getName)
          if (dot.exists) {
            (utils.parseResolving(dot.toURI, true), a) match {
              case (Some(dExp), Some(dAct)) => {
                println("Comparing to DITA-OT: " + act)
                assert(DitaComparer.compare(dExp, dAct, List(Preprocessor.MUUNTAJA_NS), true))
              }
              case _ => fail("Failed to parse comparable files")
            }
          }
          */
        }
      }
  }
}
object ProcessTest {
  def main(args: Array[String]) {
    val src = new File("/Users/jelovirt/Work/personal/muuntaja")
    val tmp = new File("/Users/jelovirt/Temp/muuntaja/work")
    val m = new ProcessTester(src, tmp)
    m.run(args)
  }
}