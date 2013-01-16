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
import org.dita.dost.util.Job

abstract class Preprocess(ditaDir: File) extends Transtype(ditaDir) {

  $("ant.file.ditaot-preprocess") = new File("")

  var noTopic: Boolean = false
  var noConref: Boolean = false
  var noMap: Boolean = false
  var noConrefPush: Boolean = false
  var noImagelist: Boolean = false
  var noHtmllist: Boolean = false
  var noSublist: Boolean = false
  var noKeyref: Boolean = false
  var noCoderef: Boolean = false
  var innerTransform: Boolean = false
  var oldTransform: Boolean = false
  var is64bit: Boolean = false
  var is32bit: Boolean = false
  var noPlugin: Boolean = false

  $.readProperties(new File($("basedir") + File.separator + "local.properties"))
  $("dita.plugin.org.dita.troff.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.troff")
  $("dita.plugin.org.dita.eclipsecontent.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.eclipsecontent")
  $("dita.plugin.org.dita.eclipsehelp.dir") = ditaDir
  $("dita.plugin.org.dita.specialization.dita11.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.specialization.dita11")
  $("dita.plugin.org.dita.xhtml.dir") = ditaDir
  $("dita.plugin.org.dita.odt.dir") = ditaDir
  $("dita.plugin.org.dita.pdf2.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.pdf2")
  $("dita.plugin.org.dita.specialization.dita132.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.specialization.dita132")
  $("dita.plugin.org.dita.wordrtf.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.wordrtf")
  $("dita.plugin.org.dita.docbook.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.docbook")
  $("dita.plugin.org.dita.specialization.eclipsemap.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.specialization.eclipsemap")
  $("dita.plugin.org.dita.base.dir") = ditaDir
  $("dita.plugin.org.dita.htmlhelp.dir") = ditaDir
  $("dita.plugin.org.dita.pdf.dir") = new File(ditaDir + File.separator + "plugins" + File.separator + "org.dita.pdf")
  $("dita.plugin.org.dita.javahelp.dir") = ditaDir
  $("maxJavaMemory") = "500m"
  $.readProperties(new File(ditaDir + File.separator + "lib" + File.separator + "org.dita.dost.platform" + File.separator + "plugin.properties"))
  $.readProperties(new File(ditaDir + File.separator + "lib" + File.separator + "configuration.properties"))
  if (((System.getProperty("os.name") == "x86_64" || System.getProperty("os.name") == "amd64" || System.getProperty("os.name") == "ppc64") && !(System.getProperty("os.name") == "windows"))) {
    is64bit = true
  }
  if (!is64bit) {
    is32bit = true
  }
  if (is64bit) {
    $("jvmArchFlag") = "-d64"
  } else {
    $("jvmArchFlag") = ""
  }
  $("baseJVMArgLine") = $("jvmArchFlag") + " -Xmx" + $("maxJavaMemory")
  $("current.date") = "20120130"
  $("base.temp.dir") = new File($("basedir") + File.separator + "temp")
  $("dita.temp.dir") = new File($("base.temp.dir") + File.separator + "temp" + $("current.date"))
  $("output.dir") = new File($("basedir") + File.separator + "out")
  if (!$.contains("dita.preprocess.reloadstylesheet")) {
    $("dita.preprocess.reloadstylesheet") = "false"
  }

  def buildInit() {
    logger.logInfo("build-init:")
    depends(("init-URIResolver", initURIResolver), ("use-init", useInit), ("check-arg", checkArg))
  }

  def initURIResolver() {
    logger.logInfo("init-URIResolver:")
    var path = new File($("dita.temp.dir"))
    DitaURIResolverFactory.setPath(path.getAbsolutePath)
  }

  def useInit() {
    logger.logInfo("use-init:")
    if (($.contains("org.xml.sax.driver") && !$.contains("xml.parser"))) {
      $("xml.parser") = "XMLReader " + $("org.xml.sax.driver")
    }
    if ((class_available("org.apache.xerces.parsers.SAXParser") && !$.contains("xml.parser"))) {
      $("xml.parser") = "Xerces"
    }
    if ((class_available("com.sun.org.apache.xerces.internal.parsers.SAXParser") && !$.contains("xml.parser"))) {
      $("xml.parser") = "Xerces in Sun JDK 1.5"
    }
    if ((class_available("org.apache.crimson.parser.XMLReaderImpl") && !$.contains("xml.parser"))) {
      $("xml.parser") = "Crimson"
    }
  }

