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

class Dita2htmlhelp(ditaDir: File) extends Dita2xhtml(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.htmlhelp/build_dita2htmlhelp.xml

  Properties("ant.file.dita2htmlhelp") = new File("")
  def dita2htmlhelp() {
    println("\ndita2htmlhelp:")
    History.depends(("build-init", buildInit), ("use-init.envhhcdir", useInitEnvhhcdir), ("use-init.hhcdir", useInitHhcdir), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if (Properties.contains("noMap")) {
      return
    }
    ditaMapHtmlhelp()
    ditaHtmlhelpConvertlang()
    compileHTMLHelp()

  }
  def useInitEnvhhcdir() {
    println("\nuse-init.envhhcdir:")
    if (!Properties.contains("env.HHCDIR")) {
      return
    }
    if (new File(Properties("env.HHCDIR") + File.separator + "hhc.exe").exists()) {
      Properties("HTMLHelpCompiler") = Properties("env.HHCDIR") + File.separator + "hhc.exe"
    }

  }
  def useInitHhcdir() {
    println("\nuse-init.hhcdir:")
    if (Properties.contains("env.HHCDIR")) {
      return
    }
    if (new File("C:\\Program Files (x86)\\HTML Help Workshop").exists()) {
      Properties("hhc.dir") = "C:\\Program Files (x86)\\HTML Help Workshop"
    } else {
      Properties("hhc.dir") = "C:\\Program Files\\HTML Help Workshop"
    }
    if (new File(Properties("hhc.dir") + File.separator + "hhc.exe").exists()) {
      Properties("HTMLHelpCompiler") = Properties("hhc.dir") + File.separator + "hhc.exe"
    }

  }
  def ditaMapHtmlhelp() {
    println("\ndita.map.htmlhelp:")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit), ("dita.map.htmlhelp.hhp", ditaMapHtmlhelpHhp), ("dita.map.htmlhelp.hhc", ditaMapHtmlhelpHhc), ("dita.map.htmlhelp.hhk", ditaMapHtmlhelpHhk), ("dita.out.map.htmlhelp.hhp", ditaOutMapHtmlhelpHhp), ("dita.out.map.htmlhelp.hhc", ditaOutMapHtmlhelpHhc), ("dita.out.map.htmlhelp.hhk", ditaOutMapHtmlhelpHhk))

  }
  def ditaMapHtmlhelpInit() {
    println("\ndita.map.htmlhelp.init:")
    println("Init properties for HTMLHelp")
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }

  }
  def ditaMapHtmlhelpHhp() {
    println("\ndita.map.htmlhelp.hhp:")
    println("Build HTMLHelp HHP file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2hhp.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = ".hhp"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("HHCNAME", Properties("dita.map.filename.root") + ".hhc")
        if (Properties.contains("args.htmlhelp.includefile")) {
          transformer.setParameter("INCLUDEFILE", Properties("args.htmlhelp.includefile"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = new StreamSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  def ditaOutMapHtmlhelpHhp() {
    println("\ndita.out.map.htmlhelp.hhp:")
    println("Build HTMLHelp HHP file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2hhp.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("HHCNAME", Properties("dita.map.filename.root") + ".hhc")
        if (Properties.contains("args.htmlhelp.includefile")) {
          transformer.setParameter("INCLUDEFILE", Properties("args.htmlhelp.includefile"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.filename.root") + ".hhp"))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = new StreamSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  def ditaMapHtmlhelpHhc() {
    println("\ndita.map.htmlhelp.hhc:")
    println("Build HTMLHelp HHC file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2hhc.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = ".hhc"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = new StreamSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  def ditaOutMapHtmlhelpHhc() {
    println("\ndita.out.map.htmlhelp.hhc:")
    println("Build HTMLHelp HHC file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2hhc.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file.listfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.filename.root") + ".hhc"))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = new StreamSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }

    }

  }
  def ditaMapHtmlhelpHhk() {
    println("\ndita.map.htmlhelp.hhk:")
    println("Build HTMLHelp HHK file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "htmlhelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def ditaOutMapHtmlhelpHhk() {
    println("\ndita.out.map.htmlhelp.hhk:")
    println("Build HTMLHelp HHK file")
    History.depends(("dita.map.htmlhelp.init", ditaMapHtmlhelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("dita.map.filename.root") + ".hhk"
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "htmlhelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def ditaHtmlhelpConvertlang() {
    println("\ndita.htmlhelp.convertlang:")

  }
  def compileHTMLHelp() {
    println("\ncompile.HTML.Help:")
    println("Compile HTMLHelp output")
    if (!Properties.contains("HTMLHelpCompiler")) {
      return
    }
    if (Properties.contains("inner.transform")) {
      Properties("compile.dir") = Properties("output.dir")
    }
    if (Properties.contains("old.transform")) {
      Properties("compile.dir") = Properties("dita.map.output.dir")
    }

  }
  def ditaTopicsHtmlhelp() {
    println("\ndita.topics.htmlhelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))

  }

}
