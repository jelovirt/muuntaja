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
  $("ant.file.DOST.dir") = new File($("ant.file.DOST")).getParent()
  if (!$.contains("dita.dir")) {
    $("dita.dir") = $("ant.file.DOST.dir")
  }
  if (!$.contains("dita.dir")) {
    $("dita.dir") = $("basedir")
  }
  $("dita.plugin.org.dita.troff.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.troff")
  $("dita.plugin.org.dita.eclipsecontent.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.eclipsecontent")
  $("dita.plugin.org.dita.eclipsehelp.dir") = new File($("dita.dir"))
  $("dita.plugin.org.dita.specialization.dita11.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.dita11")
  $("dita.plugin.org.dita.xhtml.dir") = new File($("dita.dir"))
  $("dita.plugin.org.dita.odt.dir") = new File($("dita.dir"))
  $("dita.plugin.net.sourceforge.dita-ot.html.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "net.sourceforge.dita-ot.html")
  $("dita.plugin.org.dita.pdf2.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.pdf2")
  $("dita.plugin.org.dita.specialization.dita132.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.dita132")
  $("dita.plugin.com.sophos.tocjs.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "com.sophos.tocjs")
  $("dita.plugin.org.dita.wordrtf.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.wordrtf")
  $("dita.plugin.org.dita.docbook.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.docbook")
  $("dita.plugin.org.dita.specialization.eclipsemap.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.eclipsemap")
  $("dita.plugin.org.dita.base.dir") = new File($("dita.dir"))
  $("dita.plugin.org.dita.htmlhelp.dir") = new File($("dita.dir"))
  $("dita.plugin.org.dita.pdf.dir") = new File($("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.pdf")
  $("dita.plugin.org.dita.javahelp.dir") = new File($("dita.dir"))
  $("maxJavaMemory") = "500m"
  $.readProperties(new File($("dita.dir") + File.separator + "lib" + File.separator + "org.dita.dost.platform" + File.separator + "plugin.properties"))
  $.readProperties(new File($("dita.dir") + File.separator + "lib" + File.separator + "configuration.properties"))
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
  $("dita.script.dir") = new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl")
  $("dita.resource.dir") = new File($("dita.plugin.org.dita.base.dir") + File.separator + "resource")
  $("dita.empty") = ""
  $("args.message.file") = new File($("dita.plugin.org.dita.base.dir") + File.separator + "resource" + File.separator + "messages.xml")
  if (!$.contains("dita.preprocess.reloadstylesheet")) {
    $("dita.preprocess.reloadstylesheet") = "false"
  }

  def buildInit() {
    depends(("start-process", startProcess), ("init-logger", initLogger), ("init-URIResolver", initURIResolver), ("use-init", useInit), ("check-arg", checkArg), ("output-msg", outputMsg))
    logger.logInfo("build-init:")
  }

  /**Processing started */
  def startProcess() {
    logger.logInfo("start-process:")
  }

  /**Initialize log directory and file name */
  def initLogger() {
    logger.logInfo("init-logger:")
    //TODO config_logger()
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
    depends(("use-init", useInit))
    logger.logInfo("check-arg:")
    if (($.contains("args.input") && !(new File($("args.input")).exists()))) {
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
    $("dita.input.dirname") = new File($("args.input")).getParent()
    $("dita.map.filename.root") = new File($("dita.input.filename")).getName()
    $("dita.topic.filename.root") = new File($("dita.input.filename")).getName()
    if (!new File($("output.dir")).exists()) {
      new File($("output.dir")).mkdirs()
    }
    if (!new File($("dita.temp.dir")).exists()) {
      new File($("dita.temp.dir")).mkdirs()
    }
    if (($.contains("args.csspath") && ($("args.csspath").indexOf("http://") != -1 || $("args.csspath").indexOf("https://") != -1))) {
      $("user.csspath.url") = "true"
    }
    if (($.contains("args.csspath") && new File($("args.csspath")).isAbsolute)) {
      $("args.csspath.absolute") = "true"
    }
    if ((!$.contains("args.csspath") || $.contains("args.csspath.absolute"))) {
      $("user.csspath") = ""
    }
    if (!$.contains("user.csspath")) {
      $("user.csspath") = $("args.csspath") + "/"
    }
    if (($.contains("args.cssroot") && $.contains("args.css"))) {
      $("args.css.real") = $("args.cssroot") + $("file.separator") + $("args.css")
    }
    if ((!$.contains("args.cssroot") && $.contains("args.css"))) {
      $("args.css.real") = $("args.css")
    }
    if (($.contains("args.css.real") && new File($("args.css.real")).exists())) {
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
    logger.logInfo("* dita.dir = " + $("dita.dir"))
    logger.logInfo("* input = " + $("args.input"))
    logger.logInfo("* transtype = " + $("transtype"))
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

  def outputMsg() {
    depends(("output-css-warn-message", outputCssWarnMessage))
    logger.logInfo("output-msg:")
  }

  def outputCssWarnMessage() {
    if (!$.contains("args.csspath.absolute")) {
      return
    }

    logger.logInfo("output-css-warn-message:")
    logger.logInfo(get_msg("DOTA006W"))
  }

  /**Preprocessing ended */
  def preprocess() {
    depends(("gen-list", genList), ("debug-filter", debugFilter), ("copy-files", copyFiles), ("conrefpush", conrefpush), ("conref", conref), ("move-meta-entries", moveMetaEntries), ("keyref", keyref), ("coderef", coderef), ("mapref", mapref), ("mappull", mappull), ("chunk", chunk), ("maplink", maplink), ("move-links", moveLinks), ("topicpull", topicpull))
    logger.logInfo("preprocess:")
  }

  /**Clean temp directory */
  def cleanTemp() {
    if ($.contains("clean-temp.skip")) {
      return
    }

    logger.logInfo("clean-temp:")
    delete(new File($("dita.temp.dir")), listAll(new File($("dita.temp.dir"))))
  }

  /**Generate file list */
  def genList() {
    if ($.contains("preprocess.gen-list.skip")) {
      return
    }

    logger.logInfo("gen-list:")
    import org.dita.dost.module.GenMapAndTopicListModule
    val module = new org.dita.dost.module.GenMapAndTopicListModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", $("args.input"))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    modulePipelineInput.setAttribute("ditadir", $("dita.dir"))
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
    modulePipelineInput.setAttribute("transtype", $("transtype"))
    modulePipelineInput.setAttribute("gramcache", $("args.grammar.cache"))
    modulePipelineInput.setAttribute("setsystemid", $("args.xml.systemid.set"))
    module.execute(modulePipelineInput)
  }

  /**Debug and filter input files */
  def debugFilter() {
    depends(("gen-list", genList))
    if ($.contains("preprocess.debug-filter.skip")) {
      return
    }

    logger.logInfo("debug-filter:")
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
    modulePipelineInput.setAttribute("ditadir", $("dita.dir"))
    modulePipelineInput.setAttribute("validate", $("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", $("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", $("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", $("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", $("output.dir"))
    modulePipelineInput.setAttribute("transtype", $("transtype"))
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
    depends(("debug-filter", debugFilter))
    logger.logInfo("conrefpush-check:")
    if (noConrefPush) {
      $("preprocess.conrefpush.skip") = "true"
    }

    if ($.contains("preprocess.conrefpush.skip")) {
      return
    }

    logger.logInfo("conrefpush:")
    import org.dita.dost.module.ConrefPushModule
    val module = new org.dita.dost.module.ConrefPushModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Move metadata entries */
  def moveMetaEntries() {
    depends(("debug-filter", debugFilter))
    logger.logInfo("move-meta-entries-check:")
    if (noMap) {
      $("preprocess.move-meta-entries.skip") = "true"
    }

    if ($.contains("preprocess.move-meta-entries.skip")) {
      return
    }

    logger.logInfo("move-meta-entries:")
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
    depends(("debug-filter", debugFilter), ("conrefpush", conrefpush))
    logger.logInfo("conref-check:")
    if (noConref) {
      $("preprocess.conref.skip") = "true"
    }

    if ($.contains("preprocess.conref.skip")) {
      return
    }

    logger.logInfo("conref:")
    if (!$.contains("dita.preprocess.reloadstylesheet.conref")) {
      $("dita.preprocess.reloadstylesheet.conref") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "conref.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("dita.temp.dir"))
    val temp_ext = ".cnrf"
    val files = job.getSet("conreflist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("BASEDIR", $("basedir"))
      transformer.setParameter("TEMPDIR", $("dita.temp.dir"))
      transformer.setParameter("TRANSTYPE", $("transtype"))
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("file-being-processed", in_file.getName())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      val dst = new File(base_dir, l)
      logger.logInfo("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
      src.renameTo(dst)
    }
  }

  /**Resolve coderef in input files */
  def coderef() {
    depends(("debug-filter", debugFilter), ("keyref", keyref))
    logger.logInfo("coderef-check:")
    if (noCoderef) {
      $("preprocess.coderef.skip") = "true"
    }

    if ($.contains("preprocess.coderef.skip")) {
      return
    }

    logger.logInfo("coderef:")
    import org.dita.dost.module.CoderefModule
    val module = new org.dita.dost.module.CoderefModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Resolve mapref in ditamap */
  def mapref() {
    depends(("coderef", coderef))
    logger.logInfo("mapref-check:")
    if (noMap) {
      $("preprocess.mapref.skip") = "true"
    }

    if ($.contains("preprocess.mapref.skip")) {
      return
    }

    logger.logInfo("mapref:")
    if (!$.contains("dita.preprocess.reloadstylesheet.mapref")) {
      $("dita.preprocess.reloadstylesheet.mapref") = $("dita.preprocess.reloadstylesheet")
    }
    $("mapref.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mapref.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("dita.temp.dir"))
    val temp_ext = ".ditamap.ref"
    val files = job.getSet("fullditamaplist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", $("transtype"))
      transformer.setParameter("FILEREF", "file:")
      val in_file = new File(base_dir, l)
      val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      transformer.setParameter("file-being-processed", in_file.getName())
      if (!out_file.getParentFile().exists()) {
        out_file.getParentFile().mkdirs()
      }
      val source = getSource(in_file)
      val result = new StreamResult(out_file)
      logger.logInfo("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)
    }
    for (l <- files) {
      val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      val dst = new File(base_dir, l)
      logger.logInfo("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
      src.renameTo(dst)
    }
  }

  /**Resolve keyref */
  def keyref() {
    depends(("move-meta-entries", moveMetaEntries))
    logger.logInfo("keyref-check:")
    if (noKeyref) {
      $("preprocess.keyref.skip") = "true"
    }

    if ($.contains("preprocess.keyref.skip")) {
      return
    }

    logger.logInfo("keyref:")
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
    depends(("mapref", mapref))
    logger.logInfo("mappull-check:")
    if (noMap) {
      $("preprocess.mappull.skip") = "true"
    }

    if ($.contains("preprocess.mappull.skip")) {
      return
    }

    logger.logInfo("mappull:")
    $("mappull.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    if (!$.contains("dita.preprocess.reloadstylesheet.mappull")) {
      $("dita.preprocess.reloadstylesheet.mappull") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mappull.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("dita.temp.dir"))
    val temp_ext = ".ditamap.pull"
    val files = job.getSet("fullditamaplist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", $("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", $("transtype"))
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
    for (l <- files) {
      val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      val dst = new File(base_dir, l)
      logger.logInfo("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
      src.renameTo(dst)
    }
  }

  /**Process chunks */
  def chunk() {
    depends(("mappull", mappull))
    logger.logInfo("chunk-check:")
    if (noMap) {
      $("preprocess.chunk.skip") = "true"
    }

    if ($.contains("preprocess.chunk.skip")) {
      return
    }

    logger.logInfo("chunk:")
    import org.dita.dost.module.ChunkModule
    val module = new org.dita.dost.module.ChunkModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", job.getProperty(INPUT_DITAMAP))
    modulePipelineInput.setAttribute("tempDir", $("dita.temp.dir"))
    if ($.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", $("dita.ext"))
    }
    modulePipelineInput.setAttribute("transtype", $("transtype"))
    module.execute(modulePipelineInput)

    job = new Job(new File($("dita.temp.dir")))
    $.readXmlProperties(new File($("dita.temp.dir") + File.separator + "dita.xml.properties"))
    if (job.getSet("fullditatopiclist").isEmpty()) {
      noTopic = true
    }
  }

  /**Find and generate related link information */
  def maplink() {
    depends(("chunk", chunk))
    logger.logInfo("maplink-check:")
    if (noMap) {
      $("preprocess.maplink.skip") = "true"
    }

    if ($.contains("preprocess.maplink.skip")) {
      return
    }

    logger.logInfo("maplink:")
    $("maplink.workdir") = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP)).getParent()
    if (!$.contains("dita.preprocess.reloadstylesheet.maplink")) {
      $("dita.preprocess.reloadstylesheet.maplink") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "maplink.xsl"))
    val in_file = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
    val out_file = new File($("maplink.workdir") + File.separator + "maplinks.unordered")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if ($.contains("dita.ext")) {
      transformer.setParameter("DITAEXT", $("dita.ext"))
    }
    transformer.setParameter("INPUTMAP", job.getProperty(INPUT_DITAMAP))
    if ($.contains("include.rellinks")) {
      transformer.setParameter("include.rellinks", $("include.rellinks"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Move the related link information to topics */
  def moveLinks() {
    depends(("maplink", maplink))
    logger.logInfo("move-links-check:")
    if (noMap) {
      $("preprocess.move-links.skip") = "true"
    }

    if ($.contains("preprocess.move-links.skip")) {
      return
    }

    logger.logInfo("move-links:")
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
    depends(("debug-filter", debugFilter))
    logger.logInfo("topicpull-check:")
    if (noTopic) {
      $("preprocess.topicpull.skip") = "true"
    }

    if ($.contains("preprocess.topicpull.skip")) {
      return
    }

    logger.logInfo("topicpull:")
    if (!$.contains("dita.preprocess.reloadstylesheet.topicpull")) {
      $("dita.preprocess.reloadstylesheet.topicpull") = $("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "topicpull.xsl"))
    val base_dir = new File($("dita.temp.dir"))
    val dest_dir = new File($("dita.temp.dir"))
    val temp_ext = ".pull"
    val files = job.getSet("fullditatopiclist") ++ job.getSet("chunkedtopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
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
    for (l <- files) {
      val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
      val dst = new File(base_dir, l)
      logger.logInfo("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
      src.renameTo(dst)
    }
  }

  def copyFiles() {
    depends(("debug-filter", debugFilter), ("copy-image", copyImage), ("copy-html", copyHtml), ("copy-flag", copyFlag), ("copy-subsidiary", copySubsidiary), ("copy-generated-files", copyGeneratedFiles))
    if ($.contains("preprocess.copy-files.skip")) {
      return
    }

    logger.logInfo("copy-files:")
  }

  /**Copy image files */
  def copyImageUplevels() {
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

    logger.logInfo("copy-image-uplevels:")
    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir") + File.separator + $("uplevels")), job.getSet("imagelist"))
  }

  /**Copy image files */
  def copyImageNoraml() {
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

    logger.logInfo("copy-image-noraml:")
    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), job.getSet("imagelist"))
  }

  /**Copy image files */
  def copyImage() {
    depends(("copy-image-uplevels", copyImageUplevels), ("copy-image-noraml", copyImageNoraml))
    logger.logInfo("copy-image:")
  }

  /**Copy html files */
  def copyHtml() {
    logger.logInfo("copy-html-check:")
    if (($.contains("preprocess.copy-files.skip") || noHtmllist)) {
      $("preprocess.copy-html.skip") = "true"
    }

    if ($.contains("preprocess.copy-html.skip")) {
      return
    }

    logger.logInfo("copy-html:")
    copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), job.getSet("htmllist"))
  }

  /**Copy flag files */
  def copyFlag() {
    logger.logInfo("copy-flag-check:")
    if (($.contains("preprocess.copy-files.skip") || !$.contains("dita.input.valfile"))) {
      $("preprocess.copy-flag.skip") = "true"
    }

    if ($.contains("preprocess.copy-flag.skip")) {
      return
    }

    logger.logInfo("copy-flag:")
    ditaOtCopy(new File($("output.dir")), job.getSet("flagimagelist"), job.getSet("relflagimagelist"))
  }

  /**Copy subsidiary files */
  def copySubsidiary() {
    logger.logInfo("copy-subsidiary-check:")
    if (($.contains("preprocess.copy-files.skip") || noSublist)) {
      $("preprocess.copy-subsidiary.skip") = "true"
    }

    if ($.contains("preprocess.copy-subsidiary.skip")) {
      return
    }

    logger.logInfo("copy-subsidiary:")
    copy(new File(job.getProperty(INPUT_DIR)), new File($("dita.temp.dir")), job.getSet("subtargetslist"))
  }

  /**Copy generated files */
  def copyGeneratedFiles() {
    if ($.contains("preprocess.copy-generated-files.skip")) {
      return
    }

    logger.logInfo("copy-generated-files:")
    copy(new File($("dita.temp.dir")), new File($("args.logdir")), Set("dita.list", "property.temp", "dita.xml.properties"))
  }
}
