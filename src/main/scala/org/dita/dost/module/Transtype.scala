package org.dita.dost.module

import scala.collection.JavaConversions._

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import javax.xml.transform.TransformerFactory
import javax.xml.transform.Templates
import javax.xml.transform.Source
import javax.xml.transform.Result
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamResult

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.CatalogResolver
import org.apache.xml.resolver.tools.ResolvingXMLReader
import org.xml.sax.InputSource

import org.dita.dost.log.{DITAOTLogger, DITAOTJavaLogger}
import org.dita.dost.util.FileUtils
import org.dita.dost.util.Job
import org.dita.dost.util.Configuration.configuration

abstract class Transtype(ditaDir: File) {

  val $ = new Properties
  val depends = new History

  //$("basedir") = new File(".").getAbsoluteFile;
  $("basedir") = ditaDir.getAbsoluteFile.getCanonicalPath
  $("dita.dir") = ditaDir.getAbsoluteFile.getCanonicalPath
  // backwards compatibility
  $("file.separator") = File.separator
  for (key <- configuration.keySet if key.startsWith("plugin.") && key.endsWith(".dir")) {
    $("dita." + key) = new File(ditaDir, configuration.get(key)).getAbsoluteFile
  }

  val catalogManager = new CatalogManager()
  catalogManager.setCatalogFiles(new File(ditaDir, "catalog-dita.xml").toURI().toASCIIString)
  catalogManager.setPreferPublic(true)

  val logger = new DITAOTJavaLogger()

  val baseTempDir: File
  val ditaTempDir: File
//  val outputDir: File
  lazy val job = new Job(ditaTempDir)

  val transtype: String

  def run(): Unit

  implicit def fileToString(file: File): String = file.getAbsolutePath

  /**
   * Copy files by pattern.
   */
  def copy(src: File, dst: File, includes: Iterable[String]) {
    for (i <- resolvePatterns(src, includes)) {
      val s = new File(src, i)
      val d = new File(dst, i)
      if (s.exists()) {
        if (!d.getParentFile().exists) {
          d.getParentFile().mkdirs()
        }
        println("Copy " + s + " to " + d)
        FileUtils.copyFile(s, d)
      } else {
        println("Skip copy, " + s + " does not exist")
      }
    }
  }

  /**
   * Copy flag files.
   */
  def ditaOtCopy(out: File, flags: File, relFlags: Iterable[String]) {
    ditaOtCopy(out, readLines(flags), relFlags)
  }

  def ditaOtCopy(out: File, flags: Iterable[String], relFlags: Iterable[String]) {
    var b = new File($("dita.input.valfile")).getParentFile
    for (f <- relFlags) {
      val s = new File(b, f)
      val d = new File(out, f)
      logger.info("Copy " + s + " to " + d)
      FileUtils.copyFile(s, d)
    }
  }

  /**
   * ZIP files.
   */
  def zip(out: File, src: File, includes: Iterable[String]) {
    val o = new FileOutputStream(out)
    val zip = new ZipOutputStream(o)
    for (i <- resolvePatterns(src, includes)) {
      val s = new File(src, i)
      if (s.exists()) {
        val ss = new FileInputStream(s)
        val d = new ZipEntry(i)
        zip.putNextEntry(d)
        println("Zip " + s + " to " + d)
        FileUtils.copy(ss, zip)
        ss.close()
        zip.closeEntry()
      } else {
        println("Skip file, " + s + " does not exist")
      }
    }
    zip.close()
    o.close()
  }

  /**
   * Move files by pattern.
   */
  def move(src: File, dst: File, includes: Iterable[String]) {
    for (i <- resolvePatterns(src, includes)) {
      val s = new File(src, i)
      val d = new File(dst, i)
      if (s.exists()) {
        if (!d.getParentFile().exists()) {
          d.getParentFile().mkdirs()
        }
        println("Move " + s + " to " + d)
        s.renameTo(d)
      } else {
        println("Skip move, " + s + " does not exist")
      }
    }
  }

  /**
   * Resolve patterns.
   */
  private def resolvePatterns(dir: File, includes: Iterable[String]): List[String] = {
    // TODO: optimize for
    // **
    // **/*
    // **/*.ext
    // **/dir/**
    // dir/**
    // dir/dir/**/*.*
    // dir/dir/**
    val res = scala.collection.mutable.ListBuffer[String]()
    for (pattern <- includes) {
      if (pattern.charAt(0) == '*') {
        val ext = pattern.substring(1)
        res ++= dir.list().filter(f => f.endsWith(ext))
      } else {
        res += pattern
      }
    }
    res.toList
  }

