package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File

import javax.xml.transform.Transformer

import org.dita.dost.util.Job.Generate._
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.util.FileUtils._

abstract class Preprocess(ditaDir: File) extends Transtype(ditaDir) {

  $("ant.file.ditaot-preprocess") = new File("plugins/org.dita.base/build_preprocess.xml")

  var innerTransform: Boolean = false
  var oldTransform: Boolean = false
  var noPlugin: Boolean = false

  $.readProperties(new File(ditaDir, "lib" + File.separator + "org.dita.dost.platform" + File.separator + "plugin.properties"))
  $.readProperties(new File(ditaDir, "lib" + File.separator + "configuration.properties"))
  override val baseTempDir = new File("/Volumes/tmp/temp") //new File($("basedir"), "temp")
  override val ditaTempDir = new File(baseTempDir, "temp" + System.currentTimeMillis)
  var outputDir: File = null


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
    outputDir = if ($.contains("output.dir")) new File($("output.dir")) else new File($("basedir"), "out")
    if ($.contains("args.filter") && !$.contains("dita.input.valfile")) {
      $("dita.input.valfile") = $("args.filter")
    }
    if ($.contains("args.outext")) {
      $("out.ext") = (if ($("args.outext").startsWith(".")) "" else ".") + $("args.outext")
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
    if (!$.contains("validate")) {
      $("validate") = true
    }
    if (!$.contains("include.rellinks")) {
      if ($("args.rellinks") == "none") {
        $("include.rellinks") = ""
      } else if ($("args.rellinks") == "nofamily") {
        $("include.rellinks") = "#default sibling friend cousin ancestor descendant sample external other"
      } else if ($("args.hide.parent.link") == "yes") {
        $("include.rellinks") = "#default child sibling friend next previous cousin ancestor descendant sample external other"
      } else { //if ($("args.rellinks") == "all" || !$.contains("args.rellinks")) {
        $("include.rellinks") = "#default parent child sibling friend next previous cousin ancestor descendant sample external other"
      }
    }
    if (!$.contains("onlytopic.in.map")) {
      $("onlytopic.in.map") = false
    }
    if (!$.contains("outer.control")) {
      $("outer.control") = "warn"
    }
    if (!$.contains("generate.copy.outer")) {
      $("generate.copy.outer") = NOT_GENERATEOUTTER.toString
    }
    innerTransform = $("generate.copy.outer") == "1"
    oldTransform = !innerTransform
    if (!$.contains("conserve-memory")) {
      $("conserve-memory") = false
    }
    if (!$.contains("dita.preprocess.reloadstylesheet") && $.contains("conserve-memory")) {
      $("dita.preprocess.reloadstylesheet") = $("conserve-memory")
    }
  }

  private def logArg() {
    logger.info("* basedir = " + $("basedir"))
    logger.info("* dita.dir = " + ditaDir)
    logger.info("* transtype = " + transtype)
    logger.info("* tempdir = " + ditaTempDir)
    logger.info("* outputdir = " + outputDir)
    logger.info("* clean.temp = " + $("clean.temp"))
  }

  /** Preprocessing ended */
  def preprocess() {
    logger.info("preprocess:")
    preprocessInit()
    genList()
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
    //moveLinks()
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
    $("dita.map.filename.root") = getBaseName($("dita.input.filename"))
    $("dita.topic.filename.root") = getBaseName($("dita.input.filename"))
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

    val module = new GenMapAndTopicListModule
    module.setLogger(logger)
    module.setJob(job)
    val modulePipelineInput = new PipelineHashIO
    modulePipelineInput.setAttribute("inputmap", $("args.input"))
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

    logger.info("debug-filter:")

    val module2 = new DebugAndFilterModule
    module2.setLogger(logger)
    module2.setJob(job)
    module2.execute(modulePipelineInput)

    $("dita.map.output.dir") = new File(new File(outputDir, job.getInputMap).getParentFile, job.getProperty("uplevels"))
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
    module.execute(modulePipelineInput)
  }

  /** Resolve conref in input files */
  def conref() {
    logger.info("conref:")
    if ($.contains("preprocess.conref.skip")) {
      return
    }

    if (job.getFileInfo.exists(_.hasConref)) {
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
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
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
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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
      //files.foreach { l =>
      for (l <- files) {
        if ($("dita.preprocess.reloadstylesheet.mapref").toBoolean) {
          transformer = templates.newTransformer()
        }
        transformer.setParameter("TRANSTYPE", transtype)
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
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
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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
        if ($("conserve-memory").toBoolean) {
          transformer.setParameter("conserve-memory", true)
        }
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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
      modulePipelineInput.setAttribute("transtype", transtype)
      if ($.contains("root-chunk-override")) {
        modulePipelineInput.setAttribute("root-chunk-override", $("root-chunk-override"))
      }
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
      logger.info("move-links:")
      val module = new MoveLinksModule
      module.setLogger(logger)
      module.setJob(job)
      val modulePipelineInput = new PipelineHashIO
      modulePipelineInput.setAttribute("inputmap", job.getInputMap)
      modulePipelineInput.setAttribute("style", new File($("dita.plugin.org.dita.base.dir"), "xsl" + File.separator + "preprocess" + File.separator + "maplink.xsl"))
      if ($.contains("include.rellinks")) {
        modulePipelineInput.setAttribute("include.rellinks", $("include.rellinks"))
      }
      module.execute(modulePipelineInput)
    }
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
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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
        transformer.setParameter("OUTPUTDIR", outputDir)
        if ($.contains("args.debug")) {
          transformer.setParameter("DBG", $("args.debug"))
        }
        val inFile = new File(ditaTempDir, l.getPath)
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
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
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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
        val outFile = new File(ditaTempDir, l.getPath + ".tmp")
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.info("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(ditaTempDir, l.getPath + ".tmp")
        val dst = new File(ditaTempDir, l.getPath)
        moveFile(src, dst)
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

    val filterPrefix = if (oldTransform) {
      ""
    } else {
      new File(job.getInputMap).getParent match {
        case null => ""
        case p => p + File.separator
      }
    }
    val images = job.getFileInfo.filter(f => f.format == "image" && f.file.getPath.startsWith(filterPrefix))
    if (images.nonEmpty) {
      val src = new File(job.getInputDir)
      val dst = outputDir
      logger.info("Copying " + images.size + " images to " + dst.getAbsolutePath)
      for (l <- images.map(_.file.getPath)) {
        val s = new File(src, l)
        val d = new File(dst, l.substring(filterPrefix.length))
        if (s.exists()) {
          if (!d.getParentFile().exists) {
            d.getParentFile().mkdirs()
          }
          logger.debug("Copy " + s + " to " + d)
          copyFile(s, d)
        } else {
          logger.debug("Skip copy, " + s + " does not exist")
        }
      }

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
      copy(new File(job.getInputDir),
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
      copy(new File(job.getInputDir),
        ditaTempDir,
        subTargets.map(_.file.getPath).toSet)
    }
  }

}
