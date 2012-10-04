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

class JavaHelp(ditaDir: File) extends XHTML(ditaDir) {

  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.javahelp/build_dita2javahelp.xml

  Properties("ant.file.dita2javahelp") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if (Properties.contains("noMap")) {
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
    Properties("dita.map.toc.root") = new File(Properties("dita.input.filename")).getName()
    if ((!Properties.contains("args.javahelp.toc"))) {
      Properties("args.javahelp.toc") = Properties("dita.map.toc.root")
    }
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
    if ((!Properties.contains("args.javahelp.map"))) {
      Properties("args.javahelp.map") = Properties("dita.map.toc.root")
    }
  }

  /**Build JavaHelp TOC file */
  def ditaMapJavahelpToc() {
    logger.logInfo("\ndita.map.javahelp.toc:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelptoc.xsl"))
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
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.javahelp.toc") + ".xml"))
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
    if (!Properties.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelptoc.xsl"))
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
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.javahelp.toc") + ".xml"))
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
    if (!Properties.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelpmap.xsl"))
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
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.javahelp.map") + ".jhm"))
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
    if (!Properties.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelpmap.xsl"))
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
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.javahelp.map") + ".jhm"))
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
    if (!Properties.contains("old.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelpset.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("javahelpmap", Properties("args.javahelp.map"))
      transformer.setParameter("javahelptoc", Properties("args.javahelp.toc"))
      transformer.setParameter("basedir", Properties("basedir"))
      transformer.setParameter("outputdir", Properties("output.dir"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("dita.map.toc.root") + "_helpset.hs"))
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
    if (!Properties.contains("inner.transform")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "map2javahelpset.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      transformer.setParameter("javahelpmap", Properties("args.javahelp.map"))
      transformer.setParameter("javahelptoc", Properties("args.javahelp.toc"))
      transformer.setParameter("basedir", Properties("basedir"))
      transformer.setParameter("outputdir", Properties("output.dir"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.toc.root") + "_helpset.hs"))
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
    if (!Properties.contains("old.transform")) {
      return
    }

    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = ".html"
    attrs("indextype") = "javahelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")
    }
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }

  /**Build JavaHelp Index file */
  def ditaOutMapJavahelpIndex() {
    logger.logInfo("\ndita.out.map.javahelp.index:")
    if (!Properties.contains("inner.transform")) {
      return
    }

    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("dita.map.filename.root") + ".xml"
    attrs("targetext") = ".html"
    attrs("indextype") = "javahelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")
    }
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }

  /**Compile Java Help output */
  def compileJavaHelp() {
    logger.logInfo("\ncompile.Java.Help:")
    if (!Properties.contains("env.JHHOME")) {
      return
    }

    if (Properties.contains("old.transform")) {
      Properties("compile.dir") = Properties("dita.map.output.dir")
    }
    if (Properties.contains("inner.transform")) {
      Properties("compile.dir") = Properties("output.dir")
    }
  }

  def ditaTopicsJavahelp() {
    logger.logInfo("\ndita.topics.javahelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))
  }
}
