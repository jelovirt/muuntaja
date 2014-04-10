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

class XHTML(ditaDir: File) extends XHTMLBase(ditaDir) {

$("ant.file.dita2xhtml") = new File("plugins/org.dita.xhtml/build_dita2xhtml.xml")
override val transtype = "xhtml"


def dita2html5Init() {
logger.info("dita2html5.init:")
$("html-version") = "html5"
}

def dita2html5() {
logger.info("dita2html5:")
depends(("dita2html5.init", dita2html5Init), ("build-init", buildInit), ("preprocess", preprocess), ("copy-css", copyCss), ("xhtml.topics", xhtmlTopics), ("dita.map.xhtml", ditaMapXhtml))
}

def dita2xhtmlInit() {
logger.info("dita2xhtml.init:")
$("html-version") = "xhtml"
}

override def run() {
logger.info("run:")
depends(("dita2xhtml.init", dita2xhtmlInit), ("build-init", buildInit), ("preprocess", preprocess), ("copy-css", copyCss), ("xhtml.topics", xhtmlTopics), ("dita.map.xhtml", ditaMapXhtml))
}

def ditaMapXhtml() {
logger.info("dita.map.xhtml:")
depends(("dita.map.xhtml.init", ditaMapXhtmlInit), ("dita.map.xhtml.toc", ditaMapXhtmlToc), ("dita.out.map.xhtml.toc", ditaOutMapXhtmlToc))
}

def ditaMapXhtmlInit() {
logger.info("dita.map.xhtml.init:")
if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
return}

if (!$.contains("args.xhtml.toc.xsl")) {
$("args.xhtml.toc.xsl") = $("dita.plugin.org.dita.xhtml.dir") + "/xsl/map2" + $("html-version") + "toc.xsl"}
if (!$.contains("args.xhtml.toc")) {
$("args.xhtml.toc") = "index"}
}

/** Build HTML TOC file */
def ditaMapXhtmlToc() {
logger.info("dita.map.xhtml.toc:")
if (!oldTransform) {
return}
if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
return}

val templates = compileTemplates(new File($("args.xhtml.toc.xsl")))
val baseDir = ditaTempDir
val destDir = outputDir
val files = Set(new File(job.getInputMap)) -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
for (l <- files) {
val transformer = templates.newTransformer()
if ($.contains("out.ext")) {
transformer.setParameter("OUTEXT", $("out.ext"))
}
if ($.contains("args.xhtml.contenttarget")) {
transformer.setParameter("contenttarget", $("args.xhtml.contenttarget"))
}
if ($.contains("args.css.file")) {
transformer.setParameter("CSS", $("args.css.file"))
}
if ($.contains("user.csspath")) {
transformer.setParameter("CSSPATH", $("user.csspath"))
}
if ($.contains("args.xhtml.toc.class")) {
transformer.setParameter("OUTPUTCLASS", $("args.xhtml.toc.class"))
}
val inFile = new File(baseDir, l.getPath)
val outFile = new File(globMap(new File(destDir, l.getPath).getAbsolutePath, "*" + $("dita.input.filename"), "*" + $("args.xhtml.toc") + $("out.ext")))
if (!outFile.getParentFile.exists) {
outFile.getParentFile.mkdirs()}
val source = getSource(inFile)
val result = getResult(outFile)
logger.info("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)}
for (l <- files) {
val src = new File(globMap(new File(destDir, l.getPath).getAbsolutePath, "*" + $("dita.input.filename"), "*" + $("args.xhtml.toc") + $("out.ext")))
val dst = new File(baseDir, l.getPath)
FileUtils.moveFile(src, dst)}
}

/** Build HTML TOC file,which will adjust the directory */
def ditaOutMapXhtmlToc() {
logger.info("dita.out.map.xhtml.toc:")
if (!innerTransform) {
return}
if (job.getFileInfo.find(_.format == "ditamap").isEmpty) {
return}

val templates = compileTemplates(new File($("args.xhtml.toc.xsl")))
val baseDir = ditaTempDir
val destDir = outputDir
val files = Set(new File(job.getInputMap)) -- job.getFileInfo.filter(_.isResourceOnly).map(_.file).toSet
for (l <- files) {
val transformer = templates.newTransformer()
if ($.contains("out.ext")) {
transformer.setParameter("OUTEXT", $("out.ext"))
}
if ($.contains("args.xhtml.contenttarget")) {
transformer.setParameter("contenttarget", $("args.xhtml.contenttarget"))
}
if ($.contains("args.css.file")) {
transformer.setParameter("CSS", $("args.css.file"))
}
if ($.contains("user.csspath")) {
transformer.setParameter("CSSPATH", $("user.csspath"))
}
if ($.contains("args.xhtml.toc.class")) {
transformer.setParameter("OUTPUTCLASS", $("args.xhtml.toc.class"))
}
val inFile = new File(baseDir, l.getPath)
val outFile = new File(globMap(new File(destDir, l.getPath).getAbsolutePath, job.getInputMap(), $("args.xhtml.toc") + $("out.ext")))
if (!outFile.getParentFile.exists) {
outFile.getParentFile.mkdirs()}
val source = getSource(inFile)
val result = getResult(outFile)
logger.info("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)}
for (l <- files) {
val src = new File(globMap(new File(destDir, l.getPath).getAbsolutePath, job.getInputMap(), $("args.xhtml.toc") + $("out.ext")))
val dst = new File(baseDir, l.getPath)
FileUtils.moveFile(src, dst)}
}

def copyRevflag() {
logger.info("copy-revflag:")
if (!$.contains("dita.input.valfile")) {
return}

logger.info(get_msg("DOTA069W"))
}

/** Copy CSS files */
def copyCss() {
logger.info("copy-css:")
if ($.contains("user.csspath.url")) {
return}

if (($("args.copycss")=="yes" && $.contains("args.css.present"))) {
$("user.copycss.yes") = "true"}
$("user.csspath.real") = new File(outputDir + File.separator + $("user.csspath"))
if (!new File($("user.csspath.real")).exists) {
new File($("user.csspath.real")).mkdirs()}
copy(new File($("dita.plugin.org.dita.xhtml.dir") + File.separator + "resource"), new File($("user.csspath.real")), Set("*.css"))
copyCssUser()
}

def copyCssUser() {
logger.info("copy-css-user:")
if (!$.contains("user.copycss.yes")) {
return}

}
}
