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

abstract class XHTMLBase(ditaDir: File) extends Preprocess(ditaDir) {

  Properties("ant.file.build_generaltargets") = new File("")

  def ditaXhtmlInit() {
    logger.logInfo("\ndita.xhtml.init:")
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()
    if ((!Properties.contains("dita.xhtml.reloadstylesheet"))) {
      Properties("dita.xhtml.reloadstylesheet") = "false"
    }
  }

  /**Build XHTML output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicsXhtml() {
    logger.logInfo("\ndita.topics.xhtml:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      transformer.setParameter("input.map.url", Properties("net.sourceforge.dita-ot.html.map.url"))
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTML files from inner and outer dita topics,which will adjust the directory.  */
  def ditaTopicsHtml() {
    logger.logInfo("\ndita.topics.html:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2html.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build XHTML output from inner dita topics */
  def ditaInnerTopicsXhtml() {
    logger.logInfo("\ndita.inner.topics.xhtml:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      transformer.setParameter("input.map.url", Properties("net.sourceforge.dita-ot.html.map.url"))
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTML files from inner dita topics */
  def ditaInnerTopicsHtml() {
    logger.logInfo("\ndita.inner.topics.html:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2html.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  def checkouterTransform() {
    logger.logInfo("\ncheckouterTransform:")
    if ((Properties("generate.copy.outer") == "2" && (Properties.contains("outditafileslist") && !("" == Properties("outditafileslist"))))) {
      Properties("outer.transform") = "true"
    }
  }

  /**Build XHTML output from outer dita topics */
  def ditaOuterTopicsXhtml() {
    logger.logInfo("\ndita.outer.topics.xhtml:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("outditafileslist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      transformer.setParameter("input.map.url", Properties("net.sourceforge.dita-ot.html.map.url"))
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTML files from outer dita topics */
  def ditaOuterTopicsHtml() {
    logger.logInfo("\ndita.outer.topics.html:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }

    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + File.separator + "xsl" + File.separator + "dita2html.xsl"
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
    val temp_ext = Properties("out.ext")
    val files = job.getSet("outditafileslist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
      }
      if (Properties.contains("args.css.file")) {
        transformer.setParameter("CSS", Properties("args.css.file"))
      }
      if (Properties.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", Properties("user.csspath"))
      }
      if (Properties.contains("args.hdf")) {
        transformer.setParameter("HDF", Properties("args.hdf"))
      }
      if (Properties.contains("args.hdr")) {
        transformer.setParameter("HDR", Properties("args.hdr"))
      }
      if (Properties.contains("args.ftr")) {
        transformer.setParameter("FTR", Properties("args.ftr"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", Properties("args.artlbl"))
      }
      if (Properties.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))
      }
      if (Properties.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))
      }
      if (Properties.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
      if (Properties.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))
      }
      if (Properties.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
      }
      if (Properties.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("FILENAME", in_file.getName())
      transformer.setParameter("FILEDIR", in_file.getParent())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }
}
