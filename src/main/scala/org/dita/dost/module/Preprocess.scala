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

class DitaotPreprocess(ditaDir: File) extends Transtype(ditaDir) {
  // file:/Users/jelovirt/Work/github/dita-ot/src/main/plugins/org.dita.base/build_preprocess.xml

  Properties("ant.file.ditaot-preprocess") = new File("")
  // start src/main/build.xml
  Properties.read_properties(Properties("basedir") + "/local.properties")
  Properties("ant.file.DOST.dir") = new File(Properties("ant.file.DOST")).getParent()
  if ((!Properties.contains("dita.dir"))) {
    Properties("dita.dir") = Properties("ant.file.DOST.dir")
  }
  if ((!Properties.contains("dita.dir"))) {
    Properties("dita.dir") = Properties("basedir")
  }
  Properties("dita.plugin.org.dita.troff.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.troff")
  Properties("dita.plugin.org.dita.eclipsecontent.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.eclipsecontent")
  Properties("dita.plugin.org.dita.eclipsehelp.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.specialization.dita11.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.specialization.dita11")
  Properties("dita.plugin.org.dita.xhtml.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.odt.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.specialization.dita132.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.specialization.dita132")
  Properties("dita.plugin.org.dita.wordrtf.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.wordrtf")
  Properties("dita.plugin.org.dita.docbook.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.docbook")
  Properties("dita.plugin.org.dita.specialization.eclipsemap.dir") = new File(Properties("dita.dir") + "/plugins/org.dita.specialization.eclipsemap")
  Properties("dita.plugin.org.dita.htmlhelp.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.base.dir") = new File(Properties("dita.dir"))
  Properties("dita.plugin.org.dita.javahelp.dir") = new File(Properties("dita.dir"))
  // end src/main/build.xml
  // start src/main/plugins/org.dita.base/build_init.xml
  Properties("maxJavaMemory") = "500m"
  Properties.read_properties(Properties("dita.dir") + "/lib/org.dita.dost.platform/plugin.properties")
  Properties.read_properties(Properties("dita.dir") + "/lib/configuration.properties")
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
  Properties("base.temp.dir") = new File(Properties("basedir") + "/temp")
  Properties("dita.temp.dir") = new File(Properties("base.temp.dir") + File.separator + "temp" + Properties("current.date"))
  Properties("output.dir") = new File(Properties("basedir") + "/out")
  Properties("dita.script.dir") = new File(Properties("dita.dir") + File.separator + "xsl")
  Properties("dita.resource.dir") = new File(Properties("dita.dir") + File.separator + "resource")
  Properties("dita.empty") = ""
  Properties("args.message.file") = new File(Properties("dita.dir") + File.separator + "resource" + File.separator + "messages.xml")
  // end src/main/plugins/org.dita.base/build_init.xml
  if ((!Properties.contains("dita.preprocess.reloadstylesheet"))) {
    Properties("dita.preprocess.reloadstylesheet") = "false"
  }
  def buildInit() {
    println("\nbuild-init:")
    History.depends(("start-process", startProcess), ("init-logger", initLogger), ("init-URIResolver", initURIResolver), ("use-init", useInit), ("check-arg", checkArg), ("output-msg", outputMsg))

  }
  /**Processing started */
  def startProcess() {
    println("\nstart-process:")

  }
  /**Initialize log directory and file name */
  def initLogger() {
    println("\ninit-logger:")
    //TODO config_logger()

  }
  def initURIResolver() {
    println("\ninit-URIResolver:")
    var path = new File(Properties("dita.temp.dir"))
    if (!path.isAbsolute()) {
      path = new File("", path.getPath)
      DitaURIResolverFactory.setPath(path.getAbsolutePath)
    }

  }
  def useInit() {
    println("\nuse-init:")
    if ((Properties.contains("org.xml.sax.driver") && (!Properties.contains("xml.parser")))) {
      Properties("xml.parser") = "XMLReader " + Properties("org.xml.sax.driver")
    }
    if ((class_available("org.apache.xerces.parsers.SAXParser") && (!Properties.contains("xml.parser")))) {
      Properties("xml.parser") = "Xerces"
    }
    if ((class_available("com.sun.org.apache.xerces.internal.parsers.SAXParser") && (!Properties.contains("xml.parser")))) {
      Properties("xml.parser") = "Xerces in Sun JDK 1.5"
    }
    if ((class_available("org.apache.crimson.parser.XMLReaderImpl") && (!Properties.contains("xml.parser")))) {
      Properties("xml.parser") = "Crimson"
    }

  }
  /**Validate and init input arguments */
  def checkArg() {
    println("\ncheck-arg:")
    History.depends(("use-init", useInit))
    if ((Properties.contains("args.input") && !(new File(Properties("args.input")).exists()))) {
      println("DOTA069F")
      sys.exit()
    }
    if (((!Properties.contains("args.input")))) {
      println("DOTA002F")
      sys.exit()
    }
    if ((Properties.contains("args.xsl") && !(new File(Properties("args.xsl")).exists()))) {
      println("DOTA003F")
      sys.exit()
    }
    if ((Properties.contains("args.ftr") && !(new File(Properties("args.ftr")).exists()))) {
      println("DOTA007E")
      sys.exit()
    }
    if ((Properties.contains("args.hdr") && !(new File(Properties("args.hdr")).exists()))) {
      println("DOTA008E")
      sys.exit()
    }
    if ((Properties.contains("args.hdf") && !(new File(Properties("args.hdf")).exists()))) {
      println("DOTA009E")
      sys.exit()
    }
    if (Properties.contains("dita.input.valfile")) {
      println("DOTA012W")
      sys.exit()
    }
    if ((Properties.contains("args.filter") && (!Properties.contains("dita.input.valfile")))) {
      Properties("dita.input.valfile") = Properties("args.filter")
    }
    if ((Properties.contains("args.outext") && !(Properties("args.outext").indexOf(".") != -1))) {
      Properties("out.ext") = "." + Properties("args.outext")
    }
    if ((Properties.contains("args.outext") && Properties("args.outext").indexOf(".") != -1)) {
      Properties("out.ext") = Properties("args.outext")
    }
    if ((!Properties.contains("args.grammar.cache"))) {
      Properties("args.grammar.cache") = "yes"
    }
    if ((!Properties.contains("args.xml.systemid.set"))) {
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
    if (((!Properties.contains("args.csspath")) || Properties.contains("args.csspath.absolute"))) {
      Properties("user.csspath") = ""
    }
    if ((!Properties.contains("user.csspath"))) {
      Properties("user.csspath") = Properties("args.csspath") + "/"
    }
    if (Properties.contains("args.cssroot")) {
      Properties("args.css.real") = Properties("args.cssroot") + File.separator + Properties("args.css")
    }
    if ((!Properties.contains("args.cssroot"))) {
      Properties("args.css.real") = Properties("args.css")
    }
    if (new File(Properties("args.css.real")).exists()) {
      Properties("args.css.present") = "true"
    }
    Properties("args.css.file.temp") = new File(Properties("args.css")).getName()
    if ((Properties.contains("args.css.present") || Properties.contains("user.csspath.url"))) {
      Properties("args.css.file") = Properties("args.css.file.temp")
    }
    if ((!Properties.contains("args.logdir"))) {
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
    if ((!Properties.contains("validate"))) {
      Properties("validate") = "true"
    }
    if (Properties("args.rellinks") == "none") {
      Properties("include.rellinks") = ""
    }
    if (Properties("args.rellinks") == "nofamily") {
      Properties("include.rellinks") = "#default friend sample external other"
    }
    if ((Properties("args.rellinks") == "all" || (!Properties.contains("args.rellinks")))) {
      Properties("include.rellinks") = "#default parent child sibling friend next previous cousin ancestor descendant sample external other"
    }
    if ((!Properties.contains("generate.copy.outer"))) {
      Properties("generate.copy.outer") = "1"
    }
    if ((!Properties.contains("onlytopic.in.map"))) {
      Properties("onlytopic.in.map") = "false"
    }
    if ((!Properties.contains("outer.control"))) {
      Properties("outer.control") = "warn"
    }
    if ((Properties("generate.copy.outer") == "1" || Properties("generate.copy.outer") == "2")) {
      Properties("inner.transform") = "true"
    }
    if (Properties("generate.copy.outer") == "3") {
      Properties("old.transform") = "true"
    }
    println("*****************************************************************")
    println("* basedir = " + Properties("basedir"))
    println("* dita.dir = " + Properties("dita.dir"))
    println("* input = " + Properties("args.input"))
    println("* transtype = " + Properties("transtype"))
    println("* tempdir = " + Properties("dita.temp.dir"))
    println("* outputdir = " + Properties("output.dir"))
    println("* extname = " + Properties("dita.ext"))
    println("* clean.temp = " + Properties("clean.temp"))
    println("* DITA-OT version = " + Properties("otversion"))
    println("* XML parser = " + Properties("xml.parser"))
    println("* XSLT processor = " + Properties("xslt.parser"))
    println("* collator = " + Properties("collator"))
    println("*****************************************************************")
    println("*****************************************************************")

  }
  def outputMsg() {
    println("\noutput-msg:")
    History.depends(("output-css-warn-message", outputCssWarnMessage))

  }
  def outputCssWarnMessage() {
    println("\noutput-css-warn-message:")
    if (!Properties.contains("args.csspath.absolute")) {
      return
    }
    println(get_msg("DOTA006W"))

  }
  /**Preprocessing ended */
  def preprocess() {
    println("\npreprocess:")
    History.depends(("gen-list", genList), ("debug-filter", debugFilter), ("copy-files", copyFiles), ("conrefpush", conrefpush), ("conref", conref), ("move-meta-entries", moveMetaEntries), ("keyref", keyref), ("coderef", coderef), ("mapref", mapref), ("mappull", mappull), ("chunk", chunk), ("maplink", maplink), ("move-links", moveLinks), ("topicpull", topicpull))

  }
  /**Clean temp directory */
  def cleanTemp() {
    println("\nclean-temp:")
    if (Properties.contains("clean-temp.skip")) {
      return
    }

  }
  /**Generate file list */
  def genList() {
    println("\ngen-list:")
    if (Properties.contains("preprocess.gen-list.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("args.input")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.GenMapAndTopicListModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("ditadir") = Properties("dita.dir")
    if (Properties.contains("dita.input.valfile")) {
      attrs("ditaval") = Properties("dita.input.valfile")

    }
    if (Properties.contains("dita.ext")) {
      attrs("ditaext") = Properties("dita.ext")

    }
    attrs("validate") = Properties("validate")
    attrs("generatecopyouter") = Properties("generate.copy.outer")
    attrs("outercontrol") = Properties("outer.control")
    attrs("onlytopicinmap") = Properties("onlytopic.in.map")
    attrs("outputdir") = Properties("output.dir")
    attrs("transtype") = Properties("transtype")
    attrs("gramcache") = Properties("args.grammar.cache")
    attrs("setsystemid") = Properties("args.xml.systemid.set")
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  /**Debug and filter input files */
  def debugFilter() {
    println("\ndebug-filter:")
    History.depends(("gen-list", genList))
    if (Properties.contains("preprocess.debug-filter.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.DebugAndFilterModule])
    module.setLogger(new DITAOTJavaLogger())
    if (Properties.contains("dita.input.valfile")) {
      attrs("ditaval") = Properties("dita.input.valfile")

    }
    if (Properties.contains("dita.ext")) {
      attrs("ditaext") = Properties("dita.ext")

    }
    attrs("ditadir") = Properties("dita.dir")
    attrs("validate") = Properties("validate")
    attrs("generatecopyouter") = Properties("generate.copy.outer")
    attrs("outercontrol") = Properties("outer.control")
    attrs("onlytopicinmap") = Properties("onlytopic.in.map")
    attrs("outputdir") = Properties("output.dir")
    attrs("transtype") = Properties("transtype")
    attrs("setsystemid") = Properties("args.xml.systemid.set")
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)
    Properties.read_xml_properties(Properties("dita.temp.dir") + "/dita.xml.properties")
    Properties("dita.map.output.dir") = new File(Properties("output.dir") + "/" + Properties("user.input.file")).getParent()
    if (Properties("conreflist") == "") {
      Properties("noConref") = "true"
    }
    if (Properties("fullditamaplist") == "") {
      Properties("noMap") = "true"
    }
    if (Properties("imagelist") == "") {
      Properties("noImagelist") = "true"
    }
    if (Properties("htmllist") == "") {
      Properties("noHtmllist") = "true"
    }
    if (Properties("subtargetslist") == "") {
      Properties("noSublist") = "true"
    }
    if (Properties("conrefpushlist") == "") {
      Properties("noConrefPush") = "true"
    }
    if (Properties("keyreflist") == "") {
      Properties("noKeyref") = "true"
    }
    if (Properties("codereflist") == "") {
      Properties("noCoderef") = "true"
    }

  }
  /**Resolve conref push */
  def conrefpush() {
    println("\nconrefpush:")
    History.depends(("debug-filter", debugFilter), ("conrefpush-check", conrefpushCheck))
    if (Properties.contains("preprocess.conrefpush.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.ConrefPushModule])
    module.setLogger(new DITAOTJavaLogger())
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def conrefpushCheck() {
    println("\nconrefpush-check:")
    if (Properties.contains("noConrefPush")) {
      Properties("preprocess.conrefpush.skip") = "true"
    }

  }
  /**Move metadata entries */
  def moveMetaEntries() {
    println("\nmove-meta-entries:")
    History.depends(("debug-filter", debugFilter), ("move-meta-entries-check", moveMetaEntriesCheck))
    if (Properties.contains("preprocess.move-meta-entries.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.MoveMetaModule])
    module.setLogger(new DITAOTJavaLogger())
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def moveMetaEntriesCheck() {
    println("\nmove-meta-entries-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.move-meta-entries.skip") = "true"
    }

  }
  /**Resolve conref in input files */
  def conref() {
    println("\nconref:")
    History.depends(("debug-filter", debugFilter), ("conrefpush", conrefpush), ("conref-check", conrefCheck))
    if (Properties.contains("preprocess.conref.skip")) {
      return
    }
    if ((!Properties.contains("dita.preprocess.reloadstylesheet.conref"))) {
      Properties("dita.preprocess.reloadstylesheet.conref") = Properties("dita.preprocess.reloadstylesheet")
    }

    try {

      val templates = compileTemplates(new File(Properties("dita.script.dir") + "/preprocess/conref.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("dita.temp.dir"))
      val temp_ext = ".cnrf"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + "/" + Properties("conreffile")), "UTF-8")
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
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        val dst = new File(base_dir, l)
        println("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
        src.renameTo(dst)
      }

    }

  }
  def conrefCheck() {
    println("\nconref-check:")
    if (Properties.contains("noConref")) {
      Properties("preprocess.conref.skip") = "true"
    }

  }
  /**Resolve coderef in input files */
  def coderef() {
    println("\ncoderef:")
    History.depends(("debug-filter", debugFilter), ("keyref", keyref), ("coderef-check", coderefCheck))
    if (Properties.contains("preprocess.coderef.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.CoderefModule])
    module.setLogger(new DITAOTJavaLogger())
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def coderefCheck() {
    println("\ncoderef-check:")
    if (Properties.contains("noCoderef")) {
      Properties("preprocess.coderef.skip") = "true"
    }

  }
  /**Resolve mapref in ditamap */
  def mapref() {
    println("\nmapref:")
    History.depends(("coderef", coderef), ("mapref-check", maprefCheck))
    if (Properties.contains("preprocess.mapref.skip")) {
      return
    }
    if ((!Properties.contains("dita.preprocess.reloadstylesheet.mapref"))) {
      Properties("dita.preprocess.reloadstylesheet.mapref") = Properties("dita.preprocess.reloadstylesheet")
    }
    Properties("mapref.workdir") = new File(Properties("dita.temp.dir") + "/" + Properties("user.input.file")).getParent()

    try {

      val templates = compileTemplates(new File(Properties("dita.script.dir") + "/preprocess/mapref.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("dita.temp.dir"))
      val temp_ext = ".ditamap.ref"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + "/" + Properties("fullditamapfile")), "UTF-8")
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
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        val dst = new File(base_dir, l)
        println("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
        src.renameTo(dst)
      }

    }

  }
  def maprefCheck() {
    println("\nmapref-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.mapref.skip") = "true"
    }

  }
  /**Resolve keyref */
  def keyref() {
    println("\nkeyref:")
    History.depends(("move-meta-entries", moveMetaEntries), ("keyref-check", keyrefCheck))
    if (Properties.contains("preprocess.keyref.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.KeyrefModule])
    module.setLogger(new DITAOTJavaLogger())
    if (Properties.contains("dita.ext")) {
      attrs("ditaext") = Properties("dita.ext")

    }
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def keyrefCheck() {
    println("\nkeyref-check:")
    if (Properties.contains("noKeyref")) {
      Properties("preprocess.keyref.skip") = "true"
    }

  }
  /**Pull the navtitle and topicmeta from topics to ditamap */
  def mappull() {
    println("\nmappull:")
    History.depends(("mapref", mapref), ("mappull-check", mappullCheck))
    if (Properties.contains("preprocess.mappull.skip")) {
      return
    }
    Properties("mappull.workdir") = new File(Properties("dita.temp.dir") + "/" + Properties("user.input.file")).getParent()
    if ((!Properties.contains("dita.preprocess.reloadstylesheet.mappull"))) {
      Properties("dita.preprocess.reloadstylesheet.mappull") = Properties("dita.preprocess.reloadstylesheet")
    }

    try {

      val templates = compileTemplates(new File(Properties("dita.script.dir") + "/preprocess/mappull.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("dita.temp.dir"))
      val temp_ext = ".ditamap.pull"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + "/" + Properties("fullditamapfile")), "UTF-8")
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
        transformer.setParameter("TRANSTYPE", Properties("transtype"))
        val in_file = new File(base_dir, l)
        val out_file = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        if (!out_file.getParentFile().exists()) {
          out_file.getParentFile().mkdirs()
        }
        val source = getSource(in_file)
        val result = new StreamResult(out_file)
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        val dst = new File(base_dir, l)
        println("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
        src.renameTo(dst)
      }

    }

  }
  def mappullCheck() {
    println("\nmappull-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.mappull.skip") = "true"
    }

  }
  /**Process chunks */
  def chunk() {
    println("\nchunk:")
    History.depends(("mappull", mappull), ("chunk-check", chunkCheck))
    if (Properties.contains("preprocess.chunk.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.ChunkModule])
    module.setLogger(new DITAOTJavaLogger())
    if (Properties.contains("dita.ext")) {
      attrs("ditaext") = Properties("dita.ext")

    }
    attrs("transtype") = Properties("transtype")
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)
    Properties.read_xml_properties(Properties("dita.temp.dir") + "/dita.xml.properties")
    if (Properties("fullditatopiclist") == "") {
      Properties("noTopic") = "true"
    }

  }
  def chunkCheck() {
    println("\nchunk-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.chunk.skip") = "true"
    }

  }
  /**Find and generate related link information */
  def maplink() {
    println("\nmaplink:")
    History.depends(("chunk", chunk), ("maplink-check", maplinkCheck))
    if (Properties.contains("preprocess.maplink.skip")) {
      return
    }
    Properties("maplink.workdir") = new File(Properties("dita.temp.dir") + "/" + Properties("user.input.file")).getParent()
    if ((!Properties.contains("dita.preprocess.reloadstylesheet.maplink"))) {
      Properties("dita.preprocess.reloadstylesheet.maplink") = Properties("dita.preprocess.reloadstylesheet")
    }

    try {
      val templates = compileTemplates(new File(Properties("dita.script.dir") + "/preprocess/maplink.xsl"))
      val in_file = new File(Properties("dita.temp.dir") + "/" + Properties("user.input.file"))
      val out_file = new File(Properties("maplink.workdir") + "/maplinks.unordered")
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
      println("Processing " + in_file + " to " + out_file)
      transformer.transform(source, result)

    }

  }
  def maplinkCheck() {
    println("\nmaplink-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.maplink.skip") = "true"
    }

  }
  /**Move the related link information to topics */
  def moveLinks() {
    println("\nmove-links:")
    History.depends(("maplink", maplink), ("move-links-check", moveLinksCheck))
    if (Properties.contains("preprocess.move-links.skip")) {
      return
    }
    val attrs = scala.collection.mutable.Map[String, String]()
    attrs("inputmap") = Properties("user.input.file")
    attrs("tempDir") = Properties("dita.temp.dir")
    val module = ModuleFactory.instance().createModule(classOf[org.dita.dost.module.MoveLinksModule])
    module.setLogger(new DITAOTJavaLogger())
    attrs("maplinks") = Properties("maplink.workdir") + "/maplinks.unordered"
    val module_pipelineInput = new PipelineHashIO()
    for (e <- attrs.entrySet()) {
      module_pipelineInput.setAttribute(e.getKey(), e.getValue())
    }
    module.execute(module_pipelineInput)

  }
  def moveLinksCheck() {
    println("\nmove-links-check:")
    if (Properties.contains("noMap")) {
      Properties("preprocess.move-links.skip") = "true"
    }

  }
  /**Pull metadata for link and xref element */
  def topicpull() {
    println("\ntopicpull:")
    History.depends(("debug-filter", debugFilter), ("topicpull-check", topicpullCheck))
    if (Properties.contains("preprocess.topicpull.skip")) {
      return
    }
    if ((!Properties.contains("dita.preprocess.reloadstylesheet.topicpull"))) {
      Properties("dita.preprocess.reloadstylesheet.topicpull") = Properties("dita.preprocess.reloadstylesheet")
    }

    try {

      val templates = compileTemplates(new File(Properties("dita.script.dir") + "/preprocess/topicpull.xsl"))
      val base_dir = new File(Properties("dita.temp.dir"))
      val dest_dir = new File(Properties("dita.temp.dir"))
      val temp_ext = ".pull"
      val includes_file = Source.fromFile(new File(Properties("dita.temp.dir") + "/" + Properties("fullditatopicfile")), "UTF-8")
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
        println("Processing " + in_file + " to " + out_file)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(dest_dir, FileUtils.replaceExtension(l, temp_ext))
        val dst = new File(base_dir, l)
        println("Moving " + new File(dest_dir, FileUtils.replaceExtension(l, temp_ext)) + " to " + new File(base_dir, l))
        src.renameTo(dst)
      }

    }

  }
  def topicpullCheck() {
    println("\ntopicpull-check:")
    if (Properties.contains("noTopic")) {
      Properties("preprocess.topicpull.skip") = "true"
    }

  }
  def copyFiles() {
    println("\ncopy-files:")
    History.depends(("debug-filter", debugFilter), ("copy-image", copyImage), ("copy-html", copyHtml), ("copy-flag", copyFlag), ("copy-subsidiary", copySubsidiary), ("copy-generated-files", copyGeneratedFiles))
    if (Properties.contains("preprocess.copy-files.skip")) {
      return
    }

  }
  /**Copy image files */
  def copyImageUplevels() {
    println("\ncopy-image-uplevels:")
    History.depends(("copy-image-check", copyImageCheck))
    if (!Properties.contains("image.copy.uplevels")) {
      return
    }
    if (Properties.contains("preprocess.copy-image.skip")) {
      return
    }
    copy_list(Properties("user.input.dir"), Properties("output.dir") + "/" + Properties("uplevels"), Properties("dita.temp.dir") + "/" + Properties("imagefile"))

  }
  /**Copy image files */
  def copyImageNoraml() {
    println("\ncopy-image-noraml:")
    History.depends(("copy-image-check", copyImageCheck))
    if (!Properties.contains("image.copy.normal")) {
      return
    }
    if (Properties.contains("preprocess.copy-image.skip")) {
      return
    }
    copy_list(Properties("user.input.dir"), Properties("output.dir"), Properties("dita.temp.dir") + "/" + Properties("imagefile"))

  }
  /**Copy image files */
  def copyImage() {
    println("\ncopy-image:")
    History.depends(("copy-image-uplevels", copyImageUplevels), ("copy-image-noraml", copyImageNoraml))

  }
  def copyImageCheck() {
    println("\ncopy-image-check:")
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noImagelist"))) {
      Properties("preprocess.copy-image.skip") = "true"
    }
    if (!(Properties("generate.copy.outer") == "3")) {
      Properties("image.copy.uplevels") = "true"
    }
    if ((Properties("generate.copy.outer") == "3")) {
      Properties("image.copy.normal") = "true"
    }

  }
  /**Copy html files */
  def copyHtml() {
    println("\ncopy-html:")
    History.depends(("copy-html-check", copyHtmlCheck))
    if (Properties.contains("preprocess.copy-html.skip")) {
      return
    }
    copy_list(Properties("user.input.dir"), Properties("output.dir"), Properties("dita.temp.dir") + "/" + Properties("htmlfile"))

  }
  def copyHtmlCheck() {
    println("\ncopy-html-check:")
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noHtmllist"))) {
      Properties("preprocess.copy-html.skip") = "true"
    }

  }
  /**Copy flag files */
  def copyFlag() {
    println("\ncopy-flag:")
    History.depends(("copy-flag-check", copyFlagCheck))
    if (Properties.contains("preprocess.copy-flag.skip")) {
      return
    }

  }
  def copyFlagCheck() {
    println("\ncopy-flag-check:")
    if ((Properties.contains("preprocess.copy-files.skip") || (!Properties.contains("dita.input.valfile")))) {
      Properties("preprocess.copy-flag.skip") = "true"
    }

  }
  /**Copy subsidiary files */
  def copySubsidiary() {
    println("\ncopy-subsidiary:")
    History.depends(("copy-subsidiary-check", copySubsidiaryCheck))
    if (Properties.contains("preprocess.copy-subsidiary.skip")) {
      return
    }
    copy_list(Properties("user.input.dir"), Properties("dita.temp.dir"), Properties("dita.temp.dir") + "/" + Properties("subtargetsfile"))

  }
  def copySubsidiaryCheck() {
    println("\ncopy-subsidiary-check:")
    if ((Properties.contains("preprocess.copy-files.skip") || Properties.contains("noSublist"))) {
      Properties("preprocess.copy-subsidiary.skip") = "true"
    }

  }
  /**Copy generated files */
  def copyGeneratedFiles() {
    println("\ncopy-generated-files:")
    if (Properties.contains("preprocess.copy-generated-files.skip")) {
      return
    }
    copy(Properties("dita.temp.dir"), Properties("args.logdir"), "dita.list,property.temp,dita.xml.properties")

  }

}
