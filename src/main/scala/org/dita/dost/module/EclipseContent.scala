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

class EclipseContent(ditaDir: File) extends Preprocess(ditaDir) {

  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.eclipsecontent/build_dita2eclipsecontent.xml

  Properties("ant.file.dita2eclipsecontent") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.topics.eclipse.content", ditaTopicsEclipseContent), ("dita.map.eclipse.content", ditaMapEclipseContent))
    if (Properties.contains("noMap")) {
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
    Properties("dita.map.toc.root") = new File(Properties("dita.input.filename")).getName()
    if ((!Properties.contains("args.eclipsecontent.toc"))) {
      Properties("args.eclipsecontent.toc") = Properties("dita.map.toc.root")
    }
    if (Properties("dita.ext") == ".dita") {
      Properties("content.link.ext") = ".html?srcext=dita"
    }
    if (Properties("dita.ext") == ".xml") {
      Properties("content.link.ext") = ".html?srcext=xml"
    }
  }

  /**Build EclipseContent TOC file */
  def ditaMapEclipsecontentToc() {
    logger.logInfo("\ndita.map.eclipsecontent.toc:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.eclipsehelp.dir") + "/xsl/map2eclipse.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      transformer.setParameter("OUTEXT", Properties("content.link.ext"))
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.eclipsecontent.toc") + ".xml"))
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
    if (Properties.contains("noMap")) {
      return
    }

    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = Properties("content.link.ext")
    attrs("indextype") = "eclipsehelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")
    }
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }

  /**Build EclipseContent plugin file */
  def ditaMapEclipsecontentPlugin() {
    logger.logInfo("\ndita.map.eclipsecontent.plugin:")
    History.depends(("dita.map.eclipsecontent.init", ditaMapEclipsecontentInit))
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.eclipsecontent.dir") + "/xsl/map2plugin-cp.xsl"))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
    val out_file = new File(Properties("dita.map.output.dir") + File.separator + "plugin.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("TOCROOT", Properties("args.eclipsecontent.toc"))
    if (Properties.contains("args.eclipse.version")) {
      transformer.setParameter("version", Properties("args.eclipse.version"))
    }
    if (Properties.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", Properties("args.eclipse.provider"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def ditaTopicsEclipseContent() {
    logger.logInfo("\ndita.topics.eclipse.content:")
    if (Properties.contains("noTopic")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.eclipsecontent.dir") + "/xsl/dita2dynamicdita.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("output.dir"))
    val temp_ext = Properties("dita.ext")
    val files = readList(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")))
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("OUTEXT", Properties("dita.ext"))
      }
      if (Properties.contains("args.draft")) {
        transformer.setParameter("DRAFT", Properties("args.draft"))
      }
      if (Properties.contains("args.debug")) {
        transformer.setParameter("DBG", Properties("args.debug"))
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
