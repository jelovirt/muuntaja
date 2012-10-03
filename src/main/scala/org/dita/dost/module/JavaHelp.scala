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

class Dita2javahelp(ditaDir: File) extends Dita2xhtml(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.javahelp/build_dita2javahelp.xml

  Properties("ant.file.dita2javahelp") = new File("")
  def dita2javahelp() {
    println("\ndita2javahelp:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.html", ditaTopicsHtml), ("dita.inner.topics.html", ditaInnerTopicsHtml), ("dita.outer.topics.html", ditaOuterTopicsHtml))
    if (Properties.contains("noMap")) {
      return
    }
    ditaMapJavahelp()
    compileJavaHelp()

  }
  def ditaMapJavahelp() {
    println("\ndita.map.javahelp:")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.map.javahelp.toc", ditaMapJavahelpToc), ("dita.map.javahelp.map", ditaMapJavahelpMap), ("dita.map.javahelp.set", ditaMapJavahelpSet), ("dita.map.javahelp.index", ditaMapJavahelpIndex), ("dita.out.map.javahelp.toc", ditaOutMapJavahelpToc), ("dita.out.map.javahelp.map", ditaOutMapJavahelpMap), ("dita.out.map.javahelp.set", ditaOutMapJavahelpSet), ("dita.out.map.javahelp.index", ditaOutMapJavahelpIndex))

  }
  def ditaMapJavahelpInit() {
    println("\ndita.map.javahelp.init:")
    println("Init properties for JavaHelp")
    Properties("dita.map.toc.root") = new File(Properties("dita.input.filename")).getName()
    if ((!Properties.contains("args.javahelp.toc"))) {
      Properties("args.javahelp.toc") = Properties("dita.map.toc.root")
    }
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
    if ((!Properties.contains("args.javahelp.map"))) {
      Properties("args.javahelp.map") = Properties("dita.map.toc.root")
    }

  }
  def ditaMapJavahelpToc() {
    println("\ndita.map.javahelp.toc:")
    println("Build JavaHelp TOC file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelptoc.xsl")))
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
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.javahelp.toc") + ".xml"))
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
  def ditaOutMapJavahelpToc() {
    println("\ndita.out.map.javahelp.toc:")
    println("Build JavaHelp TOC file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelptoc.xsl")))
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
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.javahelp.toc") + ".xml"))
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
  def ditaMapJavahelpMap() {
    println("\ndita.map.javahelp.map:")
    println("Build JavaHelp Map file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelpmap.xsl")))
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
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.javahelp.map") + ".jhm"))
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
  def ditaOutMapJavahelpMap() {
    println("\ndita.out.map.javahelp.map:")
    println("Build JavaHelp Map file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelpmap.xsl")))
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
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.javahelp.map") + ".jhm"))
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
  def ditaMapJavahelpSet() {
    println("\ndita.map.javahelp.set:")
    println("Build JavaHelp Set file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.map.javahelp.map", ditaMapJavahelpMap))
    if (!Properties.contains("old.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelpset.xsl")))
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
        transformer.setParameter("javahelpmap", Properties("args.javahelp.map"))
        transformer.setParameter("javahelptoc", Properties("args.javahelp.toc"))
        transformer.setParameter("basedir", Properties("basedir"))
        transformer.setParameter("outputdir", Properties("output.dir"))
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("dita.map.toc.root") + "_helpset.hs"))
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
  def ditaOutMapJavahelpSet() {
    println("\ndita.out.map.javahelp.set:")
    println("Build JavaHelp Set file")
    History.depends(("dita.map.javahelp.init", ditaMapJavahelpInit), ("dita.out.map.javahelp.map", ditaOutMapJavahelpMap))
    if (!Properties.contains("inner.transform")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2javahelpset.xsl")))
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
        transformer.setParameter("javahelpmap", Properties("args.javahelp.map"))
        transformer.setParameter("javahelptoc", Properties("args.javahelp.toc"))
        transformer.setParameter("basedir", Properties("basedir"))
        transformer.setParameter("outputdir", Properties("output.dir"))
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("dita.map.toc.root") + "_helpset.hs"))
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
  def ditaMapJavahelpIndex() {
    println("\ndita.map.javahelp.index:")
    println("Build JavaHelp Index file")
    if (!Properties.contains("old.transform")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = ".html"
    attrs("indextype") = "javahelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def ditaOutMapJavahelpIndex() {
    println("\ndita.out.map.javahelp.index:")
    println("Build JavaHelp Index file")
    if (!Properties.contains("inner.transform")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("dita.map.filename.root") + ".xml"
    attrs("targetext") = ".html"
    attrs("indextype") = "javahelp"
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def compileJavaHelp() {
    println("\ncompile.Java.Help:")
    println("Compile Java Help output")
    if (!Properties.contains("env.JHHOME")) {
      return
    }
    if (Properties.contains("old.transform")) {
      Properties("compile.dir") = Properties("dita.map.output.dir")
    }
    if (Properties.contains("inner.transform")) {
      Properties("compile.dir") = Properties("output.dir")
    }

  }
  def ditaTopicsJavahelp() {
    println("\ndita.topics.javahelp:")
    History.depends(("dita.topics.html", ditaTopicsHtml))

  }

}
