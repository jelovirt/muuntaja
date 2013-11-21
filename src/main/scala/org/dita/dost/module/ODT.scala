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

class ODT(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2odt") = new File("plugins/org.dita.odt/build_dita2odt.xml")
  override val transtype = "odt"

  def set_odt_output_tempdir() {
    logger.logInfo("set_odt_output_tempdir:")
    if (!$.contains("odt.output.tempdir")) {
      $("odt.output.tempdir") = $("dita.map.output.dir") + "/" + $("uplevels") + "temp"
    }
    copy(new File($("dita.map.output.dir") + File.separator + $("uplevels")), new File($("odt.output.tempdir")), listAll(new File($("dita.map.output.dir") + File.separator + $("uplevels"))) -- Set("**/*.list") -- Set("**/*.log") -- Set("**/*.temp") -- Set("**/*.properties") -- Set("**/*.odt") -- Set("temp/**"))
  }

  def clean_output_tempdir() {
    logger.logInfo("clean_output_tempdir:")
    if ($.contains("$flag")) {
      return
    }

    delete(new File($("odt.output.tempdir")), listAll(new File($("odt.output.tempdir"))))
  }

  override def run() {
    logger.logInfo("run:")
    depends(("dita2odt.init", dita2odtInit), ("build-init", buildInit), ("preprocess", preprocess), ("odt.image-metadata", odtImageMetadata), ("set_odt_output_tempdir", set_odt_output_tempdir), ("dita.odt.package.topic", ditaOdtPackageTopic), ("dita.odt.package.map", ditaOdtPackageMap), ("move-output-file", moveOutputFile), ("clean_output_tempdir", clean_output_tempdir))
  }

  def dita2odtInit() {
    logger.logInfo("dita2odt.init:")
    $("odt.suffix") = ".odt"
    if (!$.contains("args.rellinks")) {
      $("args.rellinks") = "none"
    }
    if (!$.contains("args.odt.img.embed")) {
      $("args.odt.img.embed") = "yes"
    }
  }

  /**Read image metadata */
  def odtImageMetadata() {
    logger.logInfo("odt.image-metadata:")
    import org.dita.dost.module.ImageMetadataModule
    val module = new org.dita.dost.module.ImageMetadataModule
    module.setLogger(new DITAOTJavaLogger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    modulePipelineInput.setAttribute("outputdir", outputDir)
    module.execute(modulePipelineInput)
  }

  def map2odt() {
    logger.logInfo("map2odt:")
    if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
      return
    }

    ditaMapOdt(input = ditaTempDir + $("file.separator") + job.getInputMap(), output = $("odt.output.tempdir") + $("file.separator") + "content.xml")
  }

  def topic2odt() {
    logger.logInfo("topic2odt:")
    if (job.getFileInfo.find(_.format == "ditamap").isDefined) {
      return
    }

    ditaTopicOdt(input = ditaTempDir + $("file.separator") + job.getInputMap(), output = $("odt.output.tempdir") + $("file.separator") + "content.xml")
  }

  /**Build odt content.xml file */
  def ditaMapOdt(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("dita.map.odt:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.odt.dir") + "/xsl/dita2odt.xsl"
    }
    $("dita.odt.outputdir") = new File(output).getParent
    $("dita.temp.dir.fullpath") = new File(ditaTempDir + File.separator + "dummy.file").getParent
    import org.dita.dost.module.TopicMergeModule
    val module = new org.dita.dost.module.TopicMergeModule
    module.setLogger(new DITAOTJavaLogger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("inputmap", $("dita.temp.dir.fullpath") + $("file.separator") + job.getInputMap())
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir.fullpath"))
    modulePipelineInput.setAttribute("output", $("dita.temp.dir.fullpath") + $("file.separator") + $("dita.map.filename.root") + "_MERGED.xml")
    modulePipelineInput.setAttribute("style", $("dita.plugin.org.dita.odt.dir") + "/xsl/xslodt/common/topicmerge.xsl")
    module.execute(modulePipelineInput)
    $("dita.input.valfile.url") = new File($("dita.input.valfile")).toURI.toASCIIString
    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File(ditaTempDir + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
    val outFile = new File(output)
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("BASEDIR", $("basedir"))
    transformer.setParameter("TEMPDIR", ditaTempDir)
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    if ($.contains("dita.input.valfile")) {
      transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.odt.outputdir"))
    transformer.setParameter("include.rellinks", $("include.rellinks"))
    if ($.contains("args.indexshow")) {
      transformer.setParameter("INDEXSHOW", $("args.indexshow"))
    }
    if ($.contains("args.debug")) {
      transformer.setParameter("DBG", $("args.debug"))
    }
    if ($.contains("args.odt.img.embed")) {
      transformer.setParameter("ODTIMGEMBED", $("args.odt.img.embed"))
    }
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Build odt content.xml file */
  def ditaTopicOdt(input: String = $("input"), output: String = $("output")) {
    logger.logInfo("dita.topic.odt:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.odt.dir") + "/xsl/dita2odt.xsl"
    }
    $("dita.odt.outputdir") = new File(output).getParent
    $("dita.input.valfile.url") = new File($("dita.input.valfile")).toURI.toASCIIString
    val templates = compileTemplates(new File($("args.xsl")))
    val inFile = new File(input)
    val outFile = new File(output)
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("BASEDIR", $("basedir"))
    transformer.setParameter("TEMPDIR", ditaTempDir)
    if ($.contains("args.draft")) {
      transformer.setParameter("DRAFT", $("args.draft"))
    }
    if ($.contains("dita.input.valfile")) {
      transformer.setParameter("FILTERFILE", $("dita.input.valfile.url"))
    }
    transformer.setParameter("OUTPUTDIR", $("dita.odt.outputdir"))
    transformer.setParameter("include.rellinks", $("include.rellinks"))
    if ($.contains("args.indexshow")) {
      transformer.setParameter("INDEXSHOW", $("args.indexshow"))
    }
    if ($.contains("args.debug")) {
      transformer.setParameter("DBG", $("args.debug"))
    }
    if ($.contains("args.odt.img.embed")) {
      transformer.setParameter("ODTIMGEMBED", $("args.odt.img.embed"))
    }
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Build odt styles.xml file */
  def ditaTopicOdtStylesfile() {
    logger.logInfo("dita.topic.odt.stylesfile:")
    if (job.getFileInfo.find(_.format == "ditamap").isDefined) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
    val inFile = new File(ditaTempDir + File.separator + job.getInputMap())
    val outFile = new File($("odt.output.tempdir") + File.separator + "styles.xml")
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Build odt styles.xml file */
  def ditaMapOdtStylesfile() {
    logger.logInfo("dita.map.odt.stylesfile:")
    if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
    val inFile = new File(ditaTempDir + File.separator + $("dita.map.filename.root") + "_MERGED.xml")
    val outFile = new File($("odt.output.tempdir") + File.separator + "styles.xml")
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Build odt manifest.xml file */
  def ditaOutOdtManifestFile() {
    logger.logInfo("dita.out.odt.manifest.file:")
    val templates = compileTemplates(new File($("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtmanifest.xsl"))
    val inFile = new File(ditaTempDir + File.separator + job.getInputMap())
    val outFile = new File($("odt.output.tempdir") + File.separator + "META-INF" + File.separator + "manifest.xml")
    if (!outFile.getParentFile.exists) {
      outFile.getParentFile.mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Package into odt file */
  def ditaOdtPackageTopic() {
    logger.logInfo("dita.odt.package.topic:")
    depends(("topic2odt", topic2odt), ("dita.topic.odt.stylesfile", ditaTopicOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (job.getFileInfo.find(_.format == "ditamap").isDefined) {
      return
    }

    zip(new File($("odt.output.tempdir") + File.separator + $("dita.topic.filename.root") + $("odt.suffix")), new File($("odt.output.tempdir")), listAll(new File($("odt.output.tempdir"))) -- Set("**/*.list, **/*.log, **/*.temp, **/*.properties, **/*.odt"))
  }

  /**Package into odt file */
  def ditaOdtPackageMap() {
    logger.logInfo("dita.odt.package.map:")
    depends(("map2odt", map2odt), ("dita.map.odt.stylesfile", ditaMapOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
      return
    }

    zip(new File($("odt.output.tempdir") + File.separator + $("dita.map.filename.root") + $("odt.suffix")), new File($("odt.output.tempdir")), listAll(new File($("odt.output.tempdir"))) -- Set("**/*.list, **/*.log, **/*.temp, **/*.properties, **/*.odt"))
  }

  def moveOutputFile() {
    logger.logInfo("move-output-file:")
    move(new File($("odt.output.tempdir")), new File($("dita.map.output.dir") + File.separator + $("uplevels")), Set("**/*.list") ++ Set("**/*.log") ++ Set("**/*.temp") ++ Set("**/*.properties") ++ Set("**/*.odt"))
    if (new File($("odt.output.tempdir")).exists && new File($("odt.output.tempdir")).isDirectory) {
      $("flag") = "true"
    }
  }
}
