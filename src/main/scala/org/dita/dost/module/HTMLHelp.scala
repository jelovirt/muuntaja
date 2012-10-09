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

class HTMLHelp(ditaDir: File) extends XHTML(ditaDir) {

  $("ant.file.dita2htmlhelp") = new File("")

  override def run() {
    logger.logInfo("run:")
    History.depends(("build-init", buildInit), ("use-init.envhhcdir", useInitEnvhhcdir), ("use-init.hhcdir", useInitHhcdir), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if (noMap) {
      return
    }

    ditaMapHtmlhelp()
    ditaHtmlhelpConvertlang()
    compileHTMLHelp()
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
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit), ("dita.map.htmlhelp.hhp", ditaMapHtmlhelpHhp), ("dita.map.htmlhelp.hhc", ditaMapHtmlhelpHhc), ("dita.map.htmlhelp.hhk", ditaMapHtmlhelpHhk), ("dita.out.map.htmlhelp.hhp", ditaOutMapHtmlhelpHhp), ("dita.out.map.htmlhelp.hhc", ditaOutMapHtmlhelpHhc), ("dita.out.map.htmlhelp.hhk", ditaOutMapHtmlhelpHhk))
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
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhp.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = ".hhp"
    val files = Set(job.getProperty(INPUT_DITAMAP)) -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      transformer.setParameter("HHCNAME", $("dita.map.filename.root") + ".hhc")
      if ($.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", $("args.htmlhelp.includefile"))
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

  /**Build HTMLHelp HHP file */
  def ditaOutMapHtmlhelpHhp() {
    logger.logInfo("dita.out.map.htmlhelp.hhp:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhp.xsl"))
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
      transformer.setParameter("HHCNAME", $("dita.map.filename.root") + ".hhc")
      if ($.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", $("args.htmlhelp.includefile"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), job.getProperty(INPUT_DITAMAP), $("dita.map.filename.root") + ".hhp"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHC file */
  def ditaMapHtmlhelpHhc() {
    logger.logInfo("dita.map.htmlhelp.hhc:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhc.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = ".hhc"
    val files = Set(job.getProperty(INPUT_DITAMAP)) -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
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

  /**Build HTMLHelp HHC file */
  def ditaOutMapHtmlhelpHhc() {
    logger.logInfo("dita.out.map.htmlhelp.hhc:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.htmlhelp.dir") + File.separator + "xsl" + File.separator + "map2hhc.xsl"))
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
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), job.getProperty(INPUT_DITAMAP), $("dita.map.filename.root") + ".hhc"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build HTMLHelp HHK file */
  def ditaMapHtmlhelpHhk() {
    logger.logInfo("dita.map.htmlhelp.hhk:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!oldTransform) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + job.getProperty(INPUT_DITAMAP))
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
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!innerTransform) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
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

  def ditaTopicsHtmlhelp() {
    logger.logInfo("dita.topics.htmlhelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))
  }
}
