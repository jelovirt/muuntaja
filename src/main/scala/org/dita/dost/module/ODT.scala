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

class ODT(ditaDir: File) extends Preprocess(ditaDir) {

  Properties("ant.file.dita2odt") = new File("")

  def set_odt_output_tempdir() {
    logger.logInfo("\nset_odt_output_tempdir:")
    if ((!Properties.contains("odt.output.tempdir"))) {
      Properties("odt.output.tempdir") = Properties("dita.map.output.dir") + File.separator + "temp"
    }
    copy(Properties("dita.map.output.dir"), Properties("odt.output.tempdir"), "")
  }

  def clean_output_tempdir() {
    logger.logInfo("\nclean_output_tempdir:")
    if (Properties.contains("$flag")) {
      return
    }

  }

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("dita2odt.init", dita2odtInit), ("build-init", buildInit), ("preprocess", preprocess), ("set_odt_output_tempdir", set_odt_output_tempdir), ("dita.odt.package.topic", ditaOdtPackageTopic), ("dita.odt.package.map", ditaOdtPackageMap), ("move-output-file", moveOutputFile), ("clean_output_tempdir", clean_output_tempdir))
  }

  def dita2odtInit() {
    logger.logInfo("\ndita2odt.init:")
    Properties("odt.suffix") = ".odt"
    if ((!Properties.contains("args.rellinks"))) {
      Properties("args.rellinks") = "none"
    }
    if ((!Properties.contains("args.odt.include.rellinks"))) {
      Properties("args.odt.include.rellinks") = "none"
    }
    if ((!Properties.contains("odt.dir"))) {
      Properties("odt.dir") = "xsl" + File.separator + "xslodt"
    }
    if ((!Properties.contains("args.odt.img.embed"))) {
      Properties("args.odt.img.embed") = "yes"
    }
  }

  def map2odt() {
    logger.logInfo("\nmap2odt:")
    if (Properties.contains("noMap")) {
      return
    }

    ditaMapOdt(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("odt.output.tempdir") + File.separator + "content.xml")
  }

  def topic2odt() {
    logger.logInfo("\ntopic2odt:")
    if (!Properties.contains("noMap")) {
      return
    }

    ditaTopicOdt(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("odt.output.tempdir") + File.separator + "content.xml")
  }

  /**Build odt content.xml file */
  def ditaMapOdt(input: String = Properties("input"), output: String = Properties("output")) {
    logger.logInfo("\ndita.map.odt:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "dita2odt.xsl"
    }
    Properties("dita.odt.outputdir") = new File(output).getParent()
    Properties("dita.temp.dir.fullpath") = new File(Properties("dita.temp.dir") + File.separator + "dummy.file").getParent()
    import org.dita.dost.module.TopicMergeModule
    val module = new org.dita.dost.module.TopicMergeModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", Properties("dita.temp.dir.fullpath") + File.separator + Properties("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir.fullpath"))
    modulePipelineInput.setAttribute("output", Properties("dita.temp.dir.fullpath") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
    modulePipelineInput.setAttribute("style", Properties("dita.dir") + File.separator + Properties("odt.dir") + File.separator + "common" + File.separator + "topicmerge.xsl")
    module.execute(modulePipelineInput)
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("BASEDIR", Properties("basedir"))
    transformer.setParameter("TEMPDIR", Properties("dita.temp.dir"))
    if (Properties.contains("dita.ext")) {
      transformer.setParameter("DITAEXT", Properties("dita.ext"))
    }
    if (Properties.contains("args.draft")) {
      transformer.setParameter("DRAFT", Properties("args.draft"))
    }
    if (Properties.contains("dita.input.valfile")) {
      transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
    }
    transformer.setParameter("OUTPUTDIR", Properties("dita.odt.outputdir"))
    transformer.setParameter("disableRelatedLinks", Properties("args.odt.include.rellinks"))
    if (Properties.contains("args.indexshow")) {
      transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
    }
    if (Properties.contains("args.debug")) {
      transformer.setParameter("DBG", Properties("args.debug"))
    }
    if (Properties.contains("args.odt.img.embed")) {
      transformer.setParameter("ODTIMGEMBED", Properties("args.odt.img.embed"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build odt content.xml file */
  def ditaTopicOdt(input: String = Properties("input"), output: String = Properties("output")) {
    logger.logInfo("\ndita.topic.odt:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "dita2odt.xsl"
    }
    Properties("dita.odt.outputdir") = new File(output).getParent()
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()
    val templates = compileTemplates(new File(Properties("args.xsl")))
    val in_file = new File(input)
    val out_file = new File(output)
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("BASEDIR", Properties("basedir"))
    transformer.setParameter("TEMPDIR", Properties("dita.temp.dir"))
    if (Properties.contains("dita.ext")) {
      transformer.setParameter("DITAEXT", Properties("dita.ext"))
    }
    if (Properties.contains("args.draft")) {
      transformer.setParameter("DRAFT", Properties("args.draft"))
    }
    if (Properties.contains("dita.input.valfile")) {
      transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))
    }
    transformer.setParameter("OUTPUTDIR", Properties("dita.odt.outputdir"))
    transformer.setParameter("disableRelatedLinks", Properties("args.odt.include.rellinks"))
    if (Properties.contains("args.indexshow")) {
      transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))
    }
    if (Properties.contains("args.debug")) {
      transformer.setParameter("DBG", Properties("args.debug"))
    }
    if (Properties.contains("args.odt.img.embed")) {
      transformer.setParameter("ODTIMGEMBED", Properties("args.odt.img.embed"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build odt styles.xml file */
  def ditaTopicOdtStylesfile() {
    logger.logInfo("\ndita.topic.odt.stylesfile:")
    if (!Properties.contains("noMap")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
    val out_file = new File(Properties("odt.output.tempdir") + File.separator + "styles.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build odt styles.xml file */
  def ditaMapOdtStylesfile() {
    logger.logInfo("\ndita.map.odt.stylesfile:")
    if (Properties.contains("noMap")) {
      return
    }

    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
    val out_file = new File(Properties("odt.output.tempdir") + File.separator + "styles.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build odt manifest.xml file */
  def ditaOutOdtManifestFile() {
    logger.logInfo("\ndita.out.odt.manifest.file:")
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.odt.dir") + File.separator + "xsl" + File.separator + "xslodt" + File.separator + "dita2odtmanifest.xsl"))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
    val out_file = new File(Properties("odt.output.tempdir") + File.separator + "META-INF" + File.separator + "manifest.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Package into odt file */
  def ditaOdtPackageTopic() {
    logger.logInfo("\ndita.odt.package.topic:")
    History.depends(("topic2odt", topic2odt), ("dita.topic.odt.stylesfile", ditaTopicOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (!Properties.contains("noMap")) {
      return
    }

  }

  /**Package into odt file */
  def ditaOdtPackageMap() {
    logger.logInfo("\ndita.odt.package.map:")
    History.depends(("map2odt", map2odt), ("dita.map.odt.stylesfile", ditaMapOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (Properties.contains("noMap")) {
      return
    }

  }

  def moveOutputFile() {
    logger.logInfo("\nmove-output-file:")
    if (new File(Properties("odt.output.tempdir")).exists()) {
      Properties("flag") = "true"
    }
  }
}
