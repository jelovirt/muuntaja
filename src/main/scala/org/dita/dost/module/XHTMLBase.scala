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

abstract class XHTMLBase(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.build_generaltargets") = new File("")

  def ditaXhtmlInit() {
    logger.logInfo("dita.xhtml.init:")
    if (!$.contains("out.ext")) {
      $("out.ext") = ".html"
    }
    $("dita.input.valfile.url") = new File($("dita.input.valfile")).toURI().toASCIIString()
    if (!$.contains("dita.xhtml.reloadstylesheet")) {
      $("dita.xhtml.reloadstylesheet") = "false"
    }
  }

  /**Build XHTML output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicsXhtml() {
    depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!oldTransform) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.topics.xhtml:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = $("out.ext")
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
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
    depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!oldTransform) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.topics.html:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = $("out.ext")
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
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
    depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!innerTransform) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.inner.topics.xhtml:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = $("out.ext")
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
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
    depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!innerTransform) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.inner.topics.html:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = $("out.ext")
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
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
    logger.logInfo("checkouterTransform:")
    if (($("generate.copy.outer") == "2" && ($.contains("outditafileslist") && "" != $("outditafileslist")))) {
      $("outer.transform") = "true"
    }
  }

  /**Build XHTML output from outer dita topics */
  def ditaOuterTopicsXhtml() {
    depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!$.contains("outer.transform")) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.outer.topics.xhtml:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir") + File.separator + $("uplevels"))
    val temp_ext = $("out.ext")
    val files = job.getSet("outditafileslist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
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
    depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!$.contains("outer.transform")) {
      return
    }
    if (noTopic) {
      return
    }

    logger.logInfo("dita.outer.topics.html:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir") + File.separator + $("uplevels"))
    val temp_ext = $("out.ext")
    val files = job.getSet("outditafileslist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("TRANSTYPE", $("transtype"))
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("dita.input.valfile")) {
        transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
      }
      if ($.contains("args.css.file")) {
        transformer.setParameter("CSS", $("args.css.file"))
      }
      if ($.contains("user.csspath")) {
        transformer.setParameter("CSSPATH", $("user.csspath"))
      }
      if ($.contains("args.hdf")) {
        transformer.setParameter("HDF", $("args.hdf"))
      }
      if ($.contains("args.hdr")) {
        transformer.setParameter("HDR", $("args.hdr"))
      }
      if ($.contains("args.ftr")) {
        transformer.setParameter("FTR", $("args.ftr"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      if ($.contains("args.artlbl")) {
        transformer.setParameter("ARTLBL", $("args.artlbl"))
      }
      if ($.contains("args.gen.task.lbl")) {
        transformer.setParameter("GENERATE-TASK-LABELS", $("args.gen.task.lbl"))
      }
      if ($.contains("args.xhtml.classattr")) {
        transformer.setParameter("PRESERVE-DITA-CLASS", $("args.xhtml.classattr"))
      }
      if ($.contains("args.hide.parent.link")) {
        transformer.setParameter("NOPARENTLINK", $("args.hide.parent.link"))
      }
      transformer.setParameter("include.rellinks", $("include.rellinks"))
      if ($.contains("args.breadcrumbs")) {
        transformer.setParameter("BREADCRUMBS", $("args.breadcrumbs"))
      }
      if ($.contains("args.indexshow")) {
        transformer.setParameter("INDEXSHOW", $("args.indexshow"))
      }
      if ($.contains("args.gen.default.meta")) {
        transformer.setParameter("genDefMeta", $("args.gen.default.meta"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
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
