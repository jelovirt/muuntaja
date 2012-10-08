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

class EclipseContent(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2eclipsecontent") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topics.eclipse.content", ditaTopicsEclipseContent), ("dita.map.eclipse.content", ditaMapEclipseContent))
    if ($.contains("noMap")) {
      return
    }

  }

  def ditaMapEclipseContent() {
    logger.logInfo("\ndita.map.eclipse.content:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit), ("dita.map.eclipsecontent.toc", ditaMapEclipsecontentToc), ("dita.map.eclipsecontent.index", ditaMapEclipsecontentIndex), ("dita.map.eclipsecontent.plugin", ditaMapEclipsecontentPlugin))
  }

  /**Init properties for EclipseContent */
  def ditaMapEclipsecontentInit() {
    logger.logInfo("\ndita.map.eclipsecontent.init:")
    $("dita.map.toc.root") = new File($("dita.input.filename")).getName()
    if (!$.contains("args.eclipsecontent.toc")) {
      $("args.eclipsecontent.toc") = $("dita.map.toc.root")
    }
    if ($("dita.ext") == ".dita") {
      $("content.link.ext") = ".html?srcext=dita"
    }
    if ($("dita.ext") == ".xml") {
      $("content.link.ext") = ".html?srcext=xml"
    }
  }

  /**Build EclipseContent TOC file */
  def ditaMapEclipsecontentToc() {
    logger.logInfo("\ndita.map.eclipsecontent.toc:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("user.input.file.listlist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("OUTEXT", $("content.link.ext"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("args.eclipsecontent.toc") + ".xml"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build Eclipse Help index file */
  def ditaMapEclipsecontentIndex() {
    logger.logInfo("\ndita.map.eclipsecontent.index:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    if ($.contains("noMap")) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", $("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + $("user.input.file"))
    modulePipelineInput.setAttribute("targetext", $("content.link.ext"))
    modulePipelineInput.setAttribute("indextype", "eclipsehelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build EclipseContent plugin file */
  def ditaMapEclipsecontentPlugin() {
    logger.logInfo("\ndita.map.eclipsecontent.plugin:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsecontent.dir") + File.separator + "xsl" + File.separator + "map2plugin-cp.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + $("user.input.file"))
    val out_file = new File($("dita.map.output.dir") + File.separator + "plugin.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("TOCROOT", $("args.eclipsecontent.toc"))
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def ditaTopicsEclipseContent() {
    logger.logInfo("\ndita.topics.eclipse.content:")
    if ($.contains("noTopic")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsecontent.dir") + File.separator + "xsl" + File.separator + "dita2dynamicdita.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = $("dita.ext")
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("OUTEXT", $("dita.ext"))
      }
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
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
