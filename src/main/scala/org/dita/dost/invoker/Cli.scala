package org.dita.dost.invoker

import java.io.File

import org.dita.dost.module._

object Cli {

  private val usage = "Usage: scala Cli <transtype> <input>"

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println(usage)
    }
    val arglist = args.toList
    type OptionMap = Map[String, String]

    def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
      def parseProperty(s: String): (String, String) = {
        val i = s.indexOf('=')
        s.substring(2, i) -> s.substring(i + 1)
      }
      list match {
        case Nil =>
          map
        case "--input" :: value :: tail =>
          nextOption(map ++ Map("args.input" -> new File(value).getAbsolutePath), tail)
        case "-i" :: value :: tail =>
          nextOption(map ++ Map("args.input" -> new File(value).getAbsolutePath), tail)
        case "--format" :: value :: tail =>
          nextOption(map ++ Map("transtype" -> value), tail)
        case "-f" :: value :: tail =>
          nextOption(map ++ Map("transtype" -> value), tail)
        case "--output" :: value :: tail =>
          nextOption(map ++ Map("output.dir" -> new File(value).getAbsolutePath), tail)
        case "-o" :: value :: tail =>
          nextOption(map ++ Map("output.dir" -> new File(value).getAbsolutePath), tail)
        case value :: tail if value.startsWith("-D") =>
          nextOption(map + parseProperty(value), tail)
        //        case string :: opt2 :: tail if isSwitch(opt2) =>
//          nextOption(map ++ Map('infile -> string), list.tail)
//        case string :: Nil =>
//          nextOption(map ++ Map('infile -> string), list.tail)
        case option :: tail =>
          throw new IllegalArgumentException("Unknown option " + option)
//          sys.exit(1)
      }
    }
    val options = nextOption(Map(),arglist)
    println(options)
    val ditaDir = System.getenv("DITA_HOME") match {
      case null => new File(".").getCanonicalFile
      case d => new File(d).getCanonicalFile
    }
    //val xhtml = new XHTML(ditaDir)
    val processor = options("transtype") match {
      case "test" => new Test(ditaDir)
      case "xhtml" => {
        val p = new XHTML(ditaDir)
        p.$("html-version") = "xhtml"
        p
      }
      case "html5" => {
        val p = new XHTML(ditaDir)
        p.$("html-version") = "html5"
        p
      }
      case "rtf" => new WordRTF(ditaDir)
    }
    options.foreach(e => processor.$(e._1) = e._2)
    //processor.$("args.input") = new File(options('input)).getAbsolutePath()
    //processor.$("generate.copy.outer") = "1"
    processor.run()
  }

}