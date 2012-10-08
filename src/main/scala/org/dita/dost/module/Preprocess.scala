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
import org.dita.dost.util.Job

abstract class Preprocess(ditaDir: File) extends Transtype(ditaDir) {

  Properties("ant.file.ditaot-preprocess") = new File("")
  Properties.readProperties(new File(Properties("basedir") + File.separator + "local.properties"))
  Properties("ant.file.DOST.dir") = new File(Properties("ant.file.DOST")).getParent()
  if (!Properties.contains("dita.dir")) {
    Properties("dita.dir") = Properties("ant.file.DOST.dir")
  }
  if (!Properties.contains("dita.dir")) {
    Properties("dita.dir") = Properties("basedir")
  }
  Properties("dita.plugin.org.dita.troff.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.troff")
  Properties("dita.plugin.org.dita.eclipsecontent.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.eclipsecontent")
  Properties("dita.plugin.org.dita.eclipsehelp.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.specialization.dita11.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.dita11")
  Properties("dita.plugin.org.dita.xhtml.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.odt.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.net.sourceforge.dita-ot.html.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "net.sourceforge.dita-ot.html")
  Properties("dita.plugin.org.dita.pdf2.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.pdf2")
  Properties("dita.plugin.org.dita.specialization.dita132.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.dita132")
  Properties("dita.plugin.com.sophos.tocjs.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "com.sophos.tocjs")
  Properties("dita.plugin.org.dita.wordrtf.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.wordrtf")
  Properties("dita.plugin.org.dita.docbook.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.docbook")
  Properties("dita.plugin.org.dita.specialization.eclipsemap.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.specialization.eclipsemap")
  Properties("dita.plugin.org.dita.base.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.htmlhelp.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.pdf.dir") = new File(Properties("dita.dir") + File.separator + "plugins" + File.separator + "org.dita.pdf")
  Properties("dita.plugin.org.dita.javahelp.dir") = new File(Properties("dita.dir"))
  Properties("maxJavaMemory") = "500m"
  Properties.readProperties(new File(Properties("dita.dir") + File.separator + "lib" + File.separator + "org.dita.dost.platform" + File.separator + "plugin.properties"))
  Properties.readProperties(new File(Properties("dita.dir") + File.separator + "lib" + File.separator + "configuration.properties"))
  if (((System.getProperty("os.name") == "x86_64" || System.getProperty("os.name") == "amd64" || System.getProperty("os.name") == "ppc64") && !(System.getProperty("os.name") == "windows"))) {
    Properties("is64bit") = "true"
  }
  if (Properties("is64bit") != "true") {
    Properties("is32bit") = "true"
  }
  if (Properties("is64bit") == "true") {
    Properties("jvmArchFlag") = "-d64"
  } else {
    Properties("jvmArchFlag") = ""
  }
  Properties("baseJVMArgLine") = Properties("jvmArchFlag") + " -Xmx" + Properties("maxJavaMemory")
  Properties("current.date") = "20120130"
  Properties("base.temp.dir") = new File(Properties("basedir") + File.separator + "temp")
  Properties("dita.temp.dir") = new File(Properties("base.temp.dir") + File.separator + "temp" + Properties("current.date"))
  Properties("output.dir") = new File(Properties("basedir") + File.separator + "out")
  Properties("dita.script.dir") = new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl")
  Properties("dita.resource.dir") = new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "resource")
  Properties("dita.empty") = ""
  Properties("args.message.file") = new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "resource" + File.separator + "messages.xml")
  if (!Properties.contains("dita.preprocess.reloadstylesheet")) {
    Properties("dita.preprocess.reloadstylesheet") = "false"
  }

  def buildInit() {
    logger.logInfo("\nbuild-init:")
    History.depends(("start-process", startProcess), ("init-logger", initLogger), ("init-URIResolver", initURIResolver), ("use-init", useInit), ("check-arg", checkArg), ("output-msg", outputMsg))
  }

  /**Processing started */
  def startProcess() {
    logger.logInfo("\nstart-process:")
  }

  /**Initialize log directory and file name */
  def initLogger() {
    logger.logInfo("\ninit-logger:")
    //TODO config_logger()
  }

