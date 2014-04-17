package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File

import javax.xml.transform.Transformer

import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.util.FileUtils

abstract class Preprocess(ditaDir: File) extends Transtype(ditaDir) {

  $("ant.file.ditaot-preprocess") = new File("plugins/org.dita.base/build_preprocess.xml")

  var innerTransform: Boolean = false
  var oldTransform: Boolean = false
  /*
  var is64bit: Boolean = false
  var is32bit: Boolean = false
  */
  var noPlugin: Boolean = false

  //$("maxJavaMemory") = "500m"
  $.readProperties(new File(ditaDir, "lib" + File.separator + "org.dita.dost.platform" + File.separator + "plugin.properties"))
  $.readProperties(new File(ditaDir, "lib" + File.separator + "configuration.properties"))
  /*
  if (((System.getProperty("os.name") == "x86_64" || System.getProperty("os.name") == "amd64" || System.getProperty("os.name") == "ppc64") && !(System.getProperty("os.name") == "windows"))) {
    is64bit = true
  }
  if (!is64bit) {
    is32bit = true
  }
  if (is64bit) {
    $("jvmArchFlag") = "-d64"
  }
  else {
    $("jvmArchFlag") = ""
  }
  $("baseJVMArgLine") = $("jvmArchFlag") + " -Xmx" + $("maxJavaMemory")
  */
  override val baseTempDir = new File($("basedir"), "temp")
  override val ditaTempDir = new File(baseTempDir, "temp" + System.currentTimeMillis)
  override val outputDir = new File($("basedir"), "out")
  if (!$.contains("dita.preprocess.reloadstylesheet")) {
    $("dita.preprocess.reloadstylesheet") = "false"
  }


  def buildInit() {
    checkArg()
    logArg()
  }

