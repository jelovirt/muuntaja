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
import org.dita.dost.resolver.DitaURIResolverFactory
import org.dita.dost.util.FileUtils

class HTMLHelp(ditaDir: File) extends XHTML(ditaDir) {

  $("ant.file.dita2htmlhelp") = new File("")
  override val transtype = "htmlhelp"

  override def run() {
    logger.logInfo("run:")
    depends(("dita2htmlhelp.init", dita2htmlhelpInit), ("build-init", buildInit), ("use-init.envhhcdir", useInitEnvhhcdir), ("use-init.hhcdir", useInitHhcdir), ("preprocess", preprocess), ("copy-css", copyCss), ("xhtml.topics", xhtmlTopics))
    if (job.getFileInfo().find(_.format == "ditamap").isEmpty) {
      return
    }

    ditaMapHtmlhelp()
    ditaHtmlhelpConvertlang()
    compileHTMLHelp()
  }

  def dita2htmlhelpInit() {
    logger.logInfo("dita2htmlhelp.init:")
    $("html-version") = "html"
  }

  def useInitEnvhhcdir() {
    logger.logInfo("use-init.envhhcdir:")
    if (!$.contains("env.HHCDIR")) {
      return
    }

    if (new File($("env.HHCDIR") + File.separator + "hhc.exe").exists()) {
      $("HTMLHelpCompiler") = $("env.HHCDIR") + $("file.separator") + "hhc.exe"
    }
  }

  def useInitHhcdir() {
    logger.logInfo("use-init.hhcdir:")
    if ($.contains("env.HHCDIR")) {
      return
    }

    if (new File("C:\\Program Files (x86)\\HTML Help Workshop").exists()) {
      $("hhc.dir") = "C:\\Program Files (x86)\\HTML Help Workshop"
    } else {
      $("hhc.dir") = "C:\\Program Files\\HTML Help Workshop"
    }
    if (new File($("hhc.dir") + File.separator + "hhc.exe").exists()) {
      $("HTMLHelpCompiler") = $("hhc.dir") + $("file.separator") + "hhc.exe"
    }
  }

  def ditaMapHtmlhelp() {
    logger.logInfo("dita.map.htmlhelp:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit), ("dita.map.htmlhelp.hhp", ditaMapHtmlhelpHhp), ("dita.map.htmlhelp.hhc", ditaMapHtmlhelpHhc), ("dita.map.htmlhelp.hhk", ditaMapHtmlhelpHhk), ("dita.out.map.htmlhelp.hhp", ditaOutMapHtmlhelpHhp), ("dita.out.map.htmlhelp.hhc", ditaOutMapHtmlhelpHhc), ("dita.out.map.htmlhelp.hhk", ditaOutMapHtmlhelpHhk))
  }

  /**Init properties for HTMLHelp */
  def ditaMapHtmlhelpInit() {
    logger.logInfo("dita.map.htmlhelp.init:")
    if (!$.contains("out.ext")) {
      $("out.ext") = ".html"
    }
  }

  /**Build HTMLHelp HHP file */
  def ditaMapHtmlhelpHhp() {
    logger.logInfo("dita.map.htmlhelp.hhp:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhp.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val tempExt = ".hhp"
    val files = Set(new File(job.getInputMap)) -- job.getFileInfo().filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("HHCNAME", $("dita.map.filename.root") + ".hhc")
      if ($.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", $("args.htmlhelp.includefile"))
      }
      val inFile = new File(baseDir, l.getPath())
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHP file */
  def ditaOutMapHtmlhelpHhp() {
    logger.logInfo("dita.out.map.htmlhelp.hhp:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhp.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val files = Set(new File(job.getInputMap)) -- job.getFileInfo().filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("HHCNAME", $("dita.map.filename.root") + ".hhc")
      if ($.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", $("args.htmlhelp.includefile"))
      }
      val inFile = new File(baseDir, l.getPath())
      val outFile = new File(globMap(new File(destDir, l.getPath()).getAbsolutePath(), job.getInputMap(), $("dita.map.filename.root") + ".hhp"))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHC file */
  def ditaMapHtmlhelpHhc() {
    logger.logInfo("dita.map.htmlhelp.hhc:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhc.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val tempExt = ".hhc"
    val files = Set(new File(job.getInputMap)) -- job.getFileInfo().filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val inFile = new File(baseDir, l.getPath())
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHC file */
  def ditaOutMapHtmlhelpHhc() {
    logger.logInfo("dita.out.map.htmlhelp.hhc:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhc.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val files = Set(new File(job.getInputMap)) -- job.getFileInfo().filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val inFile = new File(baseDir, l.getPath())
      val outFile = new File(globMap(new File(destDir, l.getPath()).getAbsolutePath(), job.getInputMap(), $("dita.map.filename.root") + ".hhc"))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHK file */
  def ditaMapHtmlhelpHhk() {
    logger.logInfo("dita.map.htmlhelp.hhk:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getInputMap())
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + job.getInputMap())
    modulePipelineInput.setAttribute("targetext", $("out.ext"))
    modulePipelineInput.setAttribute("indextype", "htmlhelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build HTMLHelp HHK file */
  def ditaOutMapHtmlhelpHhk() {
    logger.logInfo("dita.out.map.htmlhelp.hhk:")
    depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getInputMap())
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + $("dita.map.filename.root") + ".hhk")
    modulePipelineInput.setAttribute("targetext", $("out.ext"))
    modulePipelineInput.setAttribute("indextype", "htmlhelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  def ditaHtmlhelpConvertlang() {
    logger.logInfo("dita.htmlhelp.convertlang:")
  }

  /**Compile HTMLHelp output */
  def compileHTMLHelp() {
    logger.logInfo("compile.HTML.Help:")
    if (!$.contains("HTMLHelpCompiler")) {
      return
    }

    if (innerTransform) {
      $("compile.dir") = $("output.dir")
    }
    if (oldTransform) {
      $("compile.dir") = $("dita.map.output.dir")
    }
  }
}
