package org.dita.dost.module

import scala.collection.JavaConversions._

import scala.io.Source

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

class Dita2odt(ditaDir: File) extends DitaotPreprocess(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.odt/build_dita2odt.xml

  Properties("ant.file.dita2odt") = new File("")
  def set_odt_output_tempdir() {
    println("\nset_odt_output_tempdir:")
    if ((!Properties.contains("odt.output.tempdir"))) {
      Properties("odt.output.tempdir") = Properties("dita.map.output.dir") + "/temp"
    }
    copy(Properties("dita.map.output.dir"), Properties("odt.output.tempdir"), "")

  }
  def clean_output_tempdir() {
    println("\nclean_output_tempdir:")
    if (Properties.contains("$flag")) {
      return
    }

  }
  def dita2odt() {
    println("\ndita2odt:")
    History.depends(("dita2odt.init", dita2odtInit), ("build-init", buildInit), ("preprocess", preprocess), ("set_odt_output_tempdir", set_odt_output_tempdir), ("dita.odt.package.topic", ditaOdtPackageTopic), ("dita.odt.package.map", ditaOdtPackageMap), ("move-output-file", moveOutputFile), ("clean_output_tempdir", clean_output_tempdir))

  }
  def dita2odtInit() {
    println("\ndita2odt.init:")
    Properties("odt.suffix") = ".odt"
    if ((!Properties.contains("args.rellinks"))) {
      Properties("args.rellinks") = "none"
    }
    if ((!Properties.contains("args.odt.include.rellinks"))) {
      Properties("args.odt.include.rellinks") = "none"
    }
    if ((!Properties.contains("odt.dir"))) {
      Properties("odt.dir") = "xsl/xslodt"
    }
    if ((!Properties.contains("args.odt.img.embed"))) {
      Properties("args.odt.img.embed") = "yes"
    }

  }
  def map2odt() {
    println("\nmap2odt:")
    if (Properties.contains("noMap")) {
      return
    }
    ditaMapOdt(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("odt.output.tempdir") + File.separator + "content.xml")

  }
  def topic2odt() {
    println("\ntopic2odt:")
    if (!Properties.contains("noMap")) {
      return
    }
    ditaTopicOdt(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("odt.output.tempdir") + File.separator + "content.xml")

  }
  /**Build odt content.xml file */
  def ditaMapOdt(input: String = Properties("input"), output: String = Properties("output")) {
    println("\ndita.map.odt:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.script.dir") + File.separator + "dita2odt.xsl"
    }
    Properties("dita.odt.outputdir") = new File(output).getParent()
    Properties("dita.temp.dir.fullpath") = new File(Properties("dita.temp.dir") + File.separator + "dummy.file").getParent()
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("dita.temp.dir.fullpath") + File.separator + Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir.fullpath")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.TopicMergeModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("dita.temp.dir.fullpath") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml"
    attrs("style") = Properties("dita.dir") + "/" + Properties("odt.dir") + "/common/topicmerge.xsl"
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()

    try {
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
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  /**Build odt content.xml file */
  def ditaTopicOdt(input: String = Properties("input"), output: String = Properties("output")) {
    println("\ndita.topic.odt:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.script.dir") + File.separator + "dita2odt.xsl"
    }
    Properties("dita.odt.outputdir") = new File(output).getParent()
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()

    try {
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
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  /**Build odt styles.xml file */
  def ditaTopicOdtStylesfile() {
    println("\ndita.topic.odt.stylesfile:")
    if (!Properties.contains("noMap")) {
      return
    }

    try {
      val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("odt.output.tempdir") + File.separator + "styles.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  /**Build odt styles.xml file */
  def ditaMapOdtStylesfile() {
    println("\ndita.map.odt.stylesfile:")
    if (Properties.contains("noMap")) {
      return
    }

    try {
      val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "xslodt" + File.separator + "dita2odtstyles.xsl"))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
      val out_file = new File(Properties("odt.output.tempdir") + File.separator + "styles.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  /**Build odt manifest.xml file */
  def ditaOutOdtManifestFile() {
    println("\ndita.out.odt.manifest.file:")

    try {
      val templates = compileTemplates(new File(Properties("dita.script.dir") + File.separator + "xslodt" + File.separator + "dita2odtmanifest.xsl"))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("odt.output.tempdir") + File.separator + "META-INF" + File.separator + "manifest.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  /**Package into odt file */
  def ditaOdtPackageTopic() {
    println("\ndita.odt.package.topic:")
    History.depends(("topic2odt", topic2odt), ("dita.topic.odt.stylesfile", ditaTopicOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (!Properties.contains("noMap")) {
      return
    }

  }
  /**Package into odt file */
  def ditaOdtPackageMap() {
    println("\ndita.odt.package.map:")
    History.depends(("map2odt", map2odt), ("dita.map.odt.stylesfile", ditaMapOdtStylesfile), ("dita.out.odt.manifest.file", ditaOutOdtManifestFile))
    if (Properties.contains("noMap")) {
      return
    }

  }
  def moveOutputFile() {
    println("\nmove-output-file:")
    if (new File(Properties("odt.output.tempdir")).exists()) {
      Properties("flag") = "true"
    }

  }

}
