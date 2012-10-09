package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

import org.dita.dost.util.Constants._
import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.resolver.DitaURIResolverFactory
import org.dita.dost.util.FileUtils

class EclipseHelp(ditaDir: File) extends XHTML(ditaDir) {

  $("ant.file.dita2eclipsehelp") = new File("")

  def ditaEclipsehelpInit() {
    logger.logInfo("\ndita.eclipsehelp.init:")
    if (!$.contains("args.xsl")) {
      $("args.xsl") = $("dita.plugin.org.dita.eclipsehelp.dir") + "/xsl/dita2xhtml_eclipsehelp.xsl"
    }
  }

  def ditaIndexEclipsehelpInit() {
    logger.logInfo("\ndita.index.eclipsehelp.init:")
    if (!$.contains("dita.eclipsehelp.index.class")) {
      $("dita.eclipsehelp.index.class") = "org.dita.dost.writer.EclipseIndexWriter"
    }
  }

  override def run() {
    logger.logInfo("\nrun:")
    History.depends(("build-init", buildInit), ("dita.eclipsehelp.init", ditaEclipsehelpInit), ("preprocess", preprocess), ("copy-css", copyCss), ("dita.topics.xhtml", ditaTopicsXhtml), ("dita.inner.topics.xhtml", ditaInnerTopicsXhtml), ("dita.outer.topics.xhtml", ditaOuterTopicsXhtml))
    if (noMap != null) {
      return
    }

    ditaMapEclipse()
  }

  def ditaMapEclipse() {
    logger.logInfo("\ndita.map.eclipse:")
    History.depends(("dita.map.eclipse.init", ditaMapEclipseInit), ("copy-plugin-files", copyPluginFiles), ("dita.map.eclipse.fragment.language.init", ditaMapEclipseFragmentLanguageInit), ("dita.map.eclipse.fragment.language.country.init", ditaMapEclipseFragmentLanguageCountryInit), ("dita.map.eclipse.fragment.error", ditaMapEclipseFragmentError))
  }

  def ditaMapEclipseInit() {
    logger.logInfo("\ndita.map.eclipse.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("eclipse.plugin")) {
      return
    }

    ditaMapEclipseGeneratePlugin()
  }

  /**Init properties for EclipseHelp */
  def ditaMapEclipsePluginInit() {
    logger.logInfo("\ndita.map.eclipse.plugin.init:")
    $("dita.map.toc.root") = new File($("dita.input.filename")).getName()
    if (!$.contains("args.eclipsehelp.toc")) {
      $("args.eclipsehelp.toc") = $("dita.map.toc.root")
    }
    if (!$.contains("out.ext")) {
      $("out.ext") = ".html"
    }
    if ($("dita.eclipse.plugin") == "no") {
      $("noPlugin") = "true"
    }
    if (($.contains("args.eclipsehelp.language") && !$.contains("args.eclipsehelp.country"))) {
      $("eclipse.fragment.language") = "true"
    }
    if (($.contains("args.eclipsehelp.language") && $.contains("args.eclipsehelp.country"))) {
      $("eclipse.fragment.country") = "true"
    }
    if (!(($.contains("args.eclipsehelp.language") || $.contains("args.eclipsehelp.country") || ($.contains("args.eclipsehelp.country") && $.contains("args.eclipsehelp.language"))))) {
      $("eclipse.plugin") = "true"
    }
    if (($.contains("args.eclipsehelp.country") && !$.contains("args.eclipsehelp.language"))) {
      $("eclipse.fragment.error") = "true"
    }
    if (!$.contains("args.eclipsehelp.indexsee")) {
      $("args.eclipsehelp.indexsee") = "false"
    }
  }

