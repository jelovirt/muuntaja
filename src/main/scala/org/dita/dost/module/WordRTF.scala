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

class WordRTF(ditaDir: File) extends Preprocess(ditaDir) {

  Properties("ant.file.dita2wordrtf") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("map2wordrtf", map2wordrtf), ("topic2wordrtf", topic2wordrtf))
  }

  def topic2wordrtf() {
    logger.logInfo("\ntopic2wordrtf:")
    if (!Properties.contains("noMap")) {
      return
    }

    ditaTopicRtf(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf")
    escapeUnicode(input = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf", output = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf.tmp")
  }

  def map2wordrtf() {
    logger.logInfo("\nmap2wordrtf:")
    if (Properties.contains("noMap")) {
      return
    }

    ditaMapRtf(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf")
    escapeUnicode(input = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf", output = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf.tmp")
  }

  def ditaTopicRtf(input: String = Properties("input"), output: String = Properties("output")) {
    logger.logInfo("\ndita.topic.rtf:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    Properties("dita.rtf.outputdir") = new File(output).getParent()
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val in_file = new File(input)
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if (Properties.contains("args.draft")) {
      transformer.setParameter("DRAFT", Properties("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", Properties("dita.rtf.outputdir"))
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def ditaMapRtf(input: String = Properties("input"), output: String = Properties("output")) {
    logger.logInfo("\ndita.map.rtf:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    Properties("dita.rtf.outputdir") = new File(output).getParent()
    try {
      val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + "/xsl/topicmerge.xsl"))
      val in_file = new File(input)
      val out_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if (Properties.contains("args.draft")) {
      transformer.setParameter("DRAFT", Properties("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", Properties("dita.rtf.outputdir"))
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def escapeUnicode(input: String = Properties("input"), output: String = Properties("output")) {
    logger.logInfo("\nescapeUnicode:")
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = ""
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.EscapeUnicodeModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("input") = input
    attrs("output") = output
    val modulePipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      modulePipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(modulePipelineInput)
  }
}