  def initURIResolver() {
    logger.logInfo("\ninit-URIResolver:")
    var path = new File(Properties("dita.temp.dir"))
    DitaURIResolverFactory.setPath(path.getAbsolutePath)
  }

  def useInit() {
    logger.logInfo("\nuse-init:")
    if ((Properties.contains("org.xml.sax.driver") && !Properties.contains("xml.parser"))) {
      Properties("xml.parser") = "XMLReader " + Properties("org.xml.sax.driver")
    }
    if ((class_available("org.apache.xerces.parsers.SAXParser") && !Properties.contains("xml.parser"))) {
      Properties("xml.parser") = "Xerces"
    }
    if ((class_available("com.sun.org.apache.xerces.internal.parsers.SAXParser") && !Properties.contains("xml.parser"))) {
      Properties("xml.parser") = "Xerces in Sun JDK 1.5"
    }
    if ((class_available("org.apache.crimson.parser.XMLReaderImpl") && !Properties.contains("xml.parser"))) {
      Properties("xml.parser") = "Crimson"
    }
  }

  /**Validate and init input arguments */
  def checkArg() {
    logger.logInfo("\ncheck-arg:")
    History.depends(("use-init", useInit))
    if ((Properties.contains("args.input") && !(new File(Properties("args.input")).exists()))) {
      logger.logError("DOTA069F")
      sys.exit()
    }
    if ((!Properties.contains("args.input"))) {
      logger.logError("DOTA002F")
      sys.exit()
    }
    if ((Properties.contains("args.xsl") && !(new File(Properties("args.xsl")).exists()))) {
      logger.logError("DOTA003F")
      sys.exit()
    }
    if ((Properties.contains("args.ftr") && !(new File(Properties("args.ftr")).exists()))) {
      logger.logError("DOTA007E")
      sys.exit()
    }
    if ((Properties.contains("args.hdr") && !(new File(Properties("args.hdr")).exists()))) {
      logger.logError("DOTA008E")
      sys.exit()
    }
    if ((Properties.contains("args.hdf") && !(new File(Properties("args.hdf")).exists()))) {
      logger.logError("DOTA009E")
      sys.exit()
    }
    if (Properties.contains("dita.input.valfile")) {
      logger.logError("DOTA012W")
      sys.exit()
    }
    if ((Properties.contains("args.filter") && !Properties.contains("dita.input.valfile"))) {
      Properties("dita.input.valfile") = Properties("args.filter")
    }
    if ((Properties.contains("args.outext") && !(Properties("args.outext").indexOf(".") != -1))) {
      Properties("out.ext") = "." + Properties("args.outext")
    }
    if ((Properties.contains("args.outext") && Properties("args.outext").indexOf(".") != -1)) {
      Properties("out.ext") = Properties("args.outext")
    }
    if (!Properties.contains("args.grammar.cache")) {
      Properties("args.grammar.cache") = "yes"
    }
    if (!Properties.contains("args.xml.systemid.set")) {
      Properties("args.xml.systemid.set") = "yes"
    }
    Properties("dita.input.filename") = new File(Properties("args.input")).getName()
    Properties("dita.input.dirname") = new File(Properties("args.input")).getParent()
    Properties("dita.map.filename.root") = new File(Properties("dita.input.filename")).getName()
    Properties("dita.topic.filename.root") = new File(Properties("dita.input.filename")).getName()
    if (!new File(Properties("output.dir")).exists()) {
      new File(Properties("output.dir")).mkdirs()
    }
    if (!new File(Properties("dita.temp.dir")).exists()) {
      new File(Properties("dita.temp.dir")).mkdirs()
    }
    if ((Properties("args.csspath").indexOf("http://") != -1 || Properties("args.csspath").indexOf("https://") != -1)) {
      Properties("user.csspath.url") = "true"
    }
    if (new File(Properties("args.csspath")).isAbsolute) {
      Properties("args.csspath.absolute") = "true"
    }
    if ((!Properties.contains("args.csspath") || Properties.contains("args.csspath.absolute"))) {
      Properties("user.csspath") = ""
    }
    if (!Properties.contains("user.csspath")) {
      Properties("user.csspath") = Properties("args.csspath") + "/"
    }
    if (Properties.contains("args.cssroot")) {
      Properties("args.css.real") = Properties("args.cssroot") + Properties("file.separator") + Properties("args.css")
    }
    if (!Properties.contains("args.cssroot")) {
      Properties("args.css.real") = Properties("args.css")
    }
    if (new File(Properties("args.css.real")).exists() && new File(Properties("args.css.real")).isFile()) {
      Properties("args.css.present") = "true"
    }
    Properties("args.css.file.temp") = new File(Properties("args.css")).getName()
    if ((Properties.contains("args.css.present") || Properties.contains("user.csspath.url"))) {
      Properties("args.css.file") = Properties("args.css.file.temp")
    }
    if (!Properties.contains("args.logdir")) {
      Properties("args.logdir") = Properties("output.dir")
    }
    if ((class_available("net.sf.saxon.StyleSheet") || class_available("net.sf.saxon.Transform"))) {
      Properties("xslt.parser") = "Saxon"
    } else {
      Properties("xslt.parser") = "Xalan"
    }
    if (class_available("com.ibm.icu.text.Collator")) {
      Properties("collator") = "ICU"
    } else {
      Properties("collator") = "JDL"
    }
    if (!Properties.contains("validate")) {
      Properties("validate") = "true"
    }
    if (Properties("args.rellinks") == "none") {
      Properties("include.rellinks") = ""
    }
    if (Properties("args.rellinks") == "nofamily") {
      Properties("include.rellinks") = "#default friend sample external other"
    }
    if ((Properties("args.rellinks") == "all" || !Properties.contains("args.rellinks"))) {
      Properties("include.rellinks") = "#default parent child sibling friend next previous cousin ancestor descendant sample external other"
    }
    if (!Properties.contains("generate.copy.outer")) {
      Properties("generate.copy.outer") = "1"
    }
    if (!Properties.contains("onlytopic.in.map")) {
      Properties("onlytopic.in.map") = "false"
    }
    if (!Properties.contains("outer.control")) {
      Properties("outer.control") = "warn"
    }
    if ((Properties("generate.copy.outer") == "1" || Properties("generate.copy.outer") == "2")) {
      Properties("inner.transform") = "true"
    }
    if (Properties("generate.copy.outer") == "3") {
      Properties("old.transform") = "true"
    }
    logger.logInfo("*****************************************************************")
    logger.logInfo("* basedir = " + Properties("basedir"))
    logger.logInfo("* dita.dir = " + Properties("dita.dir"))
    logger.logInfo("* input = " + Properties("args.input"))
    logger.logInfo("* transtype = " + Properties("transtype"))
    logger.logInfo("* tempdir = " + Properties("dita.temp.dir"))
    logger.logInfo("* outputdir = " + Properties("output.dir"))
    logger.logInfo("* extname = " + Properties("dita.ext"))
    logger.logInfo("* clean.temp = " + Properties("clean.temp"))
    logger.logInfo("* DITA-OT version = " + Properties("otversion"))
    logger.logInfo("* XML parser = " + Properties("xml.parser"))
    logger.logInfo("* XSLT processor = " + Properties("xslt.parser"))
    logger.logInfo("* collator = " + Properties("collator"))
    logger.logInfo("*****************************************************************")
    logger.logInfo("*****************************************************************")
  }

