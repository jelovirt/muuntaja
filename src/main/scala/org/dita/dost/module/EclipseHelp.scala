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

class EclipseHelp(ditaDir: File) extends XHTML(ditaDir) {

$("ant.file.dita2eclipsehelp") = new File("")
override val transtype = "eclipsehelp"


def ditaEclipsehelpInit() {
logger.logInfo("dita.eclipsehelp.init:")
$("html-version") = "xhtml"
if (!$.contains("args.xsl")) {
$("args.xsl") = $("dita.plugin.org.dita.eclipsehelp.dir") + "/xsl/dita2xhtml_eclipsehelp.xsl"}
}

def ditaIndexEclipsehelpInit() {
logger.logInfo("dita.index.eclipsehelp.init:")
if (!$.contains("dita.eclipsehelp.index.class")) {
$("dita.eclipsehelp.index.class") = "org.dita.dost.writer.EclipseIndexWriter"}
}

override def run() {
logger.logInfo("run:")
depends(("build-init", buildInit), ("dita.eclipsehelp.init", ditaEclipsehelpInit), ("preprocess", preprocess), ("copy-css", copyCss), ("xhtml.topics", xhtmlTopics))
if (job.getFileInfo().values.find(_.format == "ditamap").isEmpty) {
return}

ditaMapEclipse()
}

def ditaMapEclipse() {
logger.logInfo("dita.map.eclipse:")
depends(("dita.map.eclipse.init", ditaMapEclipseInit), ("copy-plugin-files", copyPluginFiles), ("dita.map.eclipse.fragment.language.init", ditaMapEclipseFragmentLanguageInit), ("dita.map.eclipse.fragment.language.country.init", ditaMapEclipseFragmentLanguageCountryInit), ("dita.map.eclipse.fragment.error", ditaMapEclipseFragmentError))
}

def ditaMapEclipseInit() {
logger.logInfo("dita.map.eclipse.init:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!$.contains("eclipse.plugin")) {
return}

ditaMapEclipseGeneratePlugin()
}

/**Init properties for EclipseHelp */
def ditaMapEclipsePluginInit() {
logger.logInfo("dita.map.eclipse.plugin.init:")
$("dita.map.toc.root") = new File($("dita.input.filename")).getName()
if (!$.contains("args.eclipsehelp.toc")) {
$("args.eclipsehelp.toc") = $("dita.map.toc.root")}
if (!$.contains("out.ext")) {
$("out.ext") = ".html"}
if ($("dita.eclipse.plugin")=="no") {
noPlugin = true}
if (($.contains("args.eclipsehelp.language") && !$.contains("args.eclipsehelp.country"))) {
$("eclipse.fragment.language") = "true"}
if (($.contains("args.eclipsehelp.language") && $.contains("args.eclipsehelp.country"))) {
$("eclipse.fragment.country") = "true"}
if (!(($.contains("args.eclipsehelp.language") || $.contains("args.eclipsehelp.country") || ($.contains("args.eclipsehelp.country") && $.contains("args.eclipsehelp.language"))))) {
$("eclipse.plugin") = "true"}
if (($.contains("args.eclipsehelp.country") && !$.contains("args.eclipsehelp.language"))) {
$("eclipse.fragment.error") = "true"}
if (!$.contains("args.eclipsehelp.indexsee")) {
$("args.eclipsehelp.indexsee") = "false"}
}

/**Build EclipseHelp TOC file */
def ditaMapEclipseToc() {
logger.logInfo("dita.map.eclipse.toc:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!oldTransform) {
return}
if (job.getFileInfo().values.find(_.format == "ditamap").isEmpty) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
val baseDir = new File($("dita.temp.dir"))
val destDir = new File($("output.dir"))
val tempExt = ".xml"
val files = job.getSet("fullditamaplist") -- job.getSet("resourceonlylist")
for (l <- files) {
val transformer = templates.newTransformer()
if ($.contains("out.ext")) {
transformer.setParameter("OUTEXT", $("out.ext"))
}
if ($.contains("workdir")) {
transformer.setParameter("WORKDIR", $("workdir"))
}
val inFile = new File(baseDir, l)
val outFile = new File(destDir, FileUtils.replaceExtension(l, tempExt))
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)}
}

