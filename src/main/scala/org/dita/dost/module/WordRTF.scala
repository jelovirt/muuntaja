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
import org.dita.dost.util.FileUtils

class WordRTF(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2wordrtf") = new File("plugins/org.dita.wordrtf/build_dita2wordrtf.xml")
  override val transtype = "wordrtf"

  override def run() {
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    buildInit()
    preprocess()
    wordrtfImageMetadata()
    if (job.getFileInfo.exists(_.format == "ditamap")) {
      ditaMapRtf(
        input = new File(ditaTempDir, job.getInputMap),
        output = new File(outputDir, $("dita.topic.filename.root") + ".rtf"))
    } else {
      ditaTopicRtf(
        input = new File(ditaTempDir, job.getInputMap),
        output = new File(outputDir, $("dita.topic.filename.root") + ".rtf"))
    }
    escapeUnicode(
      input = new File(outputDir, $("dita.topic.filename.root") + ".rtf"),
      output = new File(outputDir, $("dita.topic.filename.root") + ".rtf.tmp"))
  }

  /** Read image metadata */
  private def wordrtfImageMetadata() {
    logger.info("wordrtf.image-metadata:")
    val module = new ImageMetadataModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    modulePipelineInput.setAttribute("outputdir", outputDir)
    module.execute(modulePipelineInput)
  }

  private def ditaTopicRtf(input: File, output: File) {
    logger.info("dita.topic.rtf:")

    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File(input)
    val outFile = new File(output)
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", output.getParent)
    val source = getSource(inFile)
    val result = getResult(outFile)
    logger.info("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  private def ditaMapRtf(input: File, output: File) {
    logger.info("dita.map.rtf:")

    try {
      val templates = compileTemplates(new File($("dita.plugin.org.dita.wordrtf.dir"), "xsl" + File.separator + "topicmerge.xsl"))
      val inFile = new File(input)
      val outFile = new File(ditaTempDir, $("dita.map.filename.root") + "_MERGED.xml")
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.info("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File(ditaTempDir, $("dita.map.filename.root") + "_MERGED.xml")
    val outFile = new File(output)
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    transformer.setParameter("OUTPUTDIR", output.getParent)
    val source = getSource(inFile)
    val result = getResult(outFile)
    logger.info("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  private def escapeUnicode(input: File, output: File) {
    logger.info("escapeUnicode:")
    val module = new EscapeUnicodeModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    modulePipelineInput.setAttribute("input", input)
    modulePipelineInput.setAttribute("output", output)
    module.execute(modulePipelineInput)
    delete(new File(output))
  }
}