  def outputMsg() {
    logger.logInfo("\noutput-msg:")
    History.depends(("output-css-warn-message", outputCssWarnMessage))
  }

  def outputCssWarnMessage() {
    logger.logInfo("\noutput-css-warn-message:")
    if (!Properties.contains("args.csspath.absolute")) {
      return
    }

    logger.logInfo(get_msg("DOTA006W"))
  }

  /**Preprocessing ended */
  def preprocess() {
    logger.logInfo("\npreprocess:")
    History.depends(("gen-list", genList), ("debug-filter", debugFilter), ("copy-files", copyFiles), ("conrefpush", conrefpush), ("conref", conref), ("move-meta-entries", moveMetaEntries), ("keyref", keyref), ("coderef", coderef), ("mapref", mapref), ("mappull", mappull), ("chunk", chunk), ("maplink", maplink), ("move-links", moveLinks), ("topicpull", topicpull))
  }

  /**Clean temp directory */
  def cleanTemp() {
    logger.logInfo("\nclean-temp:")
    if (Properties.contains("clean-temp.skip")) {
      return
    }

    delete(new File(Properties("dita.temp.dir")), listAll(new File(Properties("dita.temp.dir"))))
  }

  /**Generate file list */
  def genList() {
    logger.logInfo("\ngen-list:")
    if (Properties.contains("preprocess.gen-list.skip")) {
      return
    }

    import org.dita.dost.module.GenMapAndTopicListModule
    val module = new org.dita.dost.module.GenMapAndTopicListModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", Properties("args.input"))
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    modulePipelineInput.setAttribute("ditadir", Properties("dita.dir"))
    if (Properties.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", Properties("dita.input.valfile"))
    }
    if (Properties.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", Properties("dita.ext"))
    }
    modulePipelineInput.setAttribute("validate", Properties("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", Properties("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", Properties("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", Properties("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", Properties("output.dir"))
    modulePipelineInput.setAttribute("transtype", Properties("transtype"))
    modulePipelineInput.setAttribute("gramcache", Properties("args.grammar.cache"))
    modulePipelineInput.setAttribute("setsystemid", Properties("args.xml.systemid.set"))
    module.execute(modulePipelineInput)
  }

  /**Debug and filter input files */
  def debugFilter() {
    logger.logInfo("\ndebug-filter:")
    History.depends(("gen-list", genList))
    if (Properties.contains("preprocess.debug-filter.skip")) {
      return
    }

    import org.dita.dost.module.DebugAndFilterModule
    val module = new org.dita.dost.module.DebugAndFilterModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    if (Properties.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", Properties("dita.input.valfile"))
    }
    if (Properties.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", Properties("dita.ext"))
    }
    modulePipelineInput.setAttribute("ditadir", Properties("dita.dir"))
    modulePipelineInput.setAttribute("validate", Properties("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", Properties("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", Properties("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", Properties("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", Properties("output.dir"))
    modulePipelineInput.setAttribute("transtype", Properties("transtype"))
    modulePipelineInput.setAttribute("setsystemid", Properties("args.xml.systemid.set"))
    module.execute(modulePipelineInput)

    job = new Job(new File(Properties("dita.temp.dir")))
    Properties.readXmlProperties(new File(Properties("dita.temp.dir") + File.separator + "dita.xml.properties"))
    Properties("dita.map.output.dir") = new File(Properties("output.dir") + File.separator + Properties("user.input.file")).getParent()
    if (job.getSet("conreflist").isEmpty()) {
      Properties("noConref") = "true"
    }
    if (job.getSet("fullditamaplist").isEmpty()) {
      Properties("noMap") = "true"
    }
    if (job.getSet("imagelist").isEmpty()) {
      Properties("noImagelist") = "true"
    }
    if (job.getSet("htmllist").isEmpty()) {
      Properties("noHtmllist") = "true"
    }
    if (job.getSet("subtargetslist").isEmpty()) {
      Properties("noSublist") = "true"
    }
    if (job.getSet("conrefpushlist").isEmpty()) {
      Properties("noConrefPush") = "true"
    }
    if (job.getSet("keyreflist").isEmpty()) {
      Properties("noKeyref") = "true"
    }
    if (job.getSet("codereflist").isEmpty()) {
      Properties("noCoderef") = "true"
    }
  }

  /**Resolve conref push */
  def conrefpush() {
    logger.logInfo("\nconrefpush:")
    History.depends(("debug-filter", debugFilter))
    if (Properties.contains("noConrefPush")) {
      Properties("preprocess.conrefpush.skip") = "true"
    }

    if (Properties.contains("preprocess.conrefpush.skip")) {
      return
    }

    import org.dita.dost.module.ConrefPushModule
    val module = new org.dita.dost.module.ConrefPushModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Move metadata entries */
  def moveMetaEntries() {
    logger.logInfo("\nmove-meta-entries:")
    History.depends(("debug-filter", debugFilter))
    if (Properties.contains("noMap")) {
      Properties("preprocess.move-meta-entries.skip") = "true"
    }

    if (Properties.contains("preprocess.move-meta-entries.skip")) {
      return
    }

    import org.dita.dost.module.MoveMetaModule
    val module = new org.dita.dost.module.MoveMetaModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", Properties("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Resolve conref in input files */
  def conref() {
    logger.logInfo("\nconref:")
    History.depends(("debug-filter", debugFilter), ("conrefpush", conrefpush))
    if (Properties.contains("noConref")) {
      Properties("preprocess.conref.skip") = "true"
    }

    if (Properties.contains("preprocess.conref.skip")) {
      return
    }

    if (!Properties.contains("dita.preprocess.reloadstylesheet.conref")) {
      Properties("dita.preprocess.reloadstylesheet.conref") = Properties("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "conref.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("dita.temp.dir"))
    val temp_ext = ".cnrf"
    val files = job.getSet("conreflist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      transformer.setParameter("BASEDIR", Properties("basedir"))
      transformer.setParameter("TEMPDIR", Properties("dita.temp.dir"))
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
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
    logger.logInfo("\ncoderef:")
    History.depends(("debug-filter", debugFilter), ("keyref", keyref))
    if (Properties.contains("noCoderef")) {
      Properties("preprocess.coderef.skip") = "true"
    }

    if (Properties.contains("preprocess.coderef.skip")) {
      return
    }

    import org.dita.dost.module.CoderefModule
    val module = new org.dita.dost.module.CoderefModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    module.execute(modulePipelineInput)
  }

  /**Resolve mapref in ditamap */
  def mapref() {
    logger.logInfo("\nmapref:")
    History.depends(("coderef", coderef))
    if (Properties.contains("noMap")) {
      Properties("preprocess.mapref.skip") = "true"
    }

    if (Properties.contains("preprocess.mapref.skip")) {
      return
    }

    if (!Properties.contains("dita.preprocess.reloadstylesheet.mapref")) {
      Properties("dita.preprocess.reloadstylesheet.mapref") = Properties("dita.preprocess.reloadstylesheet")
    }
    Properties("mapref.workdir") = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file")).getParent()
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mapref.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("dita.temp.dir"))
    val temp_ext = ".ditamap.ref"
    val files = job.getSet("fullditamaplist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
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
    logger.logInfo("\nkeyref:")
    History.depends(("move-meta-entries", moveMetaEntries))
    if (Properties.contains("noKeyref")) {
      Properties("preprocess.keyref.skip") = "true"
    }

    if (Properties.contains("preprocess.keyref.skip")) {
      return
    }

    import org.dita.dost.module.KeyrefModule
    val module = new org.dita.dost.module.KeyrefModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    if (Properties.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", Properties("dita.ext"))
    }
    module.execute(modulePipelineInput)
  }

  /**Pull the navtitle and topicmeta from topics to ditamap */
  def mappull() {
    logger.logInfo("\nmappull:")
    History.depends(("mapref", mapref))
    if (Properties.contains("noMap")) {
      Properties("preprocess.mappull.skip") = "true"
    }

    if (Properties.contains("preprocess.mappull.skip")) {
      return
    }

    Properties("mappull.workdir") = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file")).getParent()
    if (!Properties.contains("dita.preprocess.reloadstylesheet.mappull")) {
      Properties("dita.preprocess.reloadstylesheet.mappull") = Properties("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "mappull.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("dita.temp.dir"))
    val temp_ext = ".ditamap.pull"
    val files = job.getSet("fullditamaplist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      transformer.setParameter("TRANSTYPE", Properties("transtype"))
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
    logger.logInfo("\nchunk:")
    History.depends(("mappull", mappull))
    if (Properties.contains("noMap")) {
      Properties("preprocess.chunk.skip") = "true"
    }

    if (Properties.contains("preprocess.chunk.skip")) {
      return
    }

    import org.dita.dost.module.ChunkModule
    val module = new org.dita.dost.module.ChunkModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", Properties("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    if (Properties.contains("dita.ext")) {
      modulePipelineInput.setAttribute("ditaext", Properties("dita.ext"))
    }
    modulePipelineInput.setAttribute("transtype", Properties("transtype"))
    module.execute(modulePipelineInput)

    job = new Job(new File(Properties("dita.temp.dir")))
    Properties.readXmlProperties(new File(Properties("dita.temp.dir") + File.separator + "dita.xml.properties"))
    if (job.getSet("fullditatopiclist").isEmpty()) {
      Properties("noTopic") = "true"
    }
  }

  /**Find and generate related link information */
  def maplink() {
    logger.logInfo("\nmaplink:")
    History.depends(("chunk", chunk))
    if (Properties.contains("noMap")) {
      Properties("preprocess.maplink.skip") = "true"
    }

    if (Properties.contains("preprocess.maplink.skip")) {
      return
    }

    Properties("maplink.workdir") = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file")).getParent()
    if (!Properties.contains("dita.preprocess.reloadstylesheet.maplink")) {
      Properties("dita.preprocess.reloadstylesheet.maplink") = Properties("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "maplink.xsl"))
    val in_file = new File(Properties("dita.temp.dir") + File.separator + Properties("user.input.file"))
    val out_file = new File(Properties("maplink.workdir") + File.separator + "maplinks.unordered")
    if (!out_file.getParentFile().exists()) {
      out_file.getParentFile().mkdirs()
    }
    val transformer = templates.newTransformer()
    if (Properties.contains("dita.ext")) {
      transformer.setParameter("DITAEXT", Properties("dita.ext"))
    }
    transformer.setParameter("INPUTMAP", Properties("user.input.file"))
    if (Properties.contains("include.rellinks")) {
      transformer.setParameter("include.rellinks", Properties("include.rellinks"))
    }
    val source = getSource(in_file)
    val result = new StreamResult(out_file)
    logger.logInfo("Processing " + in_file + " to " + out_file)
    transformer.transform(source, result)
  }

  /**Move the related link information to topics */
  def moveLinks() {
    logger.logInfo("\nmove-links:")
    History.depends(("maplink", maplink))
    if (Properties.contains("noMap")) {
      Properties("preprocess.move-links.skip") = "true"
    }

    if (Properties.contains("preprocess.move-links.skip")) {
      return
    }

    import org.dita.dost.module.MoveLinksModule
    val module = new org.dita.dost.module.MoveLinksModule
    module.setLogger(new DITAOTJavaLogger())
    val modulePipelineInput = new PipelineHashIO()
    modulePipelineInput.setAttribute("inputmap", Properties("user.input.file"))
    modulePipelineInput.setAttribute("tempDir", Properties("dita.temp.dir"))
    modulePipelineInput.setAttribute("maplinks", Properties("maplink.workdir") + "/maplinks.unordered")
    module.execute(modulePipelineInput)
  }

  /**Pull metadata for link and xref element */
  def topicpull() {
    logger.logInfo("\ntopicpull:")
    History.depends(("debug-filter", debugFilter))
    if (Properties.contains("noTopic")) {
      Properties("preprocess.topicpull.skip") = "true"
    }

    if (Properties.contains("preprocess.topicpull.skip")) {
      return
    }

    if (!Properties.contains("dita.preprocess.reloadstylesheet.topicpull")) {
      Properties("dita.preprocess.reloadstylesheet.topicpull") = Properties("dita.preprocess.reloadstylesheet")
    }
    val templates = compileTemplates(new File(Properties("dita.plugin.org.dita.base.dir") + File.separator + "xsl" + File.separator + "preprocess" + File.separator + "topicpull.xsl"))
    val base_dir = new File(Properties("dita.temp.dir"))
    val dest_dir = new File(Properties("dita.temp.dir"))
    val temp_ext = ".pull"
    val files = job.getSet("fullditatopiclist")
    for (l <- files) {
      val transformer = templates.newTransformer()
      if (Properties.contains("dita.ext")) {
        transformer.setParameter("DITAEXT", Properties("dita.ext"))
      }
      if (Properties.contains("args.tablelink.style")) {
        transformer.setParameter("TABLELINK", Properties("args.tablelink.style"))
      }
      if (Properties.contains("args.figurelink.style")) {
        transformer.setParameter("FIGURELINK", Properties("args.figurelink.style"))
      }
      if (Properties.contains("onlytopic.in.map")) {
        transformer.setParameter("ONLYTOPICINMAP", Properties("onlytopic.in.map"))
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
    logger.logInfo("\ncopy-files:")
    History.depends(("debug-filter", debugFilter), ("copy-image", copyImage), ("copy-html", copyHtml), ("copy-flag", copyFlag), ("copy-subsidiary", copySubsidiary), ("copy-generated-files", copyGeneratedFiles))
    if (Properties.contains("preprocess.copy-files.skip")) {
      return
    }

  }

  /**Copy image files */
  def copyImageUplevels() {
    logger.logInfo("\ncopy-image-uplevels:")
    History.depends()
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noImagelist"))) {
      Properties("preprocess.copy-image.skip") = "true"
    }
    if (Properties("generate.copy.outer") != "3") {
      Properties("image.copy.uplevels") = "true"
    }
    if ((Properties("generate.copy.outer") == "3")) {
      Properties("image.copy.normal") = "true"
    }

    if (!Properties.contains("image.copy.uplevels")) {
      return
    }
    if (Properties.contains("preprocess.copy-image.skip")) {
      return
    }

    copy(new File(Properties("user.input.dir")), new File(Properties("output.dir") + File.separator + Properties("uplevels")), new File(Properties("dita.temp.dir") + File.separator + Properties("imagefile")))
  }

  /**Copy image files */
  def copyImageNoraml() {
    logger.logInfo("\ncopy-image-noraml:")
    History.depends()
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noImagelist"))) {
      Properties("preprocess.copy-image.skip") = "true"
    }
    if (Properties("generate.copy.outer") != "3") {
      Properties("image.copy.uplevels") = "true"
    }
    if ((Properties("generate.copy.outer") == "3")) {
      Properties("image.copy.normal") = "true"
    }

    if (!Properties.contains("image.copy.normal")) {
      return
    }
    if (Properties.contains("preprocess.copy-image.skip")) {
      return
    }

    copy(new File(Properties("user.input.dir")), new File(Properties("output.dir")), new File(Properties("dita.temp.dir") + File.separator + Properties("imagefile")))
  }

  /**Copy image files */
  def copyImage() {
    logger.logInfo("\ncopy-image:")
    History.depends(("copy-image-uplevels", copyImageUplevels), ("copy-image-noraml", copyImageNoraml))
  }

  /**Copy html files */
  def copyHtml() {
    logger.logInfo("\ncopy-html:")
    History.depends()
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noHtmllist"))) {
      Properties("preprocess.copy-html.skip") = "true"
    }

    if (Properties.contains("preprocess.copy-html.skip")) {
      return
    }

    copy(new File(Properties("user.input.dir")), new File(Properties("output.dir")), new File(Properties("dita.temp.dir") + File.separator + Properties("htmlfile")))
  }

  /**Copy flag files */
  def copyFlag() {
    logger.logInfo("\ncopy-flag:")
    History.depends()
    if ((Properties.contains("preprocess.copy-files.skip") || !Properties.contains("dita.input.valfile"))) {
      Properties("preprocess.copy-flag.skip") = "true"
    }

    if (Properties.contains("preprocess.copy-flag.skip")) {
      return
    }

  }

  /**Copy subsidiary files */
  def copySubsidiary() {
    logger.logInfo("\ncopy-subsidiary:")
    History.depends()
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noSublist"))) {
      Properties("preprocess.copy-subsidiary.skip") = "true"
    }

    if (Properties.contains("preprocess.copy-subsidiary.skip")) {
      return
    }

    copy(new File(Properties("user.input.dir")), new File(Properties("dita.temp.dir")), new File(Properties("dita.temp.dir") + File.separator + Properties("subtargetsfile")))
  }

  /**Copy generated files */
  def copyGeneratedFiles() {
    logger.logInfo("\ncopy-generated-files:")
    if (Properties.contains("preprocess.copy-generated-files.skip")) {
      return
    }

    copy(new File(Properties("dita.temp.dir")), new File(Properties("args.logdir")), "dita.list,property.temp,dita.xml.properties".split(","))
  }
}
