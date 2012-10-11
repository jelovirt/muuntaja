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

class XHTML(ditaDir: File) extends XHTMLBase(ditaDir) {

  $("ant.file.dita2xhtml") = new File("")
  override val transtype = "xhtml"

  override def run() {
    logger.logInfo("run:")
    depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.map.xhtml", ditaMapXhtml), ("copy-css", copyCss), ("dita.topics.xhtml", ditaTopicsXhtml), ("dita.inner.topics.xhtml", ditaInnerTopicsXhtml), ("dita.outer.topics.xhtml", ditaOuterTopicsXhtml))
  }

  def ditaMapXhtml() {
    logger.logInfo("dita.map.xhtml:")
    depends(("dita.map.xhtml.init", ditaMapXhtmlInit), ("dita.map.xhtml.toc", ditaMapXhtmlToc), ("dita.out.map.xhtml.toc", ditaOutMapXhtmlToc))
  }

  def ditaMapXhtmlInit() {
    logger.logInfo("dita.map.xhtml.init:")
    depends(("dita.xhtml.init", ditaXhtmlInit))
    if (noMap) {
      return
    }

    if (!$.contains("args.xhtml.toc.xsl")) {
      $("args.xhtml.toc.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/map2xhtmtoc.xsl"
    }
    if (!$.contains("args.xhtml.toc")) {
      $("args.xhtml.toc") = "index"
    }
  }

  /**Build HTML TOC file */
  def ditaMapXhtmlToc() {
    logger.logInfo("dita.map.xhtml.toc:")
    if (!oldTransform) {
      return
    }
    if (noMap) {
      return
    }

    val templates = compileTemplates(new File($("args.xhtml.toc.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = Set(job.getProperty(INPUT_DITAMAP)) -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      if ($.contains("args.xhtml.contenttarget")) {
        transformer.setParameter("contenttarget", $("args.xhtml.contenttarget"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.xhtml.toc.class")) {
        transformer.setParameter("OUTPUTCLASS", $("args.xhtml.toc.class"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("args.xhtml.toc") + $("out.ext")))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTML TOC file,which will adjust the directory */
  def ditaOutMapXhtmlToc() {
    logger.logInfo("dita.out.map.xhtml.toc:")
    if (!innerTransform) {
      return
    }
    if (noMap) {
      return
    }

    val templates = compileTemplates(new File($("args.xhtml.toc.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = Set(job.getProperty(INPUT_DITAMAP)) -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      if ($.contains("args.xhtml.contenttarget")) {
        transformer.setParameter("contenttarget", $("args.xhtml.contenttarget"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.xhtml.toc.class")) {
        transformer.setParameter("OUTPUTCLASS", $("args.xhtml.toc.class"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), job.getProperty(INPUT_DITAMAP), $("args.xhtml.toc") + $("out.ext")))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  def copyRevflag() {
    logger.logInfo("copy-revflag:")
    if (!$.contains("dita.input.valfile")) {
      return
    }

    logger.logInfo(get_msg("DOTA069W"))
  }

  /**Copy CSS files */
  def copyCss() {
    logger.logInfo("copy-css:")
    if ($.contains("user.csspath.url")) {
      return
    }

    if (($("args.copycss") == "yes" && $.contains("args.css.present"))) {
      $("user.copycss.yes") = "true"
    }
    $("user.csspath.real") = new File($("output.dir") + File.separator + $("user.csspath"))
    if (!new File($("user.csspath.real")).exists()) {
      new File($("user.csspath.real")).mkdirs()
    }
    copy(new File($("dita.plugin.org.dita.xhtml.dir") + File.separator + "resource"), new File($("user.csspath.real")), Set("*.css"))
    copyCssUser()
  }

  def copyCssUser() {
    logger.logInfo("copy-css-user:")
    if (!$.contains("user.copycss.yes")) {
      return
    }

  }
}
