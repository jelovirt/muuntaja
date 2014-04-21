package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import org.dita.dost.util.FileUtils

class XHTML(ditaDir: File) extends XHTMLBase(ditaDir) {

  $("ant.file.dita2xhtml") = new File("plugins/org.dita.xhtml/build_dita2xhtml.xml")
  override val transtype = "xhtml"


  def dita2html5Init() {
    logger.info("dita2html5.init:")
    $("html-version") = "html5"
  }

  def dita2html5() {
    logger.info("dita2html5:")
    dita2html5Init()
    buildInit()
    preprocess()
    xhtmlTopics()
    ditaMapXhtml()
    copyCss()
  }

  def dita2xhtmlInit() {
    logger.info("dita2xhtml.init:")
    $("html-version") = "xhtml"
  }

  override def run() {
    dita2xhtmlInit()
    buildInit()
    preprocess()
    xhtmlTopics()
    ditaMapXhtml()
    copyCss()
  }

  def ditaMapXhtml() {
    logger.info("dita.map.xhtml:")
    ditaMapXhtmlInit()
    ditaMapXhtmlToc()
  }

  def ditaMapXhtmlInit() {
    if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
      return
    }
    logger.info("dita.map.xhtml.init:")

    if (!$.contains("args.xhtml.toc.xsl")) {
      $("args.xhtml.toc.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/map2" + $("html-version") + "toc.xsl"
    }
    if (!$.contains("args.xhtml.toc")) {
      $("args.xhtml.toc") = "index"
    }
  }

  /** Build HTML TOC file */
  def ditaMapXhtmlToc() {
    if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
      return
    }
    logger.info("dita.map.xhtml.toc:")

    val templates = compileTemplates(new File($("args.xhtml.toc.xsl")))
    val transformer = templates.newTransformer()
    transformer.setParameter("OUTEXT", $("out.ext"))
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
    val l = new File(job.getInputMap)
    val inFile = new File(ditaTempDir, l.getPath)
    val outDir = if (oldTransform) {
      new File(outputDir, l.getPath).getAbsoluteFile.getParent
    } else {
      new File(outputDir, l.getPath).getAbsoluteFile.getParent + File.separator + job.getProperty("uplevels")
    }
    val outFile = new File(outDir, $("args.xhtml.toc") + $("out.ext")).getCanonicalFile
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val source = getSource(inFile)
    val result = getResult(outFile)
    logger.info("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  def copyRevflag() {
    if (!$.contains("dita.input.valfile")) {
      return
    }
    logger.info("copy-revflag:")

    logger.info(get_msg("DOTA069W"))
  }

  /** Copy CSS files */
  def copyCss() {
    if ($.contains("user.csspath.url")) {
      return
    }
    logger.info("copy-css:")

    val userCsspathReal = new File(outputDir, $("user.csspath"))
    if (!userCsspathReal.exists) {
      userCsspathReal.mkdirs()
    }
    copy(new File($("dita.plugin.org.dita.xhtml.dir"), "resource"),
      userCsspathReal,
      Set("*.css"))
    if ($("args.copycss") == "yes" && $.contains("args.css.present")) {
      FileUtils.copyFile(new File($("args.css.real")), new File(userCsspathReal, new File($("args.css.real")).getName))
    }
  }

}