  /** Validate and init input arguments */
  private def checkArg() {
    logger.info("check-arg:")
    if ($.contains("args.xsl") && !new File($("args.xsl")).exists) {
      throw new IllegalArgumentException("DOTA003F")
    }
    if ($.contains("dita.input.valfile")) {
      throw new IllegalStateException("DOTA012W")
    }
    if ($.contains("args.filter") && !$.contains("dita.input.valfile")) {
      $("dita.input.valfile") = $("args.filter")
    }
    if ($.contains("args.outext") && $("args.outext").indexOf(".") == -1) {
      $("out.ext") = "." + $("args.outext")
    }
    if ($.contains("args.outext") && $("args.outext").indexOf(".") != -1) {
      $("out.ext") = $("args.outext")
    }
    if (!$.contains("args.grammar.cache")) {
      $("args.grammar.cache") = "yes"
    }
    if (!$.contains("args.xml.systemid.set")) {
      $("args.xml.systemid.set") = "yes"
    }
    if (!outputDir.exists) {
      outputDir.mkdirs()
    }
    delete(ditaTempDir, listAll(ditaTempDir))
    if (!ditaTempDir.exists) {
      ditaTempDir.mkdirs()
    }
    /*if (!$.contains("args.logdir")) {
      $("args.logdir") = outputDir
    }*/
    if (!$.contains("validate")) {
      $("validate") = "true"
    }
    if ($("args.rellinks") == "none") {
      $("include.rellinks") = ""
    }
    if ($("args.rellinks") == "nofamily") {
      $("include.rellinks") = "#default friend sample external other"
    }
    if ($("args.hide.parent.link") == "yes") {
      $("include.rellinks") = "#default child sibling friend next previous cousin ancestor descendant sample external other"
    }
    if ($("args.rellinks") == "all" || !$.contains("args.rellinks")) {
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
    if ($("generate.copy.outer") == "1" || $("generate.copy.outer") == "2") {
      innerTransform = true
    }
    if ($("generate.copy.outer") == "3") {
      oldTransform = true
    }
  }

  private def logArg() {
    logger.info("* basedir = " + $("basedir"))
    logger.info("* dita.dir = " + ditaDir)
    logger.info("* transtype = " + transtype)
    logger.info("* tempdir = " + ditaTempDir)
    logger.info("* outputdir = " + outputDir)
    logger.info("* clean.temp = " + $("clean.temp"))
    logger.info("* DITA-OT version = " + $("otversion"))
    logger.info("* XML parser = " + $("xml.parser"))
    logger.info("* XSLT processor = " + $("xslt.parser"))
    logger.info("* collator = " + $("collator"))
  }

  /** Preprocessing ended */
  def preprocess() {
    logger.info("preprocess:")
    preprocessInit()
    genList()
    debugFilter()
    copyFiles()
    keyref()
    conrefpush()
    conref()
    topicFragment()
    coderef()
    mapref()
    moveMetaEntries()
    mappull()
    chunk()
    maplink()
    moveLinks()
    topicpull()
    flagModule()
    cleanMap()
  }

  private def preprocessInit() {
    logger.info("preprocess.init:")
    if ($.contains("args.input") && !$.contains("args.input.dir") && !new File($("args.input")).exists) {
      throw new IllegalArgumentException("DOTA069F")
    }
    if ($.contains("args.input") && $.contains("args.input.dir") && !(new File($("args.input")).exists || new File($("args.input.dir"), $("args.input")).exists)) {
      throw new IllegalArgumentException("DOTA069F")
    }
    if (!$.contains("args.input") && !$.contains("args.input.uri")) {
      throw new IllegalArgumentException("DOTA002F")
    }
    $("dita.input.filename") = new File($("args.input")).getName
    $("dita.map.filename.root") = new File($("dita.input.filename")).getName
    $("dita.topic.filename.root") = new File($("dita.input.filename")).getName
    logger.info("input = " + $("args.input"))
  }

  /** Clean temp directory */
  def cleanTemp() {
    logger.info("clean-temp:")
    if ($.contains("clean-temp.skip")) {
      return
    }

    delete(ditaTempDir, listAll(ditaTempDir))
  }

  /** Generate file list */
  def genList() {
    logger.info("gen-list:")
    if ($.contains("preprocess.gen-list.skip")) {
      return
    }

    val module = new GenMapAndTopicListModule
    module.setLogger(new DITAOTJavaLogger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("inputmap", $("args.input"))
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    if ($.contains("args.input.dir")) {
      modulePipelineInput.setAttribute("inputdir", $("args.input.dir"))
    }
    modulePipelineInput.setAttribute("ditadir", ditaDir)
    if ($.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", $("dita.input.valfile"))
    }
    modulePipelineInput.setAttribute("validate", $("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", $("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", $("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", $("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", outputDir)
    modulePipelineInput.setAttribute("transtype", transtype)
    modulePipelineInput.setAttribute("gramcache", $("args.grammar.cache"))
    modulePipelineInput.setAttribute("setsystemid", $("args.xml.systemid.set"))
    module.execute(modulePipelineInput)
  }

  /** Debug and filter input files */
  def debugFilter() {
    logger.info("debug-filter:")
    if ($.contains("preprocess.debug-filter.skip")) {
      return
    }

    val module = new DebugAndFilterModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    if ($.contains("dita.input.valfile")) {
      modulePipelineInput.setAttribute("ditaval", $("dita.input.valfile"))
    }
    modulePipelineInput.setAttribute("ditadir", ditaDir)
    modulePipelineInput.setAttribute("validate", $("validate"))
    modulePipelineInput.setAttribute("generatecopyouter", $("generate.copy.outer"))
    modulePipelineInput.setAttribute("outercontrol", $("outer.control"))
    modulePipelineInput.setAttribute("onlytopicinmap", $("onlytopic.in.map"))
    modulePipelineInput.setAttribute("outputdir", outputDir)
    modulePipelineInput.setAttribute("transtype", transtype)
    modulePipelineInput.setAttribute("setsystemid", $("args.xml.systemid.set"))
    module.execute(modulePipelineInput)
    $("_dita.map.output.dir") = new File(outputDir, job.getInputMap).getParent
    $("dita.map.output.dir") = new File($("_dita.map.output.dir"), job.getProperty("uplevels"))
  }

  /** Resolve conref push */
  def conrefpush() {
    logger.info("conrefpush:")
    if ($.contains("preprocess.conrefpush.skip")) {
      return
    }

    val module = new ConrefPushModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    module.execute(modulePipelineInput)
  }

  /** Move metadata entries */
  def moveMetaEntries() {
    logger.info("move-meta-entries:")
    if ($.contains("preprocess.move-meta-entries.skip")) {
      return
    }

    val module = new MoveMetaModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("inputmap", job.getInputMap)
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    module.execute(modulePipelineInput)
  }

  /** Resolve conref in input files */
  def conref() {
    logger.info("conref:")
    if ($.contains("preprocess.conref.skip")) {
      return
    }

    if (!job.getFileInfo.exists(_.hasConref)) {
      if (!$.contains("dita.preprocess.reloadstylesheet.conref")) {
        $("dita.preprocess.reloadstylesheet.conref") = $("dita.preprocess.reloadstylesheet")
      }
      $("exportfile.url") = new File(ditaTempDir, "export.xml").toURI.toASCIIString
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "conref.xsl"))
      val files = job.getFileInfo.filter(_.hasConref).map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.conref").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.conref").toBoolean) {
          transformer = templates.newTransformer()
        }
        transformer.setParameter("EXPORTFILE", $("exportfile.url"))
        transformer.setParameter("TRANSTYPE", transtype)
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        transformer.setParameter("file-being-processed", inFile.getName)
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  /** Resolve same topic fragment identifiers */
  def topicFragment() {
    logger.info("topic-fragment:")

    val module = new TopicFragmentModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    module.execute(modulePipelineInput)
  }

  /** Resolve coderef in input files */
  def coderef() {
    logger.info("coderef:")
    if ($.contains("preprocess.coderef.skip")) {
      return
    }

    if (job.getFileInfo.exists(_.hasCoderef)) {
      val module = new CoderefModule
      module.setLogger(logger)
      module.setJob(job)
      val modulePipelineInput = new PipelineHashIO
      modulePipelineInput.setAttribute("tempDir", ditaTempDir)
      module.execute(modulePipelineInput)
    }
  }

  /** Resolve mapref in ditamap */
  def mapref() {
    logger.info("mapref:")
    if ($.contains("preprocess.mapref.skip")) {
      return
    }

    val maps = job.getFileInfo.filter(_.format == "ditamap")
    if (maps.nonEmpty) {
      if (!$.contains("dita.preprocess.reloadstylesheet.mapref")) {
        $("dita.preprocess.reloadstylesheet.mapref") = $("dita.preprocess.reloadstylesheet")
      }
      $("mapref.workdir") = new File(ditaTempDir, job.getInputMap).getParent
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "mapref.xsl"))
      val files = maps.map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.mapref").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.mapref").toBoolean) {
          transformer = templates.newTransformer()
        }
        transformer.setParameter("TRANSTYPE", transtype)
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        transformer.setParameter("file-being-processed", inFile.getName)
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  /** Resolve keyref */
  def keyref() {
    logger.info("keyref:")
    if ($.contains("preprocess.keyref.skip")) {
      return
    }

    if (job.getFileInfo.exists(_.hasKeyref)) {
      val module = new KeyrefModule
      module.setLogger(logger)
      module.setJob(job)
      val modulePipelineInput = new PipelineHashIO
      modulePipelineInput.setAttribute("tempDir", ditaTempDir)
      module.execute(modulePipelineInput)
    }
  }

  /** Pull the navtitle and topicmeta from topics to ditamap */
  def mappull() {
    logger.info("mappull:")
    if ($.contains("preprocess.mappull.skip")) {
      return
    }

    val maps = job.getFileInfo.filter(_.format == "ditamap")
    if (maps.nonEmpty) {
      $("mappull.workdir") = new File(ditaTempDir, job.getInputMap).getParent
      if (!$.contains("dita.preprocess.reloadstylesheet.mappull")) {
        $("dita.preprocess.reloadstylesheet.mappull") = $("dita.preprocess.reloadstylesheet")
      }
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "mappull.xsl"))
      val files = maps.map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.mappull").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.mappull").toBoolean) {
          transformer = templates.newTransformer()
        }
        transformer.setParameter("TRANSTYPE", transtype)
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  /** Process chunks */
  def chunk() {
    logger.info("chunk:")
    if ($.contains("preprocess.chunk.skip")) {
      return
    }

    if (job.getFileInfo.exists(_.format == "ditamap")) {
      val module = new ChunkModule
      module.setLogger(logger)
      module.setJob(job)
      val modulePipelineInput = new PipelineHashIO
      modulePipelineInput.setAttribute("inputmap", job.getInputMap)
      modulePipelineInput.setAttribute("tempDir", ditaTempDir)
      modulePipelineInput.setAttribute("transtype", transtype)
      module.execute(modulePipelineInput)
    }
  }

