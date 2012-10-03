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

class Dita2eclipsehelp(ditaDir: File) extends Dita2xhtml(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.eclipsehelp/build_dita2eclipsehelp.xml

  Properties("ant.file.dita2eclipsehelp") = new File("")
  def ditaEclipsehelpInit() {
    println("\ndita.eclipsehelp.init:")
    if ((!Properties.contains("args.xsl"))) {
      Properties("args.xsl") = Properties("dita.plugin.org.dita.eclipsehelp.dir") + "/xsl/dita2xhtml_eclipsehelp.xsl"
    }

  }
  def ditaIndexEclipsehelpInit() {
    println("\ndita.index.eclipsehelp.init:")
    if ((!Properties.contains("dita.eclipsehelp.index.class"))) {
      Properties("dita.eclipsehelp.index.class") = "org.dita.dost.writer.EclipseIndexWriter"
    }

  }
  def dita2eclipsehelp() {
    println("\ndita2eclipsehelp:")
    History.depends(("build-init", buildInit), ("dita.eclipsehelp.init", ditaEclipsehelpInit), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.xhtml", ditaTopicsXhtml), ("dita.inner.topics.xhtml", ditaInnerTopicsXhtml), ("dita.outer.topics.xhtml", ditaOuterTopicsXhtml))
    if (Properties.contains("noMap")) {
      return
    }
    ditaMapEclipse()

  }
  def ditaMapEclipse() {
    println("\ndita.map.eclipse:")
    History.depends(("dita.map.eclipse.init", ditaMapEclipseInit), ("copy-plugin-files", copyPluginFiles), ("dita.map.eclipse.fragment.language.init", ditaMapEclipseFragmentLanguageInit), ("dita.map.eclipse.fragment.language.country.init", ditaMapEclipseFragmentLanguageCountryInit), ("dita.map.eclipse.fragment.error", ditaMapEclipseFragmentError))

  }
  def ditaMapEclipseInit() {
    println("\ndita.map.eclipse.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("eclipse.plugin")) {
      return
    }
    ditaMapEclipseGeneratePlugin()

  }
  def ditaMapEclipsePluginInit() {
    println("\ndita.map.eclipse.plugin.init:")
    println("Init properties for EclipseHelp")
    Properties("dita.map.toc.root") = new File(Properties("dita.input.filename")).getName()
    if ((!Properties.contains("args.eclipsehelp.toc"))) {
      Properties("args.eclipsehelp.toc") = Properties("dita.map.toc.root")
    }
    if ((!Properties.contains("out.ext"))) {
      Properties("out.ext") = ".html"
    }
    if (Properties("dita.eclipse.plugin") == "no") {
      Properties("noPlugin") = "true"
    }
    if ((Properties.contains("args.eclipsehelp.language") && (!Properties.contains("args.eclipsehelp.country")))) {
      Properties("eclipse.fragment.language") = "true"
    }
    if ((Properties.contains("args.eclipsehelp.language") && Properties.contains("args.eclipsehelp.country"))) {
      Properties("eclipse.fragment.country") = "true"
    }
    if (!((Properties.contains("args.eclipsehelp.language") || Properties.contains("args.eclipsehelp.country") || (Properties.contains("args.eclipsehelp.country") && Properties.contains("args.eclipsehelp.language"))))) {
      Properties("eclipse.plugin") = "true"
    }
    if ((Properties.contains("args.eclipsehelp.country") && (!Properties.contains("args.eclipsehelp.language")))) {
      Properties("eclipse.fragment.error") = "true"
    }
    if ((!Properties.contains("args.eclipsehelp.indexsee"))) {
      Properties("args.eclipsehelp.indexsee") = "false"
    }

  }
  def ditaMapEclipseToc() {
    println("\ndita.map.eclipse.toc:")
    println("Build EclipseHelp TOC file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2eclipse.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val temp_ext = ".xml"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditamapfile")), "UTF-8")
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
        if (Properties.contains("workdir")) {
          transformer.setParameter("WORKDIR", Properties("workdir"))

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
  def ditaOutMapEclipseToc() {
    println("\ndita.out.map.eclipse.toc:")
    println("Build EclipseHelp TOC file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2eclipse.xsl")))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("output.dir"))
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + File.separator + Properties("fullditamapfile")), "UTF-8")
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
        if (Properties.contains("workdir")) {
          transformer.setParameter("WORKDIR", Properties("workdir"))

        }
        val in_file = new File(base_dir, l)
        val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "^(" + Properties("tempdirToinputmapdir.relative.value") + ")(.*?)(\\.ditamap)$$", "\\2\\.xml"))
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
  def ditaMapEclipseIndex() {
    println("\ndita.map.eclipse.index:")
    println("Build Eclipse Help index file")
    History.depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }
    println(" args.eclipsehelp.indexsee = " + Properties("args.eclipsehelp.indexsee") + " ")
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + Properties("user.input.file")
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "eclipsehelp"
    attrs("indexclass") = Properties("dita.eclipsehelp.index.class")
    attrs("eclipse.indexsee") = Properties("args.eclipsehelp.indexsee")
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def ditaOutMapEclipseIndex() {
    println("\ndita.out.map.eclipse.index:")
    println("Build Eclipse Help index file")
    History.depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noMap")) {
      return
    }
    println(" args.eclipsehelp.indexsee = " + Properties("args.eclipsehelp.indexsee") + " ")
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.IndexTermExtractModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("output") = Properties("output.dir") + File.separator + "index.xml"
    attrs("targetext") = Properties("out.ext")
    attrs("indextype") = "eclipsehelp"
    attrs("indexclass") = Properties("dita.eclipsehelp.index.class")
    attrs("eclipse.indexsee") = Properties("args.eclipsehelp.indexsee")
    if (Properties.contains("args.dita.locale")) {
      attrs("encoding") = Properties("args.dita.locale")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def ditaMapEclipsePlugin() {
    println("\ndita.map.eclipse.plugin:")
    println("Build Eclipsehelp plugin file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("dita.map.output.dir") + File.separator + "plugin.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      transformer.setParameter("TOCROOT", Properties("args.eclipsehelp.toc"))
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      if (Properties.contains("args.eclipse.symbolic.name")) {
        transformer.setParameter("osgi.symbolic.name", Properties("args.eclipse.symbolic.name"))

      }
      transformer.setParameter("dita.plugin.output", "dita.eclipse.plugin")
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaOutMapEclipsePlugin() {
    println("\ndita.out.map.eclipse.plugin:")
    println("Build Eclipsehelp plugin file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("output.dir") + File.separator + "plugin.xml")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      transformer.setParameter("TOCROOT", Properties("args.eclipsehelp.toc"))
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      if (Properties.contains("args.eclipse.symbolic.name")) {
        transformer.setParameter("osgi.symbolic.name", Properties("args.eclipse.symbolic.name"))

      }
      transformer.setParameter("dita.plugin.output", "dita.eclipse.plugin")
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaMapEclipseManifestFile() {
    println("\ndita.map.eclipse.manifest.file:")
    println("Build Eclipsehelp manifest.mf file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      if (Properties.contains("args.eclipse.symbolic.name")) {
        transformer.setParameter("osgi.symbolic.name", Properties("args.eclipse.symbolic.name"))

      }
      transformer.setParameter("plugin", Properties("eclipse.plugin"))
      if (Properties.contains("eclipse.fragment.country")) {
        transformer.setParameter("fragment.country", Properties("args.eclipsehelp.country"))

      }
      if (Properties.contains("args.eclipsehelp.language")) {
        transformer.setParameter("fragment.lang", Properties("args.eclipsehelp.language"))

      }
      transformer.setParameter("dita.plugin.output", "dita.eclipse.manifest")
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaOutMapEclipseManifestFile() {
    println("\ndita.out.map.eclipse.manifest.file:")
    println("Build Eclipsehelp manifest.mf file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      if (Properties.contains("args.eclipse.symbolic.name")) {
        transformer.setParameter("osgi.symbolic.name", Properties("args.eclipse.symbolic.name"))

      }
      transformer.setParameter("plugin", Properties("eclipse.plugin"))
      if (Properties.contains("eclipse.fragment.country")) {
        transformer.setParameter("fragment.country", Properties("args.eclipsehelp.country"))

      }
      if (Properties.contains("args.eclipsehelp.language")) {
        transformer.setParameter("fragment.lang", Properties("args.eclipsehelp.language"))

      }
      transformer.setParameter("dita.plugin.output", "dita.eclipse.manifest")
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaMapEclipsePluginProperties() {
    println("\ndita.map.eclipse.plugin.properties:")
    println("Create eclipse plugin.properties file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("old.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("output.dir") + File.separator + "plugin.properties")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaOutMapEclipsePluginProperties() {
    println("\ndita.out.map.eclipse.plugin.properties:")
    println("Create eclipse plugin.properties file")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("inner.transform")) {
      return
    }
    if (Properties.contains("noPlugin")) {
      return
    }

    try {
      val templates = TransformerFactory.newInstance().newTemplates(new StreamSource(new File(Properties("dita.script.dir") + File.separator + "map2plugin.xsl")))
      val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
      val out_file = new File(Properties("output.dir") + File.separator + "plugin.properties")
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val transformer = templates.newTransformer()
      transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
      if (Properties.contains("args.eclipse.version")) {
        transformer.setParameter("version", Properties("args.eclipse.version"))

      }
      if (Properties.contains("args.eclipse.provider")) {
        transformer.setParameter("provider", Properties("args.eclipse.provider"))

      }
      val source = new StreamSource(in_file)
      val result = new StreamResult(out_file)
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def ditaMapEclipseFragmentLanguageInit() {
    println("\ndita.map.eclipse.fragment.language.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("eclipse.fragment.language")) {
      return
    }
    Properties("fragment.dirname.init") = "nl"
    Properties("fragment.dirname") = Properties("fragment.dirname.init") + File.separator + Properties("args.eclipsehelp.language")
    Properties("fragment.property.name") = Properties("args.eclipsehelp.language")
    ditaMapEclipseGenetrateFragment()

  }
  def ditaMapEclipseFragmentLanguageCountryInit() {
    println("\ndita.map.eclipse.fragment.language.country.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("eclipse.fragment.country")) {
      return
    }
    Properties("fragment.dirname.init") = "nl"
    Properties("fragment.dirname") = Properties("fragment.dirname.init") + File.separator + Properties("args.eclipsehelp.language") + File.separator + Properties("args.eclipsehelp.country")
    Properties("fragment.property.name") = Properties("args.eclipsehelp.language") + "_" + Properties("args.eclipsehelp.country")
    ditaMapEclipseGenetrateFragment()

  }
  def ditaMapEclipseFragmentError() {
    println("\ndita.map.eclipse.fragment.error:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!Properties.contains("eclipse.fragment.error")) {
      return
    }
    println("")

  }
  def ditaMapEclipseFragmentMoveFiles() {
    println("\ndita.map.eclipse.fragment.move.files:")
    if (!Properties.contains("old.transform")) {
      return
    }

  }
  def ditaOutMapEclipseFragmentMoveFiles() {
    println("\ndita.out.map.eclipse.fragment.move.files:")
    if (!Properties.contains("inner.transform")) {
      return
    }

  }
  def ditaMapEclipseGeneratePlugin() {
    println("\ndita.map.eclipse.generate.plugin:")
    History.depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin", ditaMapEclipsePlugin), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin))

  }
  def ditaMapEclipseGenetrateFragment() {
    println("\ndita.map.eclipse.genetrate.fragment:")
    History.depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin), ("dita.map.eclipse.fragment.move.files", ditaMapEclipseFragmentMoveFiles), ("dita.out.map.eclipse.fragment.move.files", ditaOutMapEclipseFragmentMoveFiles))

  }
  def ditaTopicsEclipse() {
    println("\ndita.topics.eclipse:")
    History.depends(("dita.topics.xhtml", ditaTopicsXhtml))

  }
  def copyPluginFiles() {
    println("\ncopy-plugin-files:")
    copy(Properties("user.input.dir"), Properties("output.dir"), "")

  }

}