  /**
   * Copy files by pattern file.
   */
  def copy(src: File, dst: File, includesfile: File) {
    val f = scala.io.Source.fromFile(includesfile, "UTF-8")
    copy(src, dst, f.getLines.toList)
    f.close()
  }

  /**
   * Read lines in a file.
   */
  def readLines(includesfile: File): Iterable[String] = {
    val f = scala.io.Source.fromFile(includesfile, "UTF-8")
    try {
      return f.getLines.toList
    } finally {
      f.close()
    }
  }

  /**
   * Delete files by pattern.
   */
  def delete(src: File, includes: Iterable[String]) {
    for (i <- resolvePatterns(src, includes)) {
      val s = new File(src, i)
      if (s.exists()) {
        logger.info("Delete " + s)
        s.delete()
      } else {
        logger.debug("Skip delete, " + s + " does not exist")
      }
    }
  }

  /**
   * Delete a file.
   */
  def delete(file: File) {
    if (file.exists()) {
      logger.info("Delete " + file)
      file.delete()
    } else {
      logger.debug("Skip delete, " + file + " does not exist")
    }
  }

  def listAll(dir: File): Set[String] = {
    // TODO: return Set("**")
    dir.list() match {
      case null => Set.empty
      case files => files.toSet
    }
  }

  /**
   * Read list file
   *
   * @param file list file
   * @return lines in the file
   */
  def readList(file: File): List[String] = {
    val includes_file = scala.io.Source.fromFile(file, "UTF-8")
    val files: List[String] = includes_file.getLines().toList
    includes_file.close()
    return files
  }

  /**
   * Join path parts into a single file
   */
  def join(paths: String*): String = {
    return new File(paths.mkString(File.separator)).getAbsolutePath()
  }

  def globMap(input: String, from: String, to: String): String = {
    val i = input.indexOf(from)
    if (i != -1 && from.length != 0) {
      return input.substring(0, i) + to + input.substring(i + from.length)
    } else {
      return input
    }
  }

  def class_available(cls: String): Boolean = {
    try {
      Class.forName(cls)
      true
    } catch {
      case ex: ClassNotFoundException => false
    }
  }

  def compileTemplates(style: File): Templates = {
    val factory = TransformerFactory.newInstance()
    factory.setURIResolver(new CatalogResolver(catalogManager))
    factory.newTemplates(getSource(style))
  }

  def getSource(file: File): Source = {
    //new StreamSource(file)
    new SAXSource(new ResolvingXMLReader(catalogManager), new InputSource(file.toURI().toASCIIString()))
  }

  def getResult(file: File): Result = {
    new StreamResult(file)
  }

  // FIXME
  def get_msg(id: String): String = {
    return ""
  }

}

object OsPath {

  def splitext(path: String): (String, String) = {
    val dot = path.lastIndexOf('.')
    return (path.substring(0, dot), path.substring(dot))
  }

}

/**
 * Global properties store.
 */
class Properties {

  val m = scala.collection.mutable.Map[String, String]()
  m("basedir") = new File(".").getAbsolutePath()
  m("ant.file.DOST") = new File(m("basedir"), "build.xml").getAbsolutePath()

  def update(key: String, value: String) {
    if (!m.contains(key)) {
      m(key) = value
    }
  }

  def update(key: String, value: File) {
    if (!m.contains(key)) {
      m(key) = value.getAbsolutePath
    }
  }

  def apply(key: String): String = {
    if (m.contains(key)) {
      return m(key)
    } else {
      return "${" + key + "}"
    }
  }

  def contains(key: String): Boolean = {
    return m.contains(key)
  }

  /**
   * Read XML property file to global properties. Unlike {@link #readProperties(File)}, this will override existing values.
   */
  def readXmlProperties(props: File) {
    if (props.exists()) {
      val f = new FileInputStream(props)
      val p = new java.util.Properties()
      p.loadFromXML(f)
      f.close()
      for (k <- p.keySet) {
        m(k.toString) = p.get(k).toString
      }
    }
  }

  /**
   * Read property file to global properties.
   */
  def readProperties(props: File) {
    if (props.exists()) {
      val f = new FileInputStream(props)
      val p = new java.util.Properties()
      p.load(f)
      f.close()
      for (k <- p.keySet) {
        if (!m.contains(k.toString)) {
          m(k.toString) = p.get(k).toString
        }
      }
    }
  }

}

class History {

  val h = scala.collection.mutable.Set[String]()

  /**
   * Run dependencies. Because you can't compare functions for identity, we
   * pass in the original Ant name of the task.
   */
  def apply(funcs: (String, () => Unit)*) {
    for (f <- funcs) {
      if (!h.contains(f._1)) {
        h += f._1
        f._2()
      } else {
        throw new IllegalStateException("Repeated call to " + f._1)
      }
    }
  }

}