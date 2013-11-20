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

class WordRTF(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2wordrtf") = new File("")
  override val transtype = "wordrtf"

  override def run() {
    logger.logInfo("run:")
    depends(("build-init", buildInit), ("preprocess", preprocess), ("wordrtf.image-metadata", wordrtfImageMetadata), ("map2wordrtf", map2wordrtf), ("topic2wordrtf", topic2wordrtf))
  }

  /**Read image metadata */
  def wordrtfImageMetadata() {
    logger.logInfo("wordrtf.image-metadata:")
    import org.dita.dost.module.ImageMetadataModule
    val module = new org.dita.dost.module.ImageMetadataModule
    module.setLogger(new DITAOTJavaLogger())
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("outputdir", $("output.dir"))
    module.execute(modulePipelineInput)
  }

  def topic2wordrtf() {
    logger.logInfo("topic2wordrtf:")
    if (job.getFileInfo().find(_.format == "ditamap").isDefined) {
      return
    }

    ditaTopicRtf(input = $("dita.temp.dir") + $("file.separator") + job.getInputMap(), output = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf")
    escapeUnicode(input = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf", output = $("dita.map.output.dir") + $("file.separator") + $("dita.topic.filename.root") + ".rtf.tmp")
  }

  def map2wordrtf() {
    logger.logInfo("map2wordrtf:")
    if (job.getFileInfo().find(_.format == "ditamap").isEmpty) {
      return
    }

    ditaMapRtf(input = $("dita.temp.dir") + $("file.separator") + job.getInputMap(), output = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf")
    escapeUnicode(input = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf", output = $("dita.map.output.dir") + $("file.separator") + $("dita.map.filename.root") + ".rtf.tmp")
  }

  def ditaTopicRtf(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("dita.topic.rtf:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    $("dita.rtf.outputdir") = new File(output).getParent()
    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File(input)
    val outFile = new File(output)
    if (!outFile.getParentFile().exists()) {
      outFile.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.rtf.outputdir"))
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  def ditaMapRtf(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("dita.map.rtf:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    $("dita.rtf.outputdir") = new File(output).getParent()
    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.wordrtf.dir") + File.separator + "xsl" + File.separator + "topicmerge.xsl"))
      val inFile = new File(input)
      val outFile = new File($("dita.temp.dir") + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File($("dita.temp.dir") + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
    val outFile = new File(output)
    if (!outFile.getParentFile().exists()) {
      outFile.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.rtf.outputdir"))
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  def escapeUnicode(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("escapeUnicode:")
    import org.dita.dost.module.EscapeUnicodeModule
    val module = new org.dita.dost.module.EscapeUnicodeModule
    module.setLogger(new DITAOTJavaLogger())
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("input", input)
    modulePipelineInput.setAttribute("output", output)
    module.execute(modulePipelineInput)
    delete(new File(output))
  }
}
