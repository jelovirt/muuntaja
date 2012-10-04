package org.dita.dost.module

import scala.collection.JavaConversions._

import scala.io.Source

import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.resolver.DitaURIResolverFactory
import org.dita.dost.util.FileUtils

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

class Dita2troff(ditaDir: File) extends DitaotPreprocess(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.troff/build_dita2troff.xml

  Properties("ant.file.dita2troff") = new File("")
  def dita2troff() {
    println("\ndita2troff:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topic.troff", ditaTopicTroff), ("dita.inner.topic.troff", ditaInnerTopicTroff), ("dita.outer.topic.troff", ditaOuterTopicTroff))

  }
  /**Build troff output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicTroff() {
    println("\ndita.topic.troff:")
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    Properties("dita.ext") = ".dita"

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step2-shell.xsl"))
      val base_dir = new File(Properties("dita.map.output.dir"))
      val dest_dir = new File(Properties("dita.map.output.dir"))
      val temp_ext = ".cli"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  /**Build troff output from inner dita topics */
  def ditaInnerTopicTroff() {
    println("\ndita.inner.topic.troff:")
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    Properties("dita.ext") = ".dita"

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }
    println("the ditmapoutputdir is " + Properties("dita.map.output.dir"))

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step2-shell.xsl"))
      val base_dir = new File(Properties("output.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = ".cli"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  /**Build troff output from outer dita topics */
  def ditaOuterTopicTroff() {
    println("\ndita.outer.topic.troff:")
    History.depends(("troff.checkouterTransform", troffCheckouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    Properties("dita.ext") = ".dita"

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("outditafilesfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

    try {

      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + "/xsl/dita2troff-step2-shell.xsl"))
      val base_dir = new File(Properties("output.dir"))
      val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
      val temp_ext = ".cli"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("outditafilesfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  def troffCheckouterTransform() {
    println("\ntroff.checkouterTransform:")
    if ((Properties("generate.copy.outer") == "2" && (Properties.contains("outditafileslist") && !("" == Properties("outditafileslist"))))) {
      Properties("outer.transform") = "true"
    }

  }

}
