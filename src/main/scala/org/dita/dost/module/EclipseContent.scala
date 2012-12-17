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

class EclipseContent(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2eclipsecontent") = new File("")
  override val transtype = "eclipsecontent"

  override def run() {
    logger.logInfo("run:")
    depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topics.eclipse.content", ditaTopicsEclipseContent), ("dita.map.eclipse.content", ditaMapEclipseContent))
    if (noMap) {
      return
    }

  }

  def ditaMapEclipseContent() {
    logger.logInfo("dita.map.eclipse.content:")
    depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit), ("dita.map.eclipsecontent.toc", ditaMapEclipsecontentToc), ("dita.map.eclipsecontent.index", ditaMapEclipsecontentIndex), ("dita.map.eclipsecontent.plugin", ditaMapEclipsecontentPlugin))
  }

  /**Init properties for EclipseContent */
  def ditaMapEclipsecontentInit() {
    logger.logInfo("dita.map.eclipsecontent.init:")
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
    logger.logInfo("dita.map.eclipsecontent.toc:")
    depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val files = Set(job.getProperty(INPUT_DITAMAP)) -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("OUTEXT", $("content.link.ext"))
      val inFile = new File(baseDir, l)
      val outFile = new File(globMap(new File(destDir, l).getAbsolutePath(), "*" + $("dita.input.filename"), "*" + $("args.eclipsecontent.toc") + ".xml"))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /**Build Eclipse Help index file */
  def ditaMapEclipsecontentIndex() {
    logger.logInfo("dita.map.eclipsecontent.index:")
    depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    if (noMap) {
      return
    }

    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("targetext", $("content.link.ext"))
    modulePipelineInput.setAttribute("indextype", "eclipsehelp")
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build EclipseContent plugin file */
  def ditaMapEclipsecontentPlugin() {
    logger.logInfo("dita.map.eclipsecontent.plugin:")
    depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsecontent.dir") + File.separator + "xsl" + File.separator + "map2plugin-cp.xsl"))
    val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val outFile = new File($("dita.map.output.dir") + File.separator + "plugin.xml")
    if (!outFile.getParentFile().exists()) {
      outFile.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("TOCROOT", $("args.eclipsecontent.toc"))
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  def ditaTopicsEclipseContent() {
    logger.logInfo("dita.topics.eclipse.content:")
    if (noTopic) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsecontent.dir") + File.separator + "xsl" + File.separator + "dita2dynamicdita.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("output.dir"))
    val tempExt = $("dita.ext")
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
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
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("FILENAME", inFile.getName())
      transformer.setParameter("FILEDIR", inFile.getParent())
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }
}
