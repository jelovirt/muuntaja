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

class HTMLHelp(ditaDir: File) extends XHTML(ditaDir) {

  Properties("ant.file.dita2htmlhelp") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("use-init.envhhcdir", useInitEnvhhcdir), ("use-init.hhcdir", useInitHhcdir), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if (Properties.contains("noMap")) {
      return
    }

    ditaMapHtmlhelp()
    ditaHtmlhelpConvertlang()
    compileHTMLHelp()
  }

  def useInitEnvhhcdir() {
    logger.logInfo("\nuse-init.envhhcdir:")
    if (!Properties.contains("env.HHCDIR")) {
      return
    }

    if (new File(Properties("env.HHCDIR") + File.separator + "hhc.exe").exists()) {
      Properties("HTMLHelpCompiler") = Properties("env.HHCDIR") + File.separator + "hhc.exe"
    }
  }

  def useInitHhcdir() {
    logger.logInfo("\nuse-init.hhcdir:")
    if (Properties.contains("env.HHCDIR")) {
      return
    }

    if (new File("C:\\Program Files (x86)\\HTML Help Workshop").exists()) {
      Properties("hhc.dir") = "C:\\Program Files (x86)\\HTML Help Workshop"
    } else {
      Properties("hhc.dir") = "C:\\Program Files\\HTML Help Workshop"
    }
    if (new File(Properties("hhc.dir") + File.separator + "hhc.exe").exists()) {
      Properties("HTMLHelpCompiler") = Properties("hhc.dir") + File.separator + "hhc.exe"
    }
  }

  def ditaMapHtmlhelp() {
    logger.logInfo("\ndita.map.htmlhelp:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit), ("dita.map.htmlhelp.hhp", ditaMapHtmlhelpHhp), ("dita.map.htmlhelp.hhc", ditaMapHtmlhelpHhc), ("dita.map.htmlhelp.hhk", ditaMapHtmlhelpHhk), ("dita.out.map.htmlhelp.hhp", ditaOutMapHtmlhelpHhp), ("dita.out.map.htmlhelp.hhc", ditaOutMapHtmlhelpHhc), ("dita.out.map.htmlhelp.hhk", ditaOutMapHtmlhelpHhk))
  }

  /**Init properties for HTMLHelp */
  def ditaMapHtmlhelpInit() {
    logger.logInfo("\ndita.map.htmlhelp.init:")
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
  }

  /**Build HTMLHelp HHP file */
  def ditaMapHtmlhelpHhp() {
    logger.logInfo("\ndita.map.htmlhelp.hhp:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2hhp.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = ".hhp"
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("HHCNAME", Properties("dita.map.filename.root") + ".hhc")
      if (Properties.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", Properties("args.htmlhelp.includefile"))
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
    logger.logInfo("\ndita.out.map.htmlhelp.hhp:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2hhp.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      transformer.setParameter("HHCNAME", Properties("dita.map.filename.root") + ".hhc")
      if (Properties.contains("args.htmlhelp.includefile")) {
        transformer.setParameter("INCLUDEFILE", Properties("args.htmlhelp.includefile"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.filename.root") + ".hhp"))
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
    logger.logInfo("\ndita.map.htmlhelp.hhc:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2hhc.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = ".hhc"
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
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
    logger.logInfo("\ndita.out.map.htmlhelp.hhc:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2hhc.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("out.ext")) {
        transformer.setParameter("OUTEXT", Properties("out.ext"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.filename.root") + ".hhc"))
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
    logger.logInfo("\ndita.map.htmlhelp.hhk:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "htmlhelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")
    }
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }

  /**Build HTMLHelp HHK file */
  def ditaOutMapHtmlhelpHhk() {
    logger.logInfo("\ndita.out.map.htmlhelp.hhk:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("dita.map.filename.root") + ".hhk"
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "htmlhelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")
    }
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }

  def ditaHtmlhelpConvertlang() {
    logger.logInfo("\ndita.htmlhelp.convertlang:")
  }

  /**Compile HTMLHelp output */
  def compileHTMLHelp() {
    logger.logInfo("\ncompile.HTML.Help:")
    if (!Properties.contains("HTMLHelpCompiler")) {
      return
    }

    if (Properties.contains("inner.transform")) {
      Properties("compile.dir") = Properties("output.dir")
    }
    if (Properties.contains("old.transform")) {
      Properties("compile.dir") = Properties("dita.map.output.dir")
    }
  }

  def ditaTopicsHtmlhelp() {
    logger.logInfo("\ndita.topics.htmlhelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))
  }
}
