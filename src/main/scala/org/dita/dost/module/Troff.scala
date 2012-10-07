package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.resolver.DitaURIResolverFactory
import org.dita.dost.util.FileUtils

class Troff(ditaDir: File) extends Preprocess(ditaDir) {

  Properties("ant.file.dita2troff") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topic.troff", ditaTopicTroff), ("dita.inner.topic.troff", ditaInnerTopicTroff), ("dita.outer.topic.troff", ditaOuterTopicTroff))
  }

  /**Build troff output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicTroff() {
    logger.logInfo("\ndita.topic.troff:")
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    Properties("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val files = job.getSet("fullditatopiclist")
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
        logger.logInfo("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File(Properties("dita.map.output.dir"))
    val dest_dir = new File(Properties("dita.map.output.dir"))
    val temp_ext = ".cli"
    val files = job.getSet("fullditatopiclist")
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
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build troff output from inner dita topics */
  def ditaInnerTopicTroff() {
    logger.logInfo("\ndita.inner.topic.troff:")
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    Properties("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val files = job.getSet("fullditatopiclist")
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
        logger.logInfo("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
    }
    logger.logInfo("the ditmapoutputdir is " + Properties("dita.map.output.dir"))
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File(Properties("output.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = ".cli"
    val files = job.getSet("fullditatopiclist")
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
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build troff output from outer dita topics */
  def ditaOuterTopicTroff() {
    logger.logInfo("\ndita.outer.topic.troff:")
    History.depends(("troff.checkouterTransform", troffCheckouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    Properties("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("dita.ext")
      val files = job.getSet("outditafileslist")
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
        logger.logInfo("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File(Properties("output.dir"))
    val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
    val temp_ext = ".cli"
    val files = job.getSet("outditafileslist")
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
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  def troffCheckouterTransform() {
    logger.logInfo("\ntroff.checkouterTransform:")
    if ((Properties("generate.copy.outer") == "2" && (Properties.contains("outditafileslist") && "" != Properties("outditafileslist")))) {
      Properties("outer.transform") = "true"
    }
  }
}