  /**Validate and init input arguments */
  def checkArg() {
    logger.logInfo("check-arg:")
    if (($.contains("args.input") && !$.contains("args.input.dir") && !(new File($("args.input")).exists()))) {
      logger.logError("DOTA069F")
      sys.exit()
    }
    if (($.contains("args.input") && $.contains("args.input.dir") && !((new File($("args.input")).exists() || new File($("args.input.dir") + File.separator + $("args.input")).exists())))) {
      logger.logError("DOTA069F")
      sys.exit()
    }
    if ((!$.contains("args.input"))) {
      logger.logError("DOTA002F")
      sys.exit()
    }
    if (($.contains("args.xsl") && !(new File($("args.xsl")).exists()))) {
      logger.logError("DOTA003F")
      sys.exit()
    }
    if (($.contains("args.ftr") && !(new File($("args.ftr")).exists()))) {
      logger.logError("DOTA007E")
      sys.exit()
    }
    if (($.contains("args.hdr") && !(new File($("args.hdr")).exists()))) {
      logger.logError("DOTA008E")
      sys.exit()
    }
    if (($.contains("args.hdf") && !(new File($("args.hdf")).exists()))) {
      logger.logError("DOTA009E")
      sys.exit()
    }
    if ($.contains("dita.input.valfile")) {
      logger.logError("DOTA012W")
      sys.exit()
    }
    if (($.contains("args.filter") && !$.contains("dita.input.valfile"))) {
      $("dita.input.valfile") = $("args.filter")
    }
    if (($.contains("args.outext") && !($("args.outext").indexOf(".") != -1))) {
      $("out.ext") = "." + $("args.outext")
    }
    if (($.contains("args.outext") && $("args.outext").indexOf(".") != -1)) {
      $("out.ext") = $("args.outext")
    }
    if (!$.contains("args.grammar.cache")) {
      $("args.grammar.cache") = "yes"
    }
    if (!$.contains("args.xml.systemid.set")) {
      $("args.xml.systemid.set") = "yes"
    }
    $("dita.input.filename") = new File($("args.input")).getName()
    if ($.contains("args.input.dir")) {
      $("dita.input.dirname") = $("args.input.dir")
    }
    $("dita.input.dirname") = new File($("args.input")).getParent()
    $("dita.map.filename.root") = new File($("dita.input.filename")).getName()
    $("dita.topic.filename.root") = new File($("dita.input.filename")).getName()
    if (!new File($("output.dir")).exists()) {
      new File($("output.dir")).mkdirs()
    }
    if (!new File($("dita.temp.dir")).exists()) {
      new File($("dita.temp.dir")).mkdirs()
    }
    if (($("args.csspath").indexOf("http://") != -1 || $("args.csspath").indexOf("https://") != -1)) {
      $("user.csspath.url") = "true"
    }
    if (new File($("args.csspath")).isAbsolute) {
      $("args.csspath.absolute") = "true"
    }
    if ((!$.contains("args.csspath") || $.contains("args.csspath.absolute"))) {
      $("user.csspath") = ""
    }
    if (!$.contains("user.csspath")) {
      $("user.csspath") = $("args.csspath") + "/"
    }
    if ($.contains("args.cssroot")) {
      $("args.css.real") = $("args.cssroot") + $("file.separator") + $("args.css")
    }
    if (!$.contains("args.cssroot")) {
      $("args.css.real") = $("args.css")
    }
    if (new File($("args.css.real")).exists() && new File($("args.css.real")).isFile()) {
      $("args.css.present") = "true"
    }
    $("args.css.file.temp") = new File($("args.css")).getName()
    if (($.contains("args.css.present") || $.contains("user.csspath.url"))) {
      $("args.css.file") = $("args.css.file.temp")
    }
    if (!$.contains("args.logdir")) {
      $("args.logdir") = $("output.dir")
    }
    if ((class_available("net.sf.saxon.StyleSheet") || class_available("net.sf.saxon.Transform"))) {
      $("xslt.parser") = "Saxon"
    } else {
      $("xslt.parser") = "Xalan"
    }
    if (class_available("com.ibm.icu.text.Collator")) {
      $("collator") = "ICU"
    } else {
      $("collator") = "JDL"
    }
    if (!$.contains("validate")) {
      $("validate") = "true"
    }
    if ($("args.rellinks") == "none") {
      $("include.rellinks") = ""
    }
    if ($("args.rellinks") == "nofamily") {
      $("include.rellinks") = "#default friend sample external other"
    }
    if (($("args.rellinks") == "all" || !$.contains("args.rellinks"))) {
      $("include.rellinks") = "#default parent child sibling friend next previous cousin ancestor descendant sample external other"
    }
    if (!$.contains("generate.copy.outer")) {
      $("generate.copy.outer") = "1"
    }
    if (!$.contains("onlytopic.in.map")) {
      $("onlytopic.in.map") = "false"
    }
    if (!$.contains("outer.control")) {
      $("outer.control") = "warn"
    }
    if (($("generate.copy.outer") == "1" || $("generate.copy.outer") == "2")) {
      innerTransform = true
    }
    if ($("generate.copy.outer") == "3") {
      oldTransform = true
    }
    logger.logInfo("*****************************************************************")
    logger.logInfo("* basedir = " + $("basedir"))
    logger.logInfo("* dita.dir = " + ditaDir)
    logger.logInfo("* input = " + $("args.input"))
    logger.logInfo("* inputdir = " + $("dita.input.dirname"))
    logger.logInfo("* transtype = " + transtype)
    logger.logInfo("* tempdir = " + $("dita.temp.dir"))
    logger.logInfo("* outputdir = " + $("output.dir"))
    logger.logInfo("* extname = " + $("dita.ext"))
    logger.logInfo("* clean.temp = " + $("clean.temp"))
    logger.logInfo("* DITA-OT version = " + $("otversion"))
    logger.logInfo("* XML parser = " + $("xml.parser"))
    logger.logInfo("* XSLT processor = " + $("xslt.parser"))
    logger.logInfo("* collator = " + $("collator"))
    logger.logInfo("*****************************************************************")
    logger.logInfo("*****************************************************************")
  }