/**Build EclipseHelp TOC file */
def ditaOutMapEclipseToc() {
logger.logInfo("dita.out.map.eclipse.toc:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!innerTransform) {
return}
if (job.getFileInfo().values.find(_.format == "ditamap").isEmpty) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2eclipse.xsl"))
val baseDir = new File($("dita.temp.dir"))
val destDir = new File($("output.dir"))
val files = job.getSet("fullditamaplist") -- job.getSet("resourceonlylist")
for (l <- files) {
val transformer = templates.newTransformer()
if ($.contains("out.ext")) {
transformer.setParameter("OUTEXT", $("out.ext"))
}
if ($.contains("workdir")) {
transformer.setParameter("WORKDIR", $("workdir"))
}
val inFile = new File(baseDir, l)
val outFile = new File(globMap(new File(destDir, l).getAbsolutePath(), "^(" + $("tempdirToinputmapdir.relative.value") + ")(.*?)(\\.ditamap)$$", "\\2\\.xml"))
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)}
}

/**Build Eclipse Help index file */
def ditaMapEclipseIndex() {
logger.logInfo("dita.map.eclipse.index:")
depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
if (!oldTransform) {
return}
if (job.getFileInfo().values.find(_.format == "ditamap").isEmpty) {
return}

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
logger.logInfo("dita.out.map.eclipse.index:")
depends(("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit), ("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit), ("dita.index.eclipsehelp.init", ditaIndexEclipsehelpInit))
if (!innerTransform) {
return}
if (job.getFileInfo().values.find(_.format == "ditamap").isEmpty) {
return}

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
logger.logInfo("dita.map.eclipse.plugin:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!oldTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("dita.map.output.dir") + File.separator + "plugin.xml")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
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
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

/**Build Eclipsehelp plugin file */
def ditaOutMapEclipsePlugin() {
logger.logInfo("dita.out.map.eclipse.plugin:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!innerTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("output.dir") + File.separator + "plugin.xml")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
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
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

/**Build Eclipsehelp manifest.mf file */
def ditaMapEclipseManifestFile() {
logger.logInfo("dita.map.eclipse.manifest.file:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!oldTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
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
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

/**Build Eclipsehelp manifest.mf file */
def ditaOutMapEclipseManifestFile() {
logger.logInfo("dita.out.map.eclipse.manifest.file:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!innerTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("dita.map.output.dir") + File.separator + "META-INF" + File.separator + "MANIFEST.MF")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
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
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

/**Create eclipse plugin.properties file */
def ditaMapEclipsePluginProperties() {
logger.logInfo("dita.map.eclipse.plugin.properties:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!oldTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("output.dir") + File.separator + "plugin.properties")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
val transformer = templates.newTransformer()
transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
if ($.contains("args.eclipse.version")) {
transformer.setParameter("version", $("args.eclipse.version"))
}
if ($.contains("args.eclipse.provider")) {
transformer.setParameter("provider", $("args.eclipse.provider"))
}
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

/**Create eclipse plugin.properties file */
def ditaOutMapEclipsePluginProperties() {
logger.logInfo("dita.out.map.eclipse.plugin.properties:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!innerTransform) {
return}
if (noPlugin) {
return}

val templates = compileTemplates(new File($("dita.plugin.org.dita.eclipsehelp.dir") + File.separator + "xsl" + File.separator + "map2plugin.xsl"))
val inFile = new File($("dita.temp.dir") + File.separator + job.getProperty(INPUT_DITAMAP))
val outFile = new File($("output.dir") + File.separator + "plugin.properties")
if (!outFile.getParentFile().exists()) {
outFile.getParentFile().mkdirs()}
val transformer = templates.newTransformer()
transformer.setParameter("dita.plugin.output", "dita.eclipse.properties")
if ($.contains("args.eclipse.version")) {
transformer.setParameter("version", $("args.eclipse.version"))
}
if ($.contains("args.eclipse.provider")) {
transformer.setParameter("provider", $("args.eclipse.provider"))
}
val source = getSource(inFile)
val result = new StreamResult(outFile)
logger.logInfo("Processing " + inFile + " to " + outFile)
transformer.transform(source, result)
}

def ditaMapEclipseFragmentLanguageInit() {
logger.logInfo("dita.map.eclipse.fragment.language.init:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!$.contains("eclipse.fragment.language")) {
return}

$("fragment.dirname.init") = "nl"
$("fragment.dirname") = $("fragment.dirname.init") + $("file.separator") + $("args.eclipsehelp.language")
$("fragment.property.name") = $("args.eclipsehelp.language")
ditaMapEclipseGenetrateFragment()
}

def ditaMapEclipseFragmentLanguageCountryInit() {
logger.logInfo("dita.map.eclipse.fragment.language.country.init:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!$.contains("eclipse.fragment.country")) {
return}

$("fragment.dirname.init") = "nl"
$("fragment.dirname") = $("fragment.dirname.init") + $("file.separator") + $("args.eclipsehelp.language") + $("file.separator") + $("args.eclipsehelp.country")
$("fragment.property.name") = $("args.eclipsehelp.language") + "_" + $("args.eclipsehelp.country")
ditaMapEclipseGenetrateFragment()
}

def ditaMapEclipseFragmentError() {
logger.logInfo("dita.map.eclipse.fragment.error:")
depends(("dita.map.eclipse.plugin.init", ditaMapEclipsePluginInit))
if (!$.contains("eclipse.fragment.error")) {
return}

logger.logInfo("")
}

def ditaMapEclipseFragmentMoveFiles() {
logger.logInfo("dita.map.eclipse.fragment.move.files:")
if (!oldTransform) {
return}

delete(new File($("output.dir") + File.separator + "plugin.xml"))
delete(new File($("output.dir") + File.separator + "plugincustomization.ini"))
move(new File($("dita.map.output.dir")), new File($("dita.map.output.dir") + File.separator + $("fragment.dirname")), listAll(new File($("dita.map.output.dir"))) -- Set("helpData.xml") -- Set("plugin.properties") -- Set("plugin_${fragment.property.name}.properties") -- Set("*.list") -- Set("disabled_book.css") -- Set("narrow_book.css") -- Set("${os}_narrow_book.css") -- Set("book.css") -- Set("plugincustomization.ini"))
}

def ditaOutMapEclipseFragmentMoveFiles() {
logger.logInfo("dita.out.map.eclipse.fragment.move.files:")
if (!innerTransform) {
return}

delete(new File($("output.dir") + File.separator + "plugin.xml"))
delete(new File($("output.dir") + File.separator + "plugincustomization.ini"))
move(new File($("output.dir")), new File($("output.dir") + File.separator + $("fragment.dirname")), listAll(new File($("output.dir"))) -- Set("helpData.xml") -- Set("plugin.properties") -- Set("plugin_${fragment.property.name}.properties") -- Set("*.list") -- Set("disabled_book.css") -- Set("narrow_book.css") -- Set("${os}_narrow_book.css") -- Set("book.css") -- Set("plugincustomization.ini"))
}

def ditaMapEclipseGeneratePlugin() {
logger.logInfo("dita.map.eclipse.generate.plugin:")
depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin", ditaMapEclipsePlugin), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin))
}

def ditaMapEclipseGenetrateFragment() {
logger.logInfo("dita.map.eclipse.genetrate.fragment:")
depends(("dita.map.eclipse.toc", ditaMapEclipseToc), ("dita.map.eclipse.index", ditaMapEclipseIndex), ("dita.map.eclipse.plugin.properties", ditaMapEclipsePluginProperties), ("dita.map.eclipse.manifest.file", ditaMapEclipseManifestFile), ("dita.out.map.eclipse.plugin.properties", ditaOutMapEclipsePluginProperties), ("dita.out.map.eclipse.manifest.file", ditaOutMapEclipseManifestFile), ("dita.out.map.eclipse.toc", ditaOutMapEclipseToc), ("dita.out.map.eclipse.index", ditaOutMapEclipseIndex), ("dita.out.map.eclipse.plugin", ditaOutMapEclipsePlugin), ("dita.map.eclipse.fragment.move.files", ditaMapEclipseFragmentMoveFiles), ("dita.out.map.eclipse.fragment.move.files", ditaOutMapEclipseFragmentMoveFiles))
}

def copyPluginFiles() {
logger.logInfo("copy-plugin-files:")
copy(new File(job.getProperty(INPUT_DIR)), new File($("output.dir")), Set("disabled_book.css") ++ Set("narrow_book.css") ++ Set("${os}_narrow_book.css") ++ Set("book.css") ++ Set("plugincustomization.ini") ++ Set("helpData.xml"))
}
}