  /** Find and generate related link information */
  def maplink() {
    logger.info("maplink:")
    if ($.contains("preprocess.maplink.skip")) {
      return
    }

    if (job.getFileInfo.exists(_.format == "ditamap")) {
      $("maplink.workdir") = new File(ditaTempDir, job.getInputMap).getParent
      if (!$.contains("dita.preprocess.reloadstylesheet.maplink")) {
        $("dita.preprocess.reloadstylesheet.maplink") = $("dita.preprocess.reloadstylesheet")
      }
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "maplink.xsl"))
      val inFile = new File(ditaTempDir, job.getInputMap)
      val outFile = new File($("maplink.workdir"), "maplinks.unordered")
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val transformer = templates.newTransformer()
      transformer.setParameter("INPUTMAP", job.getInputMap)
      if ($.contains("include.rellinks")) {
        transformer.setParameter("include.rellinks", $("include.rellinks"))
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.info("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /** Move the related link information to topics */
  def moveLinks() {
    logger.info("move-links:")
    if ($.contains("preprocess.move-links.skip")) {
      return
    }

    val module = new MoveLinksModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("inputmap", job.getInputMap)
    modulePipelineInput.setAttribute("tempDir", ditaTempDir)
    modulePipelineInput.setAttribute("maplinks", $("maplink.workdir") + "/maplinks.unordered")
    module.execute(modulePipelineInput)
  }

  /** Pull metadata for link and xref element */
  def topicpull() {
    logger.info("topicpull:")
    if ($.contains("preprocess.topicpull.skip")) {
      return
    }

    val topics = job.getFileInfo.filter(_.format == "dita")
    if (topics.nonEmpty) {
      if (!$.contains("dita.preprocess.reloadstylesheet.topicpull")) {
        $("dita.preprocess.reloadstylesheet.topicpull") = $("dita.preprocess.reloadstylesheet")
      }
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "topicpull.xsl"))
      val files = topics.map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.topicpull").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.topicpull").toBoolean) {
          transformer = templates.newTransformer()
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
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  /** Add flagging information to topics */
  def flagModule() {
    logger.info("flag-module:")
    if ($.contains("preprocess.flagging.skip")) {
      return
    }

    if (!$.contains("args.filter")) {
      return
    }
    val topics = job.getFileInfo.filter(f => f.format == "dita" && !f.isResourceOnly)
    if (topics.nonEmpty) {
      val filterFileUrl = new File($("args.filter")).toURI.toASCIIString
      if (!$.contains("dita.preprocess.reloadstylesheet.flag-module")) {
        $("dita.preprocess.reloadstylesheet.flag-module") = $("dita.preprocess.reloadstylesheet")
      }
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "flag.xsl"))
      val files = topics.map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.flag-module").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.flag-module").toBoolean) {
          transformer = templates.newTransformer()
        }
        transformer.setParameter("TRANSTYPE", transtype)
        transformer.setParameter("FILTERFILEURL", filterFileUrl)
        if ($.contains("args.draft")) {
          transformer.setParameter("DRAFT", $("args.draft"))
        }
        transformer.setParameter("BASEDIR", $("basedir"))
        transformer.setParameter("OUTPUTDIR", outputDir)
        if ($.contains("args.debug")) {
          transformer.setParameter("DBG", $("args.debug"))
        }
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        transformer.setParameter("FILENAME", inFile.getName)
        transformer.setParameter("FILEDIR", inFile.getParent)
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  /** Clean ditamap */
  def cleanMap() {
    logger.info("clean-map:")
    if ($.contains("preprocess.clean-map-check.skip")) {
      return
    }

    val maps = job.getFileInfo.filter(_.format == "ditamap")
    if (maps.nonEmpty) {
      if (!$.contains("dita.preprocess.reloadstylesheet.clean-map")) {
        $("dita.preprocess.reloadstylesheet.clean-map") = $("dita.preprocess.reloadstylesheet")
      }
      val templates = compileTemplates(new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "clean-map.xsl"))
      val files = maps.map(_.file).toSet
      var transformer: Transformer = if (!$("dita.preprocess.reloadstylesheet.clean-map").toBoolean) templates.newTransformer() else null
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.clean-map").toBoolean) {
          transformer = templates.newTransformer()
        }
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
  }

  def copyFiles() {
    logger.info("copy-files:")
    if ($.contains("preprocess.copy-files.skip")) {
      return
    }
    copyImage()
    copyHtml()
    copyFlag()
    copySubsidiary()
  }

  /** Copy image files */
  def copyImage() {
    logger.info("copy-image:")
    if ($.contains("preprocess.copy-image.skip")) {
      return
    }

    val images = job.getFileInfo.filter(_.format == "image")
    if (images.nonEmpty) {
      val copyImageTodir = if ($("generate.copy.outer") == "3") new File(outputDir, job.getProperty("uplevels")) else outputDir

      copy(new File(job.getInputMap),
        copyImageTodir,
        images.map(_.file.getPath).toSet)
    }
  }

  /** Copy html files */
  def copyHtml() {
    logger.info("copy-html:")
    if ($.contains("preprocess.copy-html.skip")) {
      return
    }

    val htmlFiles = job.getFileInfo.filter(_.format == "html")
    if (htmlFiles.nonEmpty) {
      copy(new File(job.getInputMap),
        outputDir,
        job.getFileInfo.filter(_.format == "html").map(_.file.getPath).toSet)
    }
  }

  /** Copy flag files */
  def copyFlag() {
    logger.info("copy-flag:")
    if ($.contains("preprocess.copy-flag.skip")) {
      return
    }

    if ($.contains("dita.input.valfile")) {
      ditaOtCopy(outputDir,
        new File(ditaTempDir, $("flagimagefile")),
        $("relflagimagelist").split(','))
    }
  }

  /** Copy subsidiary files */
  def copySubsidiary() {
    logger.info("copy-subsidiary:")
    if ($.contains("preprocess.copy-subsidiary.skip")) {
      return
    }

    val subTargets = job.getFileInfo.filter(_.isSubtarget) // job.getFileInfo.filter(_.format == "data")
    if (subTargets.nonEmpty) {
      copy(new File(job.getInputMap),
        ditaTempDir,
        subTargets.map(_.file.getPath).toSet)
    }
  }

}
