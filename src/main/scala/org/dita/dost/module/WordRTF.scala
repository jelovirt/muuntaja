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

class WordRTF(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2wordrtf") = new File("")

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("map2wordrtf", map2wordrtf), ("topic2wordrtf", topic2wordrtf))
  }

  def topic2wordrtf() {
    logger.logInfo("\ntopic2wordrtf:")
    if (noMap == null) {
      return
    }

    ditaTopicRtf(input = $("dita.temp.dir") + $("file.separator") + job.getProperty(INPUT_DITAMAP), output = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf")
    escapeUnicode(input = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf", output = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf.tmp")
  }

  def map2wordrtf() {
    logger.logInfo("\nmap2wordrtf:")
    if (noMap != null) {
      return
    }

    ditaMapRtf(input = $("dita.temp.dir") + $("file.separator") + job.getProperty(INPUT_DITAMAP), output = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf")
    escapeUnicode(input = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf", output = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf.tmp")
  }

  def ditaTopicRtf(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("\ndita.topic.rtf:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    $("dita.rtf.outputdir") = new File(output).getParent()
    val templates = compileTemplates(new File($("args.xsl")))
    val in_file = new File(input)
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.rtf.outputdir"))
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def ditaMapRtf(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("\ndita.map.rtf:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    $("dita.rtf.outputdir") = new File(output).getParent()
    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "topicmerge.xsl"))
      val in_file = new File(input)
      val out_file = new File($("dita.temp.dir") + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val in_file = new File($("dita.temp.dir") + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.rtf.outputdir"))
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def escapeUnicode(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("\nescapeUnicode:")
    import org.dita.dost.module.EscapeUnicodeModule
    val module = new org.dita.dost.module.EscapeUnicodeModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("input", input)
    modulePipelineInput.setAttribute("output", output)
    module.execute(modulePipelineInput)
    delete(new File(output))
  }
}
