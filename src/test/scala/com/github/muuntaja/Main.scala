package com.github.muuntaja


import java.io.{File, FilenameFilter}
import java.util.logging.{Logger, ConsoleHandler, Level}
import org.apache.tools.ant.Task


class ProcessRunner(src: File, tmp: File) {
  var validate: Boolean = false
  var otCompatibility: Boolean = false
  
  private val resource = new File(src, "src/main/resources")
  private val tests = new File(src, "src/test/xml")
  private val catalog = new File(resource, "dtd" + File.separator + "catalog.xml")
  
  private val utils = new XMLUtils
  utils.catalogFiles(catalog)
  
  private val logger = Logger.getAnonymousLogger()
  logger.setUseParentHandlers(false)
  logger.addHandler(new ConsoleHandler)
  logger.setLevel(Level.INFO)
    
  def run(args : Array[String]) {
    val files = for {
                  n <- tests.listFiles
                  if (n.isDirectory)
                  if (args.length == 0 || args.exists(_ == n.getName))
                  source = new File(n, "in" + File.separator + "test.ditamap")
                  if (source.exists)
                } yield source
    process(files)
  }
  
  private def process(sources: Array[File]) {
    for (source <- sources; val parent = source.getParentFile.getParentFile.getName) {
      val expected = new File(tests, parent + File.separator + "out")
      val expectedOt = new File(tmp, parent + File.separator + "ot.out")
      logger.info("Process " + source.getAbsolutePath)
      // convert
      val actual = new File(tmp, parent + File.separator + "act")
      val processor = new Processor(catalog, actual, false, logger)
      processor.run(source.toURI)
      val actualOt = new File(tmp, parent + File.separator + "ot.act")
      if (otCompatibility) {
        val otProcessor = new Processor(catalog, actualOt, true, logger)
        otProcessor.run(source.toURI)
      }
      // compare
      if (validate) {
        compare(actual, expected, "expected default output", false)
        if (otCompatibility && expectedOt.exists) {
          compare(actualOt, expectedOt, "expected OT compatible output", true)
        }
      }
    }
  }
  
  private def compare(actual: File, expected: File, desc: String, otCompatibility: Boolean) {
    object Filter extends FilenameFilter {
      def accept(base: File, name: String): Boolean = {
        return (name.endsWith("xml") || name.endsWith("dita") || name.endsWith("ditamap"))
      }
    }
    
    for (act <- actual.listFiles(Filter)) {
      if (act.isFile) {
        val a = XMLUtils.parse(act.toURI)
        val exp = new File(expected, act.getName)
        if (exp.exists) {
          (utils.parseResolving(exp.toURI, true), a) match {
            case (Some(dExp), Some(dAct)) => {
              logger.info("Comparing to " + desc + ": " + act)
              assert(DitaComparer.compare(dExp, dAct,
                                          if (otCompatibility) List(Preprocessor.MUUNTAJA_NS) else Nil,
                                          otCompatibility))
            }
            case _ => logger.severe("ERROR: Failed to parse comparable files")
          }
        }
      }
    }
  }
}

object Main {
  def main(args: Array[String]) {
    val src = new File(System.getenv("HOME") + File.separator + "Work/personal/muuntaja")
    val tmp = new File(System.getenv("HOME") + File.separator + "Temp/muuntaja/work")
    
    val m = new ProcessRunner(src, tmp)
    for (a <- args.filter(_.startsWith("-"))) {
      a match {
        case "-v" => m.validate = true
        case "-o" => m.otCompatibility = true
      }
    }
    m.run(args.filter(a => !(a.startsWith("-"))))
  }
}
