package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

import org.dita.dost.util.Constants._
import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.resolver.DitaURIResolverFactory
import org.dita.dost.util.FileUtils

class Troff(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2troff") = new File("")
  override val transtype = "troff"

  override def run() {
    logger.logInfo("run:")
    depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topic.troff", ditaTopicTroff), ("dita.inner.topic.troff", ditaInnerTopicTroff), ("dita.outer.topic.troff", ditaOuterTopicTroff))
  }

  /**Build troff output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicTroff() {
    logger.logInfo("dita.topic.troff:")
    if (!oldTransform) {
      return
    }
    if (noTopic) {
      return
    }

    $("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File($("dita.temp.dir"))
      val dest_dir = new File($("output.dir"))
      val temp_ext = $("dita.ext")
      val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
      for (l <- files) {
        val transformer = templates.newTransformer()
        if ($.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", $("dita.ext"))
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
    val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File($("dita.map.output.dir"))
    val dest_dir = new File($("dita.map.output.dir"))
    val temp_ext = ".cli"
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
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
    logger.logInfo("dita.inner.topic.troff:")
    if (!innerTransform) {
      return
    }
    if (noTopic) {
      return
    }

    $("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File($("dita.temp.dir"))
      val dest_dir = new File($("output.dir"))
      val temp_ext = $("dita.ext")
      val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
      for (l <- files) {
        val transformer = templates.newTransformer()
        if ($.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", $("dita.ext"))
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
    logger.logInfo("the ditmapoutputdir is " + $("dita.map.output.dir"))
    val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File($("output.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = ".cli"
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
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
    logger.logInfo("dita.outer.topic.troff:")
    depends(("troff.checkouterTransform", troffCheckouterTransform))
    if (!$.contains("outer.transform")) {
      return
    }
    if (noTopic) {
      return
    }

    $("dita.ext") = ".dita"
    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl"))
      val base_dir = new File($("dita.temp.dir"))
      val dest_dir = new File($("output.dir"))
      val temp_ext = $("dita.ext")
      val files = job.getSet("outditafileslist") -- job.getSet("resourceonlylist")
      for (l <- files) {
        val transformer = templates.newTransformer()
        if ($.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", $("dita.ext"))
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
    val templates = compileTemplates(new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl"))
    val base_dir = new File($("output.dir"))
    val dest_dir = new File($("output.dir") + File.separator + $("uplevels"))
    val temp_ext = ".cli"
    val files = job.getSet("outditafileslist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
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
    logger.logInfo("troff.checkouterTransform:")
    if (($("generate.copy.outer") == "2" && ($.contains("outditafileslist") && "" != $("outditafileslist")))) {
      $("outer.transform") = "true"
    }
  }
}