  /**Preprocessing ended */
  def preprocess() {
    logger.logInfo("preprocess:")
    depends(("gen-list", genList), ("debug-filter", debugFilter), ("copy-files", copyFiles), ("conrefpush", conrefpush), ("conref", conref), ("move-meta-entries", moveMetaEntries), ("keyref", keyref), ("coderef", coderef), ("mapref", mapref), ("mappull", mappull), ("chunk", chunk), ("maplink", maplink), ("move-links", moveLinks), ("topicpull", topicpull), ("flag-module", flagModule))
  }

  /**Clean temp directory */
  def cleanTemp() {
    logger.logInfo("clean-temp:")
    if ($.contains("clean-temp.skip")) {
      return
    }

    delete(new File($("dita.temp.dir")), listAll(new File($("dita.temp.dir"))))
  }

  /**Generate file list */
  def genList() {
    logger.logInfo("gen-list:")
    if ($.contains("preprocess.gen-list.skip")) {
      return
    }

    import org.dita.dost.module.GenMapAndTopicListModule
    val module = new org.dita.dost.module.GenMapAndTopicListModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", $("args.input"))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    if ($.contains("args.input.dir")) {
      modulePipelineInput.setAttribute("inputdir", $("args.input.dir"))
    }
    modulePipelineInput.setAttribute("ditadir", ditaDir)
    if ($.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", $("dita.input.valfile"))
    }
    if ($.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", $("dita.ext"))
    }
    modulePipelineInput.setAttribute("validate", $("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", $("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", $("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", $("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", $("output.dir"))
    modulePipelineInput.setAttribute("transtype", transtype)
    modulePipelineInput.setAttribute("gramcache", $("args.grammar.cache"))
    modulePipelineInput.setAttribute("setsystemid", $("args.xml.systemid.set"))
    module.execute(modulePipelineInput)
  }

  /**Debug and filter input files */
  def debugFilter() {
    logger.logInfo("debug-filter:")
    if ($.contains("preprocess.debug-filter.skip")) {
      return
    }

    import org.dita.dost.module.DebugAndFilterModule
    val module = new org.dita.dost.module.DebugAndFilterModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    if ($.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", $("dita.input.valfile"))
    }
    if ($.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", $("dita.ext"))
    }
    modulePipelineInput.setAttribute("ditadir", ditaDir)
    modulePipelineInput.setAttribute("validate", $("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", $("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", $("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", $("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", $("output.dir"))
    modulePipelineInput.setAttribute("transtype", transtype)
    modulePipelineInput.setAttribute("setsystemid", $("args.xml.systemid.set"))
    module.execute(modulePipelineInput)

    job = new Job(new File($("dita.temp.dir")))
    $.readXmlProperties(new File($("dita.temp.dir") + File.separator + "dita.xml.properties"))
    $("dita.map.output.dir") = new File($("output.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    if (job.getSet("conreflist").isEmpty()) {
      noConref = true
    }
    if (job.getSet("fullditamaplist").isEmpty()) {
      noMap = true
    }
    if (job.getSet("imagelist").isEmpty()) {
      noImagelist = true
    }
    if (job.getSet("htmllist").isEmpty()) {
      noHtmllist = true
    }
    if (job.getSet("subtargetslist").isEmpty()) {
      noSublist = true
    }
    if (job.getSet("conrefpushlist").isEmpty()) {
      noConrefPush = true
    }
    if (job.getSet("keyreflist").isEmpty()) {
      noKeyref = true
    }
    if (job.getSet("codereflist").isEmpty()) {
      noCoderef = true
    }
  }

  /**Resolve conref push */
  def conrefpush() {
    logger.logInfo("conrefpush:")
    logger.logInfo("conrefpush-check:")
    if (noConrefPush) {
      $("preprocess.conrefpush.skip") = "true"
    }

    if ($.contains("preprocess.conrefpush.skip")) {
      return
    }

    import org.dita.dost.module.ConrefPushModule
    val module = new org.dita.dost.module.ConrefPushModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Move metadata entries */
  def moveMetaEntries() {
    logger.logInfo("move-meta-entries:")
    logger.logInfo("move-meta-entries-check:")
    if (noMap) {
      $("preprocess.move-meta-entries.skip") = "true"
    }

    if ($.contains("preprocess.move-meta-entries.skip")) {
      return
    }

    import org.dita.dost.module.MoveMetaModule
    val module = new org.dita.dost.module.MoveMetaModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Resolve conref in input files */
  def conref() {
    logger.logInfo("conref:")
    logger.logInfo("conref-check:")
    if (noConref) {
      $("preprocess.conref.skip") = "true"
    }

    if ($.contains("preprocess.conref.skip")) {
      return
    }

    if (!$.contains("dita.preprocess.reloadstylesheet.conref")) {
      $("dita.preprocess.reloadstylesheet.conref") = $("dita.preprocess.reloadstylesheet")
    }
    $("exportfile.url") = new File($("dita.temp.dir") + File.separator + "export.xml").toURI().toASCIIString()
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "conref.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("dita.temp.dir"))
    val tempExt = ".cnrf"
    val files = job.getSet("conreflist")
    var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.conref").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.preprocess.reloadstylesheet.conref").toBoolean) {
        transformer = templates.newTransformer()
      }
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("EXPORTFILE", $("exportfile.url"))
      transformer.setParameter("TRANSTYPE", transtype)
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("file-being-processed", inFile.getName())
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      val dst = new File(baseDir, l)
      logger.logInfo("Moving " + new File(destDir, FileUtils.replaceExtension(l, tempExt)) + " to " + new File(baseDir, l))
      src.renameTo(dst)
    }
  }

  /**Resolve coderef in input files */
  def coderef() {
    logger.logInfo("coderef:")
    logger.logInfo("coderef-check:")
    if (noCoderef) {
      $("preprocess.coderef.skip") = "true"
    }

    if ($.contains("preprocess.coderef.skip")) {
      return
    }

    import org.dita.dost.module.CoderefModule
    val module = new org.dita.dost.module.CoderefModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Resolve mapref in ditamap */
  def mapref() {
    logger.logInfo("mapref:")
    logger.logInfo("mapref-check:")
    if (noMap) {
      $("preprocess.mapref.skip") = "true"
    }

    if ($.contains("preprocess.mapref.skip")) {
      return
    }

    if (!$.contains("dita.preprocess.reloadstylesheet.mapref")) {
      $("dita.preprocess.reloadstylesheet.mapref") = $("dita.preprocess.reloadstylesheet")
    }
    $("mapref.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mapref.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("dita.temp.dir"))
    val tempExt = ".ditamap.ref"
    val files = job.getSet("fullditamaplist")
    var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.mapref").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.preprocess.reloadstylesheet.mapref").toBoolean) {
        transformer = templates.newTransformer()
      }
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", transtype)
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("file-being-processed", inFile.getName())
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      val dst = new File(baseDir, l)
      logger.logInfo("Moving " + new File(destDir, FileUtils.replaceExtension(l, tempExt)) + " to " + new File(baseDir, l))
      src.renameTo(dst)
    }
  }

  /**Resolve keyref */
  def keyref() {
    logger.logInfo("keyref:")
    logger.logInfo("keyref-check:")
    if (noKeyref) {
      $("preprocess.keyref.skip") = "true"
    }

    if ($.contains("preprocess.keyref.skip")) {
      return
    }

    import org.dita.dost.module.KeyrefModule
    val module = new org.dita.dost.module.KeyrefModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    if ($.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", $("dita.ext"))
    }
    module.execute(modulePipelineInput)
  }

  /**Pull the navtitle and topicmeta from topics to ditamap */
  def mappull() {
    logger.logInfo("mappull:")
    logger.logInfo("mappull-check:")
    if (noMap) {
      $("preprocess.mappull.skip") = "true"
    }

    if ($.contains("preprocess.mappull.skip")) {
      return
    }

    $("mappull.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    if (!$.contains("dita.preprocess.reloadstylesheet.mappull")) {
      $("dita.preprocess.reloadstylesheet.mappull") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mappull.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("dita.temp.dir"))
    val tempExt = ".ditamap.pull"
    val files = job.getSet("fullditamaplist")
    var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.mappull").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.preprocess.reloadstylesheet.mappull").toBoolean) {
        transformer = templates.newTransformer()
      }
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", transtype)
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      val dst = new File(baseDir, l)
      logger.logInfo("Moving " + new File(destDir, FileUtils.replaceExtension(l, tempExt)) + " to " + new File(baseDir, l))
      src.renameTo(dst)
    }
  }

  /**Process chunks */
  def chunk() {
    logger.logInfo("chunk:")
    logger.logInfo("chunk-check:")
    if (noMap) {
      $("preprocess.chunk.skip") = "true"
    }

    if ($.contains("preprocess.chunk.skip")) {
      return
    }

    import org.dita.dost.module.ChunkModule
    val module = new org.dita.dost.module.ChunkModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    if ($.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", $("dita.ext"))
    }
    modulePipelineInput.setAttribute("transtype", transtype)
    module.execute(modulePipelineInput)

    job = new Job(new File($("dita.temp.dir")))
    $.readXmlProperties(new File($("dita.temp.dir") + File.separator + "dita.xml.properties"))
    if (job.getSet("fullditatopiclist").isEmpty()) {
      noTopic = true
    }
  }

  /**Find and generate related link information */
  def maplink() {
    logger.logInfo("maplink:")
    logger.logInfo("maplink-check:")
    if (noMap) {
      $("preprocess.maplink.skip") = "true"
    }

    if ($.contains("preprocess.maplink.skip")) {
      return
    }

    $("maplink.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    if (!$.contains("dita.preprocess.reloadstylesheet.maplink")) {
      $("dita.preprocess.reloadstylesheet.maplink") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "maplink.xsl"))
    val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val outFile = new File($("maplink.workdir") + File.separator + "maplinks.unordered")
    if (!outFile.getParentFile().exists()) {
      outFile.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("dita.ext")) {
      transformer.setParameter("DITAEXT", $("dita.ext"))
    }
    transformer.setParameter("INPUTMAP", job.getProperty(INPUT_DITAMAP))
    if ($.contains("include.rellinks")) {
      transformer.setParameter("include.rellinks", $("include.rellinks"))
    }
    val source = getSource(inFile)
    val result = new StreamResult(outFile)
    logger.logInfo("Processing " + inFile + " to " + outFile)
    transformer.transform(source, result)
  }

  /**Move the related link information to topics */
  def moveLinks() {
    logger.logInfo("move-links:")
    logger.logInfo("move-links-check:")
    if (noMap) {
      $("preprocess.move-links.skip") = "true"
    }

    if ($.contains("preprocess.move-links.skip")) {
      return
    }

    import org.dita.dost.module.MoveLinksModule
    val module = new org.dita.dost.module.MoveLinksModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("maplinks", $("maplink.workdir") + "/maplinks.unordered")
    module.execute(modulePipelineInput)
  }

  /**Pull metadata for link and xref element */
  def topicpull() {
    logger.logInfo("topicpull:")
    logger.logInfo("topicpull-check:")
    if (noTopic) {
      $("preprocess.topicpull.skip") = "true"
    }

    if ($.contains("preprocess.topicpull.skip")) {
      return
    }

    if (!$.contains("dita.preprocess.reloadstylesheet.topicpull")) {
      $("dita.preprocess.reloadstylesheet.topicpull") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "topicpull.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("dita.temp.dir"))
    val tempExt = ".pull"
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist")
    var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.topicpull").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.preprocess.reloadstylesheet.topicpull").toBoolean) {
        transformer = templates.newTransformer()
      }
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      if ($.contains("args.tablelink.style")) {
        transformer.setParameter("TABLELINK", $("args.tablelink.style"))
      }
      if ($.contains("args.figurelink.style")) {
        transformer.setParameter("FIGURELINK", $("args.figurelink.style"))
      }
      if ($.contains("onlytopic.in.map")) {
        transformer.setParameter("ONLYTOPICINMAP", $("onlytopic.in.map"))
      }
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      val dst = new File(baseDir, l)
      logger.logInfo("Moving " + new File(destDir, FileUtils.replaceExtension(l, tempExt)) + " to " + new File(baseDir, l))
      src.renameTo(dst)
    }
  }