  /**Build EclipseHelp TOC file */
  def ditaMapEclipseToc() {
    logger.logInfo("\ndita.map.eclipse.toc:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("old.transform")) {
      return
    }
    if (noMap != null) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val temp_ext = ".xml"
    val files = job.getSet("fullditamaplist") ++ job.getSet("chunkedditamaplist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      if ($.contains("workdir")) {
        transformer.setParameter("WORKDIR", $("workdir"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build EclipseHelp TOC file */
  def ditaOutMapEclipseToc() {
    logger.logInfo("\ndita.out.map.eclipse.toc:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("inner.transform")) {
      return
    }
    if (noMap != null) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("output.dir"))
    val files = job.getSet("fullditamaplist") ++ job.getSet("chunkedditamaplist") -- job.getSet("resourceonlylist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("out.ext")) {
        transformer.setParameter("OUTEXT", $("out.ext"))
      }
      if ($.contains("workdir")) {
        transformer.setParameter("WORKDIR", $("workdir"))
      }
      val in_file = new File(base_dir, l)
      val out_file = new File(globMap(new File(dest_dir, l).getAbsolutePath(), "^(" + $("tempdirToinputmapdir.relative.value") + ")(.*?)(\\.ditamap)$$", "\\2\\.xml"))
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
  }

  /**Build Eclipse Help index file */
  def ditaMapEclipseIndex() {
    logger.logInfo("\ndita.map.eclipse.index:")
    History.depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
    if (!$.contains("old.transform")) {
      return
    }
    if (noMap != null) {
      return
    }

    logger.logInfo(" args.eclipsehelp.indexsee = " + $("args.eclipsehelp.indexsee") + " ")
    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("targetext", $("out.ext"))
    modulePipelineInput.setAttribute("indextype", "eclipsehelp")
    modulePipelineInput.setAttribute("indexclass", $("dita.eclipsehelp.index.class"))
    modulePipelineInput.setAttribute("eclipse.indexsee", $("args.eclipsehelp.indexsee"))
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build Eclipse Help index file */
  def ditaOutMapEclipseIndex() {
    logger.logInfo("\ndita.out.map.eclipse.index:")
    History.depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
    if (!$.contains("inner.transform")) {
      return
    }
    if (noMap != null) {
      return
    }

    logger.logInfo(" args.eclipsehelp.indexsee = " + $("args.eclipsehelp.indexsee") + " ")
    import org.dita.dost.module.IndexTermExtractModule
    val module = new org.dita.dost.module.IndexTermExtractModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("output", $("output.dir") + $("file.separator") + "index.xml")
    modulePipelineInput.setAttribute("targetext", $("out.ext"))
    modulePipelineInput.setAttribute("indextype", "eclipsehelp")
    modulePipelineInput.setAttribute("indexclass", $("dita.eclipsehelp.index.class"))
    modulePipelineInput.setAttribute("eclipse.indexsee", $("args.eclipsehelp.indexsee"))
    if ($.contains("args.dita.locale")) {
      modulePipelineInput.setAttribute("encoding", $("args.dita.locale"))
    }
    module.execute(modulePipelineInput)
  }

  /**Build Eclipsehelp plugin file */
  def ditaMapEclipsePlugin() {
    logger.logInfo("\ndita.map.eclipse.plugin:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("old.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("dita.map.output.dir") + File.separator + "plugin.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("TOCROOT", $("args.eclipsehelp.toc"))
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    if ($.contains("args.eclipse.symbolic.name")) {
      transformer.setParameter("osgi.symbolic.name", $("args.eclipse.symbolic.name"))
    }
    transformer.setParameter("dita.plugin.output", "dita.eclipse.plugin")
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build Eclipsehelp plugin file */
  def ditaOutMapEclipsePlugin() {
    logger.logInfo("\ndita.out.map.eclipse.plugin:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("inner.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("output.dir") + File.separator + "plugin.xml")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("TOCROOT", $("args.eclipsehelp.toc"))
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    if ($.contains("args.eclipse.symbolic.name")) {
      transformer.setParameter("osgi.symbolic.name", $("args.eclipse.symbolic.name"))
    }
    transformer.setParameter("dita.plugin.output", "dita.eclipse.plugin")
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build Eclipsehelp manifest.mf file */
  def ditaMapEclipseManifestFile() {
    logger.logInfo("\ndita.map.eclipse.manifest.file:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("old.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    if ($.contains("args.eclipse.symbolic.name")) {
      transformer.setParameter("osgi.symbolic.name", $("args.eclipse.symbolic.name"))
    }
    transformer.setParameter("plugin", $("eclipse.plugin"))
    if ($.contains("eclipse.fragment.country")) {
      transformer.setParameter("fragment.country", $("args.eclipsehelp.country"))
    }
    if ($.contains("args.eclipsehelp.language")) {
      transformer.setParameter("fragment.lang", $("args.eclipsehelp.language"))
    }
    transformer.setParameter("dita.plugin.output", "dita.eclipse.manifest")
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Build Eclipsehelp manifest.mf file */
  def ditaOutMapEclipseManifestFile() {
    logger.logInfo("\ndita.out.map.eclipse.manifest.file:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("inner.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    if ($.contains("args.eclipse.symbolic.name")) {
      transformer.setParameter("osgi.symbolic.name", $("args.eclipse.symbolic.name"))
    }
    transformer.setParameter("plugin", $("eclipse.plugin"))
    if ($.contains("eclipse.fragment.country")) {
      transformer.setParameter("fragment.country", $("args.eclipsehelp.country"))
    }
    if ($.contains("args.eclipsehelp.language")) {
      transformer.setParameter("fragment.lang", $("args.eclipsehelp.language"))
    }
    transformer.setParameter("dita.plugin.output", "dita.eclipse.manifest")
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Create eclipse plugin.properties file */
  def ditaMapEclipsePluginProperties() {
    logger.logInfo("\ndita.map.eclipse.plugin.properties:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("old.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("output.dir") + File.separator + "plugin.properties")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Create eclipse plugin.properties file */
  def ditaOutMapEclipsePluginProperties() {
    logger.logInfo("\ndita.out.map.eclipse.plugin.properties:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("inner.transform")) {
      return
    }
    if ($.contains("noPlugin")) {
      return
    }

    val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("output.dir") + File.separator + "plugin.properties")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
    if ($.contains("args.eclipse.version")) {
      transformer.setParameter("version", $("args.eclipse.version"))
    }
    if ($.contains("args.eclipse.provider")) {
      transformer.setParameter("provider", $("args.eclipse.provider"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  def ditaMapEclipseFragmentLanguageInit() {
    logger.logInfo("\ndita.map.eclipse.fragment.language.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("eclipse.fragment.language")) {
      return
    }

    $("fragment.dirname.init") = "nl"
    $("fragment.dirname") = $("fragment.dirname.init") + $("file.separator") + $("args.eclipsehelp.language")
    $("fragment.property.name") = $("args.eclipsehelp.language")
    ditaMapEclipseGenetrateFragment()
  }

  def ditaMapEclipseFragmentLanguageCountryInit() {
    logger.logInfo("\ndita.map.eclipse.fragment.language.country.init:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("eclipse.fragment.country")) {
      return
    }

    $("fragment.dirname.init") = "nl"
    $("fragment.dirname") = $("fragment.dirname.init") + $("file.separator") + $("args.eclipsehelp.language") + $("file.separator") + $("args.eclipsehelp.country")
    $("fragment.property.name") = $("args.eclipsehelp.language") + "_" + $("args.eclipsehelp.country")
    ditaMapEclipseGenetrateFragment()
  }

  def ditaMapEclipseFragmentError() {
    logger.logInfo("\ndita.map.eclipse.fragment.error:")
    History.depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
    if (!$.contains("eclipse.fragment.error")) {
      return
    }

    logger.logInfo("")
  }

  def ditaMapEclipseFragmentMoveFiles() {
    logger.logInfo("\ndita.map.eclipse.fragment.move.files:")
    if (!$.contains("old.transform")) {
      return
    }

    delete(new File($("output.dir") + File.separator + "plugin.xml"))
    delete(new File($("output.dir") + File.separator + "plugincustomization.ini"))
    move(new File($("dita.map.output.dir")), new File($("dita.map.output.dir") + File.separator + $("fragment.dirname")), listAll(new File($("dita.map.output.dir"))))
  }

  def ditaOutMapEclipseFragmentMoveFiles() {
    logger.logInfo("\ndita.out.map.eclipse.fragment.move.files:")
    if (!$.contains("inner.transform")) {
      return
    }

    delete(new File($("output.dir") + File.separator + "plugin.xml"))
    delete(new File($("output.dir") + File.separator + "plugincustomization.ini"))
    move(new File($("output.dir")), new File($("output.dir") + File.separator + $("fragment.dirname")), listAll(new File($("output.dir"))))
  }

  def ditaMapEclipseGeneratePlugin() {
    logger.logInfo("\ndita.map.eclipse.generate.plugin:")
    History.depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin", ditaMapEclipsePlugin), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin))
  }

  def ditaMapEclipseGenetrateFragment() {
    logger.logInfo("\ndita.map.eclipse.genetrate.fragment:")
    History.depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin), ("dita.map.eclipse.fragment.move.files", ditaMapEclipseFragmentMoveFiles), ("dita.out.map.eclipse.fragment.move.files", ditaOutMapEclipseFragmentMoveFiles))
  }

  def ditaTopicsEclipse() {
    logger.logInfo("\ndita.topics.eclipse:")
    History.depends(("dita.topics.xhtml", ditaTopicsXhtml))
  }

  def copyPluginFiles() {
    logger.logInfo("\ncopy-plugin-files:")
    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), Set("disabled_book.css") ++ Set("narrow_book.css") ++ Set("${os}_narrow_book.css") ++ Set("book.css") ++ Set("plugincustomization.ini") ++ Set("helpData.xml"))
  }
}
