package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.Transformer
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

import org.dita.dost.util.Constants._
import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.util.FileUtils

abstract class XHTMLBase(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.build_generaltargets") = new File("plugins/org.dita.xhtml/build_general.xml")


  def xhtmlInit() {
    logger.logInfo("xhtml.init:")
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
    logger.logInfo("xhtml.topics:")
    depends(("xhtml.init", xhtmlInit), ("xhtml.image-metadata", xhtmlImageMetadata), ("dita.topics.html.common", ditaTopicsHtmlCommon), ("dita.inner.topics.html.common", ditaInnerTopicsHtmlCommon), ("dita.outer.topics.html.common", ditaOuterTopicsHtmlCommon))
  }

  /** Read image metadata */
  def xhtmlImageMetadata() {
    logger.logInfo("xhtml.image-metadata:")
    if (job.getFileInfo.find(_.format == "image").isEmpty) {
      $("xhtml.image-metadata.skip") = "true"
    }

    if ($.contains("xhtml.image-metadata.skip")) {
      return
    }

    import org.dita.dost.module.ImageMetadataModule
    val module = new org.dita.dost.module.ImageMetadataModule
    module.setLogger(new DITAOTJavaLogger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    modulePipelineInput.setAttribute("outputdir", outputDir)
    module.execute(modulePipelineInput)
  }

  def ditaTopicsHtmlCommon() {
    logger.logInfo("dita.topics.html.common:")
    if (!oldTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    val templates = compileTemplates(new File($("args.xsl")))
    val baseDir = ditaTempDir
    val destDir = outputDir
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
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
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", outputDir)
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("FILENAME", inFile.getName)
      transformer.setParameter("FILEDIR", inFile.getParent)
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  def ditaInnerTopicsHtmlCommon() {
    logger.logInfo("dita.inner.topics.html.common:")
    if (!innerTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    val templates = compileTemplates(new File($("args.xsl")))
    val baseDir = ditaTempDir
    val destDir = outputDir
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
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
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", outputDir)
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("FILENAME", inFile.getName)
      transformer.setParameter("FILEDIR", inFile.getParent)
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  def checkouterTransform() {
    logger.logInfo("checkouterTransform:")
    if (($("generate.copy.outer") == "2" && ($.contains("outditafileslist") && "" != $("outditafileslist")))) {
      $("outer.transform") = "true"
    }
  }

  def ditaOuterTopicsHtmlCommon() {
    logger.logInfo("dita.outer.topics.html.common:")
    depends(("checkouterTransform", checkouterTransform))
    if (!$.contains("outer.transform")) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    val templates = compileTemplates(new File($("args.xsl")))
    val baseDir = ditaTempDir
    val destDir = new File(outputDir + File.separator + job.getProperty("uplevels"))
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.isOutDita).map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
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
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", outputDir)
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      transformer.setParameter("input.map.url", $("net.sourceforge.dita-ot.html.map.url"))
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("FILENAME", inFile.getName)
      transformer.setParameter("FILEDIR", inFile.getParent)
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }
}
