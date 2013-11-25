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
import org.dita.dost.util.FileUtils

class Troff(ditaDir: File) extends Preprocess(ditaDir) {

  $("ant.file.dita2troff") = new File("plugins/org.dita.troff/build_dita2troff.xml")
  override val transtype = "troff"


  def dita2troffInit() {
    logger.logInfo("dita2troff.init:")
    $("out.ext") = ".cli"
    $("troff.step1.xsl") = new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step1-shell.xsl")
    $("troff.step2.xsl") = new File($("dita.plugin.org.dita.troff.dir") + File.separator + "xsl" + File.separator + "dita2troff-step2-shell.xsl")
  }

  override def run() {
    logger.logInfo("run:")
    depends(("dita2troff.init", dita2troffInit), ("build-init", buildInit), ("preprocess", preprocess), ("dita.topic.troff", ditaTopicTroff), ("dita.inner.topic.troff", ditaInnerTopicTroff), ("dita.outer.topic.troff", ditaOuterTopicTroff))
  }

  /** Build troff output from dita inner and outer topics,which will adjust the directory. */
  def ditaTopicTroff() {
    logger.logInfo("dita.topic.troff:")
    if (!oldTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    try {
      val templates = compileTemplates(new File($("troff.step1.xsl")))
      val baseDir = ditaTempDir
      val destDir = ditaTempDir
      val tempExt = ".dita"
      val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
      for (l <- files) {
        val transformer = templates.newTransformer()
        val inFile = new File(baseDir, l.getPath)
        val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.logInfo("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        val dst = new File(baseDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
    val templates = compileTemplates(new File($("troff.step2.xsl")))
    val baseDir = ditaTempDir
    val destDir = outputDir
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("troff.outformat")) {
        transformer.setParameter("OUTFORMAT", $("troff.outformat"))
      }
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /** Build troff output from inner dita topics */
  def ditaInnerTopicTroff() {
    logger.logInfo("dita.inner.topic.troff:")
    if (!innerTransform) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    try {
      val templates = compileTemplates(new File($("troff.step1.xsl")))
      val baseDir = ditaTempDir
      val destDir = ditaTempDir
      val tempExt = ".dita"
      val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
      for (l <- files) {
        val transformer = templates.newTransformer()
        val inFile = new File(baseDir, l.getPath)
        val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.logInfo("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        val dst = new File(baseDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
    logger.logInfo("the ditmapoutputdir is " + $("dita.map.output.dir"))
    val templates = compileTemplates(new File($("troff.step2.xsl")))
    val baseDir = ditaTempDir
    val destDir = outputDir
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.format == "dita").map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("troff.outformat")) {
        transformer.setParameter("OUTFORMAT", $("troff.outformat"))
      }
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  /** Build troff output from outer dita topics */
  def ditaOuterTopicTroff() {
    logger.logInfo("dita.outer.topic.troff:")
    depends(("troff.checkouterTransform", troffCheckouterTransform))
    if (!$.contains("outer.transform")) {
      return
    }
    if (job.getFileInfo.find(_.format == "dita").isEmpty) {
      return
    }

    try {
      val templates = compileTemplates(new File($("troff.step1.xsl")))
      val baseDir = ditaTempDir
      val destDir = ditaTempDir
      val tempExt = ".dita"
      val files = job.getFileInfo.filter(_.isOutDita).map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
      for (l <- files) {
        val transformer = templates.newTransformer()
        val inFile = new File(baseDir, l.getPath)
        val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        if (!outFile.getParentFile.exists) {
          outFile.getParentFile.mkdirs()
        }
        val source = getSource(inFile)
        val result = getResult(outFile)
        logger.logInfo("Processing " + inFile + " to " + outFile)
        transformer.transform(source, result)
      }
      for (l <- files) {
        val src = new File(destDir, FileUtils.replaceExtension(l, tempExt))
        val dst = new File(baseDir, l.getPath)
        FileUtils.moveFile(src, dst)
      }
    }
    val templates = compileTemplates(new File($("troff.step2.xsl")))
    val baseDir = outputDir
    val destDir = new File(outputDir + File.separator + job.getProperty("uplevels"))
    val tempExt = $("out.ext")
    val files = job.getFileInfo.filter(_.isOutDita).map(_.file).toSet -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
    for (l <- files) {
      val transformer = templates.newTransformer()
      if ($.contains("troff.outformat")) {
        transformer.setParameter("OUTFORMAT", $("troff.outformat"))
      }
      val inFile = new File(baseDir, l.getPath)
      val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
      if (!outFile.getParentFile.exists) {
        outFile.getParentFile.mkdirs()
      }
      val source = getSource(inFile)
      val result = getResult(outFile)
      logger.logInfo("Processing " + inFile + " to " + outFile)
      transformer.transform(source, result)
    }
  }

  def troffCheckouterTransform() {
    logger.logInfo("troff.checkouterTransform:")
    if (($("generate.copy.outer") == "2" && ($.contains("outditafileslist") && "" != $("outditafileslist")))) {
      $("outer.transform") = "true"
    }
  }
}