  /**Add flagging information to topics */
  def flagModule() {
    logger.logInfo("flag-module:")
    logger.logInfo("flag-module-check:")
    if ((noTopic || !$.contains("args.filter"))) {
      $("preprocess.flagging.skip") = "true"
    }

    if ($.contains("preprocess.flagging.skip")) {
      return
    }

    $("dita.input.filterfile.url") = new File($("args.filter")).toURI().toASCIIString()
    if (!$.contains("dita.preprocess.reloadstylesheet.flag-module")) {
      $("dita.preprocess.reloadstylesheet.flag-module") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "flag.xsl"))
    val baseDir = new File($("dita.temp.dir"))
    val destDir = new File($("dita.temp.dir"))
    val tempExt = ".flag"
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist") -- job.getSet("resourceonlylist")
    var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.flag-module").toBoolean) templates.newTransformer() else null
    for (l <- files) {
      if ($("dita.preprocess.reloadstylesheet.flag-module").toBoolean) {
        transformer = templates.newTransformer()
      }
      transformer.setParameter("TRANSTYPE", transtype)
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("FILTERFILEURL", $("dita.input.filterfile.url"))
      if ($.contains("args.draft")) {
        transformer.setParameter("DRAFT", $("args.draft"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("OUTPUTDIR", $("output.dir"))
      if ($.contains("args.debug")) {
        transformer.setParameter("DBG", $("args.debug"))
      }
      val inFile = new File(baseDir, l)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      transformer.setParameter("FILENAME", inFile.getName())
      transformer.setParameter("FILEDIR", inFile.getParent())
      if (!outFile.getParentFile().exists()) {
        outFile.getParentFile().mkdirs()
      }
      val source = getSource(inFile)
      val result = new StreamResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      val dst = new File(baseDir, l)
      logger.logInfo("Moving " + new File(destDir, FileUtils.replaceExtension(l, tempExt)) + " to " + new File(baseDir, l))
      src.renameTo(dst)
    }
  }

  def copyFiles() {
    logger.logInfo("copy-files:")
    depends(("copy-image", copyImage), ("copy-html", copyHtml), ("copy-flag", copyFlag), ("copy-subsidiary", copySubsidiary), ("copy-generated-files", copyGeneratedFiles))
    if ($.contains("preprocess.copy-files.skip")) {
      return
    }

  }

  /**Copy image files */
  def copyImageUplevels() {
    logger.logInfo("copy-image-uplevels:")
    logger.logInfo("copy-image-check:")
    if (($.contains("preprocess.copy-files.skip") || noImagelist)) {
      $("preprocess.copy-image.skip") = "true"
    }
    if ($("generate.copy.outer") != "3") {
      $("image.copy.uplevels") = "true"
    }
    if (($("generate.copy.outer") == "3")) {
      $("image.copy.normal") = "true"
    }

    if (!$.contains("image.copy.uplevels")) {
      return
    }
    if ($.contains("preprocess.copy-image.skip")) {
      return
    }

    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir") + File.separator + $("uplevels")), job.getSet("imagelist"))
  }

  /**Copy image files */
  def copyImageNoraml() {
    logger.logInfo("copy-image-noraml:")
    logger.logInfo("copy-image-check:")
    if (($.contains("preprocess.copy-files.skip") || noImagelist)) {
      $("preprocess.copy-image.skip") = "true"
    }
    if ($("generate.copy.outer") != "3") {
      $("image.copy.uplevels") = "true"
    }
    if (($("generate.copy.outer") == "3")) {
      $("image.copy.normal") = "true"
    }

    if (!$.contains("image.copy.normal")) {
      return
    }
    if ($.contains("preprocess.copy-image.skip")) {
      return
    }

    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), job.getSet("imagelist"))
  }

