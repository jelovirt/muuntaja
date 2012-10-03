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
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

class Dita2wordrtf(ditaDir: File) extends DitaotPreprocess(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.wordrtf/build_dita2wordrtf.xml

  Properties("ant.file.dita2wordrtf") = new File("")
  def dita2wordrtf() {
    println("\ndita2wordrtf:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("map2wordrtf", map2wordrtf), ("topic2wordrtf", topic2wordrtf))

  }
  def topic2wordrtf() {
    println("\ntopic2wordrtf:")
    if (!Properties.contains("noMap")) {
      return
    }
    ditaTopicRtf(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf")
    escapeUnicode(input = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf", output = Properties("dita.map.output.dir") + File.separator + Properties("dita.topic.filename.root") + ".rtf.tmp")

  }
  def map2wordrtf() {
    println("\nmap2wordrtf:")
    if (Properties.contains("noMap")) {
      return
    }
    ditaMapRtf(input = Properties("dita.temp.dir") + File.separator + Properties("user.input.file"), output = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf")
    escapeUnicode(input = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf", output = Properties("dita.map.output.dir") + File.separator + Properties("dita.map.filename.root") + ".rtf.tmp")

  }
  def ditaTopicRtf(input: String = Properties("input"), output: String = Properties("output")) {
    println("\ndita.topic.rtf:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    Properties("dita.rtf.outputdir") = new File(output).getParent()

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
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
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaMapRtf(input: String = Properties("input"), output: String = Properties("output")) {
    println("\ndita.map.rtf:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.wordrtf.dir") + "/xsl/dita2rtf.xsl"
    }
    Properties("dita.rtf.outputdir") = new File(output).getParent()

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.plugin.org.dita.base.dir") + "/xsl/topicmerge.xsl")))
      val in_file = new File(input)
      val out_file = new File(Properties("dita.temp.dir") + File.separator + Properties("dita.map.filename.root") + "_MERGED.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
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
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def escapeUnicode(input: String = Properties("input"), output: String = Properties("output")) {
    println("\nescapeUnicode:")
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = ""
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.EscapeUnicodeModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("input") = input
    attrs("output") = output
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }

}
