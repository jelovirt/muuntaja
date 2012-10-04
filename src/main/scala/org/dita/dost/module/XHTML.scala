package org.dita.dost.module

import scala.collection.JavaConversions._

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

class XHTML(ditaDir: File) extends XHTMLBase(ditaDir) {

  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.xhtml/build_dita2xhtml.xml

  Properties("ant.file.dita2xhtml") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.map.xhtml", ditaMapXhtml), ("copy-css", copyCss), ("dita.topics.xhtml", ditaTopicsXhtml), ("dita.inner.topics.xhtml", ditaInnerTopicsXhtml), ("dita.outer.topics.xhtml", ditaOuterTopicsXhtml))
  }

  def ditaMapXhtml() {
    logger.logInfo("\ndita.map.xhtml:")
    History.depends(("dita.map.xhtml.init", ditaMapXhtmlInit), ("dita.map.xhtml.toc", ditaMapXhtmlToc), ("dita.out.map.xhtml.toc", ditaOutMapXhtmlToc))
  }

  def ditaMapXhtmlInit() {
    logger.logInfo("\ndita.map.xhtml.init:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (Properties.contains("noMap")) {
      return
    }

    if ((!Properties.contains("args.xhtml.toc.xsl"))) {
      Properties("args.xhtml.toc.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/map2xhtmtoc.xsl"
    }
    if ((!Properties.contains("args.xhtml.toc"))) {
      Properties("args.xhtml.toc") = "index"
    }
  }

  /**Build HTML TOC file */
  def ditaMapXhtmlToc() {
    logger.logInfo("\ndita.map.xhtml.toc:")
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    val templates = compileTemplates(new File(Properties("args.xhtml.toc.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      if (Properties.contains("args.xhtml.contenttarget")) {
        transformer.setParameter("contenttarget", Properties("args.xhtml.contenttarget"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.xhtml.toc.class")) {
        transformer.setParameter("OUTPUTCLASS", Properties("args.xhtml.toc.class"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.xhtml.toc") + Properties("out.ext")))
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
    logger.logInfo("\ndita.out.map.xhtml.toc:")
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    val templates = compileTemplates(new File(Properties("args.xhtml.toc.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      if (Properties.contains("args.xhtml.contenttarget")) {
        transformer.setParameter("contenttarget", Properties("args.xhtml.contenttarget"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.xhtml.toc.class")) {
        transformer.setParameter("OUTPUTCLASS", Properties("args.xhtml.toc.class"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.xhtml.toc") + Properties("out.ext")))
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
    logger.logInfo("\ncopy-revflag:")
    if (!Properties.contains("dita.input.valfile")) {
      return
    }

    logger.logInfo(get_msg("DOTA069W"))
  }

  /**Copy CSS files */
  def copyCss() {
    logger.logInfo("\ncopy-css:")
    if (Properties.contains("user.csspath.url")) {
      return
    }

    if ((Properties("args.copycss") == "yes" && Properties.contains("args.css.present"))) {
      Properties("user.copycss.yes") = "true"
    }
    Properties("user.csspath.real") = new File(Properties("output.dir") + "/" + Properties("user.csspath"))
    if (!new File(Properties("user.csspath.real")).exists()) {
      new File(Properties("user.csspath.real")).mkdirs()
    }
    copy(Properties("dita.resource.dir"), Properties("user.csspath.real"), "*.css")
    copyCssUser()
  }

  def copyCssUser() {
    logger.logInfo("\ncopy-css-user:")
    if (!Properties.contains("user.copycss.yes")) {
      return
    }

  }
}