  /**Copy image files */
  def copyImage() {
    logger.logInfo("copy-image:")
    depends(("copy-image-uplevels", copyImageUplevels), ("copy-image-noraml", copyImageNoraml))
  }

  /**Copy html files */
  def copyHtml() {
    logger.logInfo("copy-html:")
    logger.logInfo("copy-html-check:")
    if (($.contains("preprocess.copy-files.skip") || noHtmllist)) {
      $("preprocess.copy-html.skip") = "true"
    }

    if ($.contains("preprocess.copy-html.skip")) {
      return
    }

    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), job.getSet("htmllist"))
  }

  /**Copy flag files */
  def copyFlag() {
    logger.logInfo("copy-flag:")
    logger.logInfo("copy-flag-check:")
    if (($.contains("preprocess.copy-files.skip") || !$.contains("dita.input.valfile"))) {
      $("preprocess.copy-flag.skip") = "true"
    }

    if ($.contains("preprocess.copy-flag.skip")) {
      return
    }

    ditaOtCopy(new File($("output.dir")), job.getSet("flagimagelist"), job.getSet("relflagimagelist"))
  }

  /**Copy subsidiary files */
  def copySubsidiary() {
    logger.logInfo("copy-subsidiary:")
    logger.logInfo("copy-subsidiary-check:")
    if (($.contains("preprocess.copy-files.skip") || noSublist)) {
      $("preprocess.copy-subsidiary.skip") = "true"
    }

    if ($.contains("preprocess.copy-subsidiary.skip")) {
      return
    }

    copy(new File(job.getProperty(INPUT_DIR)), new File($("dita.temp.dir")), job.getSet("subtargetslist"))
  }

  /**Copy generated files */
  def copyGeneratedFiles() {
    logger.logInfo("copy-generated-files:")
    if ($.contains("preprocess.copy-generated-files.skip")) {
      return
    }

    copy(new File($("dita.temp.dir")), new File($("args.logdir")), Set("dita.list", "property.temp", "dita.xml.properties"))
  }
}
