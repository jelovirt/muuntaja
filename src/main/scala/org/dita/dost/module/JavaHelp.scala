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

class JavaHelp(ditaDir: File) extends XHTML(ditaDir) {

  $("ant.file.dita2javahelp") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if ($.contains("noMap")) {
      return
    }

    ditaMapJavahelp()
    compileJavaHelp()
  }

  def ditaMapJavahelp() {
    logger.logInfo("\ndita.map.javahelp:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.map.javahelp.toc", ditaMapJavahelpToc), ("dita.map.javahelp.map", ditaMapJavahelpMap), ("dita.map.javahelp.set", ditaMapJavahelpSet), ("dita.map.javahelp.index", ditaMapJavahelpIndex), ("dita.out.map.javahelp.toc", ditaOutMapJavahelpToc), ("dita.out.map.javahelp.map", ditaOutMapJavahelpMap), ("dita.out.map.javahelp.set", ditaOutMapJavahelpSet), ("dita.out.map.javahelp.index", ditaOutMapJavahelpIndex))
  }

  /**Init properties for JavaHelp */
  def ditaMapJavahelpInit() {
    logger.logInfo("\ndita.map.javahelp.init:")
    $("dita.map.toc.root") = new File($("dita.input.filename")).getName()
    if (!$.contains("args.javahelp.toc")) {
      $("args.javahelp.toc") = $("dita.map.toc.root")
    }
    if (!$.contains("out.ext")) {
      $("out.ext") = ".html"
    }
    if (!$.contains("args.javahelp.map")) {
      $("args.javahelp.map") = $("dita.map.toc.root")
    }
  }

  /**Build JavaHelp TOC file */
  def ditaMapJavahelpToc() {
    logger.logInfo("\ndita.map.javahelp.toc:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!$.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelptoc.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("args.javahelp.toc") + ".xml"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp TOC file */
  def ditaOutMapJavahelpToc() {
    logger.logInfo("\ndita.out.map.javahelp.toc:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!$.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelptoc.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), $("user.input.file"), $("args.javahelp.toc") + ".xml"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp Map file */
  def ditaMapJavahelpMap() {
    logger.logInfo("\ndita.map.javahelp.map:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!$.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelpmap.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("args.javahelp.map") + ".jhm"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp Map file */
  def ditaOutMapJavahelpMap() {
    logger.logInfo("\ndita.out.map.javahelp.map:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!$.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelpmap.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), $("user.input.file"), $("args.javahelp.map") + ".jhm"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp Set file */
  def ditaMapJavahelpSet() {
    logger.logInfo("\ndita.map.javahelp.set:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.map.javahelp.map", ditaMapJavahelpMap))
    if (!$.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelpset.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("javahelpmap", $("args.javahelp.map"))
      transformer.setParameter("javahelptoc", $("args.javahelp.toc"))
      transformer.setParameter("basedir", $("basedir"))
      transformer.setParameter("outputdir", $("output.dir"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("dita.map.toc.root") + "_helpset.hs"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp Set file */
  def ditaOutMapJavahelpSet() {
    logger.logInfo("\ndita.out.map.javahelp.set:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.out.map.javahelp.map", ditaOutMapJavahelpMap))
    if (!$.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.javahelp.dir") + File.separator + "xsl" + File.separator + "map2javahelpset.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("javahelpmap", $("args.javahelp.map"))
      transformer.setParameter("javahelptoc", $("args.javahelp.toc"))
      transformer.setParameter("basedir", $("basedir"))
      transformer.setParameter("outputdir", $("output.dir"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), $("user.input.file"), $("dita.map.toc.root") + "_helpset.hs"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build JavaHelp Index file */
  def ditaMapJavahelpIndex() {
    logger.logInfo("\ndita.map.javahelp.index:")
    if (!$.contains("old.transform")) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", $("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + $("user.input.file"))
    modulePipelineInput.setAttribute("targetext", ".html")
    modulePipelineInput.setAttribute("indextype", "javahelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build JavaHelp Index file */
  def ditaOutMapJavahelpIndex() {
    logger.logInfo("\ndita.out.map.javahelp.index:")
    if (!$.contains("inner.transform")) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", $("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + $("dita.map.filename.root") + ".xml")
    modulePipelineInput.setAttribute("targetext", ".html")
    modulePipelineInput.setAttribute("indextype", "javahelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Compile Java Help output */
  def compileJavaHelp() {
    logger.logInfo("\ncompile.Java.Help:")
    if (!$.contains("env.JHHOME")) {
      return
    }

    if ($.contains("old.transform")) {
      $("compile.dir") = $("dita.map.output.dir")
    }
    if ($.contains("inner.transform")) {
      $("compile.dir") = $("output.dir")
    }
    delete(new File($("compile.dir") + File.separator + "JavaHelpSearch"), listAll(new File($("compile.dir") + File.separator + "JavaHelpSearch")))
  }

  def ditaTopicsJavahelp() {
    logger.logInfo("\ndita.topics.javahelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))
  }
}
