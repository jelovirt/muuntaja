package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File

import javax.xml.transform.Transformer

import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.util.FileUtils._

abstract class XHTMLBase(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.build_generaltargets") = new File("plugins/org.dita.xhtml/build_general.xml")

  def xhtmlInit() {
    logger.info("xhtml.init:")
    if ($.contains("args.ftr") && !new File($("args.ftr")).exists) {
      throw new IllegalArgumentException("DOTA007E")
    }
    if ($.contains("args.hdr") && !new File($("args.hdr")).exists) {
      throw new IllegalArgumentException("DOTA008E")
    }
    if ($.contains("args.hdf") && !new File($("args.hdf")).exists) {
      throw new IllegalArgumentException("DOTA009E")
    }
    if ($("args.csspath").startsWith("http://") || $("args.csspath").startsWith("https://")) {
      $("user.csspath.url") = "true"
    }
    if (new File($("args.csspath")).isAbsolute) {
      $("args.csspath.absolute") = "true"
    }
    if (!$.contains("args.csspath") || $.contains("args.csspath.absolute")) {
      $("user.csspath") = ""
    }
    if (!$.contains("user.csspath")) {
      $("user.csspath") = $("args.csspath") + "/"
    }
    if ($.contains("args.cssroot")) {
      $("args.css.real") = $("args.cssroot") + $("file.separator") + $("args.css")
    }
    if (!$.contains("args.cssroot")) {
      $("args.css.real") = $("args.css")
    }
    if (new File($("args.css.real")).exists && new File($("args.css.real")).isFile) {
      $("args.css.present") = "true"
    }
    $("args.css.file.temp") = new File($("args.css")).getName
    if ($.contains("args.css.present") || $.contains("user.csspath.url")) {
      $("args.css.file") = $("args.css.file.temp")
    }
    if (!$.contains("out.ext")) {
      $("out.ext") = ".html"
    }
    if (!$.contains("html-version")) {
      $("html-version") = "xhtml"
    }
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2" + $("html-version") + ".xsl"
    }
    $("dita.input.valfile.url") = new File($("dita.input.valfile")).toURI.toASCIIString
    if (!$.contains("dita.xhtml.reloadstylesheet")) {
      $("dita.xhtml.reloadstylesheet") = "false"
    }
  }

  def xhtmlTopics() {
    logger.info("xhtml.topics:")
    xhtmlInit()
    xhtmlImageMetadata()
    ditaTopicsHtmlCommon()
    ditaInnerTopicsHtmlCommon()
  }

  /** Read image metadata */
  def xhtmlImageMetadata() {
    if ($.contains("xhtml.image-metadata.skip")) {
      return
    }
    logger.info("xhtml.image-metadata:")

    if (job.getFileInfo.exists(_.format == "image")) {
      val module = new ImageMetadataModule
      module.setLogger(logger)
      module.setJob(job)
      val modulePipelineInput = new PipelineHashIO
      modulePipelineInput.setAttribute("tempDir", ditaTempDir)
      modulePipelineInput.setAttribute("outputdir", outputDir)
      module.execute(modulePipelineInput)
    }
  }

  def ditaTopicsHtmlCommon() {
    if (!oldTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }
    logger.info("dita.topics.html.common:")

    val templates = compileTemplates(new File($("args.xsl")))
    val files = job.getFileInfo.filter(f => f.format == "dita" && !f.isResourceOnly).map(_.file).toSet
    var transformer: Transformer = if (!$("dita.xhtml.reloadstylesheet").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.xhtml.reloadstylesheet").toBoolean) {
        transformer = templates.newTransformer()
      }
      transformer.setParameter("TRANSTYPE", transtype)
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
      transformer.setParameter("OUTEXT", $("out.ext"))
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", outputDir)
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
      val inFile = new File(ditaTempDir, l.getPath)
      val outFile = new File(outputDir, replaceExtension(l.getPath, $("out.ext")))
      transformer.setParameter("FILENAME", inFile.getName)
      transformer.setParameter("FILEDIR", inFile.getParent)
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.info("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  def ditaInnerTopicsHtmlCommon() {
    if (!innerTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }
    logger.info("dita.inner.topics.html.common:")

    val templates = compileTemplates(new File($("args.xsl")))

    val filterPrefix = new File(job.getInputMap).getParent match {
      case null => ""
      case p => p + File.separator
    }
    logger.info("\n" + filterPrefix + "\n")
    val files = job.getFileInfo.filter(f => f.format == "dita" && !f.isResourceOnly && f.file.getPath.startsWith(filterPrefix)).map(_.file).toSet
    var transformer: Transformer = if (!$("dita.xhtml.reloadstylesheet").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.xhtml.reloadstylesheet").toBoolean) {
        transformer = templates.newTransformer()
      }
      transformer.setParameter("TRANSTYPE", transtype)
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
      transformer.setParameter("OUTEXT", $("out.ext"))
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", outputDir)
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
      val inFile = new File(ditaTempDir, l.getPath)
      val outFile = new File(outputDir,  replaceExtension(l.getPath.substring(filterPrefix.length), $("out.ext")))
      transformer.setParameter("FILENAME", inFile.getName)
      transformer.setParameter("FILEDIR", inFile.getParent)
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.info("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }
}
