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

class Dita2xhtml(ditaDir: File) extends DitaotPreprocess(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.xhtml/build_dita2xhtml.xml

  Properties("ant.file.dita2xhtml") = new File("")
  // start src/main/plugins/org.dita.xhtml/build_general.xml
  // end src/main/plugins/org.dita.xhtml/build_general.xml
  def ditaXhtmlInit() {
    println("\ndita.xhtml.init:")
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
    Properties("dita.input.valfile.url") = new File(Properties("dita.input.valfile")).toURI().toASCIIString()
    if ((!Properties.contains("dita.xhtml.reloadstylesheet"))) {
      Properties("dita.xhtml.reloadstylesheet") = "false"
    }

  }
  def ditaTopicsXhtml() {
    println("\ndita.topics.xhtml:")
    println("Build XHTML output from dita inner and outer topics,which will adjust the directory.")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def ditaTopicsHtml() {
    println("\ndita.topics.html:")
    println("Build HTML files from inner and outer dita topics,which will adjust the directory. ")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def ditaInnerTopicsXhtml() {
    println("\ndita.inner.topics.xhtml:")
    println("Build XHTML output from inner dita topics")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def ditaInnerTopicsHtml() {
    println("\ndita.inner.topics.html:")
    println("Build HTML files from inner dita topics")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditatopicfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def checkouterTransform() {
    println("\ncheckouterTransform:")
    if ((Properties("generate.copy.outer") == "2" && (Properties.contains("outditafileslist") && !("" == Properties("outditafileslist"))))) {
      Properties("outer.transform") = "true"
    }

  }
  def ditaOuterTopicsXhtml() {
    println("\ndita.outer.topics.xhtml:")
    println("Build XHTML output from outer dita topics")
    History.depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2xhtml.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("outditafilesfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def ditaOuterTopicsHtml() {
    println("\ndita.outer.topics.html:")
    println("Build HTML files from outer dita topics")
    History.depends(("dita.xhtml.init", ditaXhtmlInit), ("checkouterTransform", checkouterTransform))
    if (!Properties.contains("outer.transform")) {
      return
    }
    if (Properties.contains("noTopic")) {
      return
    }
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/dita2html.xsl"
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xsl"))))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir") + File.separator + Properties("uplevels"))
      val temp_ext = Properties("out.ext")
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("outditafilesfile")), "UTF-8")
      val files = scala.collection.mutable.ListBuffer[String]()
      for (line <- includes_file.getLines()) {
        files.add(line)
      }
      includes_file.close()
      for (l <- files) {
        val transformer = templates.newTransformer()
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        if (Properties.contains("dita.ext")) {
          transformer.setParameter("DITAEXT", Properties("dita.ext"))

        }
        if (Properties.contains("dita.input.valfile")) {
          transformer.setParameter("FILTERFILE", Properties("dita.input.valfile.url"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.hdf")) {
          transformer.setParameter("HDF", Properties("args.hdf"))

        }
        if (Properties.contains("args.hdr")) {
          transformer.setParameter("HDR", Properties("args.hdr"))

        }
        if (Properties.contains("args.ftr")) {
          transformer.setParameter("FTR", Properties("args.ftr"))

        }
        if (Properties.contains("args.draft")) {
          transformer.setParameter("DRAFT", Properties("args.draft"))

        }
        if (Properties.contains("args.artlbl")) {
          transformer.setParameter("ARTLBL", Properties("args.artlbl"))

        }
        if (Properties.contains("args.gen.task.lbl")) {
          transformer.setParameter("GENERATE-TASK-LABELS", Properties("args.gen.task.lbl"))

        }
        if (Properties.contains("args.xhtml.classattr")) {
          transformer.setParameter("PRESERVE-DITA-CLASS", Properties("args.xhtml.classattr"))

        }
        if (Properties.contains("args.hide.parent.link")) {
          transformer.setParameter("NOPARENTLINK", Properties("args.hide.parent.link"))

        }
        transformer.setParameter("include.rellinks", Properties("include.rellinks"))
        if (Properties.contains("args.breadcrumbs")) {
          transformer.setParameter("BREADCRUMBS", Properties("args.breadcrumbs"))

        }
        if (Properties.contains("args.indexshow")) {
          transformer.setParameter("INDEXSHOW", Properties("args.indexshow"))

        }
        if (Properties.contains("args.gen.default.meta")) {
          transformer.setParameter("genDefMeta", Properties("args.gen.default.meta"))

        }
        if (Properties.contains("out.ext")) {
          transformer.setParameter("OUTEXT", Properties("out.ext"))

        }
        transformer.setParameter("BASEDIR", Properties("basedir"))
        transformer.setParameter("OUTPUTDIR", Properties("output.dir"))
        if (Properties.contains("args.debug")) {
          transformer.setParameter("DBG", Properties("args.debug"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        transformer.setParameter("FILENAME", in_file.getName())
        transformer.setParameter("FILEDIR", in_file.getParent())
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
  def dita2xhtml() {
    println("\ndita2xhtml:")
    History.depends(("build-init", buildInit), ("preprocess", preprocess), ("dita.map.xhtml", ditaMapXhtml), ("copy-css", copyCss), ("dita.topics.xhtml", ditaTopicsXhtml), ("dita.inner.topics.xhtml", ditaInnerTopicsXhtml), ("dita.outer.topics.xhtml", ditaOuterTopicsXhtml))

  }
  def ditaMapXhtml() {
    println("\ndita.map.xhtml:")
    History.depends(("dita.map.xhtml.init", ditaMapXhtmlInit), ("dita.map.xhtml.toc", ditaMapXhtmlToc), ("dita.out.map.xhtml.toc", ditaOutMapXhtmlToc))

  }
  def ditaMapXhtmlInit() {
    println("\ndita.map.xhtml.init:")
    History.depends(("dita.xhtml.init", ditaXhtmlInit))
    if (Properties.contains("noMap")) {
      return
    }
    if ((!Properties.contains("args.xhtml.toc.xsl"))) {
      Properties("args.xhtml.toc.xsl") = Properties("dita.plugin.org.dita.xhtml.dir") + "/xsl/map2xhtmtoc.xsl"
    }
    if ((!Properties.contains("args.xhtml.toc"))) {
      Properties("args.xhtml.toc") = "index"
    }

  }
  def ditaMapXhtmlToc() {
    println("\ndita.map.xhtml.toc:")
    println("Build HTML TOC file")
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xhtml.toc.xsl"))))
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
        if (Properties.contains("args.xhtml.contenttarget")) {
          transformer.setParameter("contenttarget", Properties("args.xhtml.contenttarget"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.xhtml.toc.class")) {
          transformer.setParameter("OUTPUTCLASS", Properties("args.xhtml.toc.class"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "*" + Properties("dita.input.filename"), "*" + Properties("args.xhtml.toc") + Properties("out.ext")))
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
  def ditaOutMapXhtmlToc() {
    println("\ndita.out.map.xhtml.toc:")
    println("Build HTML TOC file,which will adjust the directory")
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("args.xhtml.toc.xsl"))))
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
        if (Properties.contains("args.xhtml.contenttarget")) {
          transformer.setParameter("contenttarget", Properties("args.xhtml.contenttarget"))

        }
        if (Properties.contains("args.css.file")) {
          transformer.setParameter("CSS", Properties("args.css.file"))

        }
        if (Properties.contains("user.csspath")) {
          transformer.setParameter("CSSPATH", Properties("user.csspath"))

        }
        if (Properties.contains("args.xhtml.toc.class")) {
          transformer.setParameter("OUTPUTCLASS", Properties("args.xhtml.toc.class"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), Properties("user.input.file"), Properties("args.xhtml.toc") + Properties("out.ext")))
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
  def copyRevflag() {
    println("\ncopy-revflag:")
    if (!Properties.contains("dita.input.valfile")) {
      return
    }
    println(get_msg("DOTA069W"))

  }
  def copyCss() {
    println("\ncopy-css:")
    println("Copy CSS files")
    if (Properties.contains("user.csspath.url")) {
      return
    }
    if ((Properties("args.copycss") == "yes" && Properties.contains("args.css.present"))) {
      Properties("user.copycss.yes") = "true"
    }
    Properties("user.csspath.real") = new File(Properties("output.dir") + "/" + Properties("user.csspath"))
    if (!new File(Properties("user.csspath.real")).exists()) {
      new File(Properties("user.csspath.real")).mkdirs()
    }
    copy(Properties("dita.resource.dir"), Properties("user.csspath.real"), "*.css")
    copyCssUser()

  }
  def copyCssUser() {
    println("\ncopy-css-user:")
    if (!Properties.contains("user.copycss.yes")) {
      return
    }

  }

}
