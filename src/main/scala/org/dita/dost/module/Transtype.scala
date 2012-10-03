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
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

class Transtype(ditaDir: File) {

  Properties("dita.dir") = ditaDir.getAbsolutePath()

  /**
   * Copy files by pattern.
   */
  def copy(src: String, dst: String, includes: String) {
    for (i <- includes.split(",")) {
      val s = new File(src, i)
      val d = new File(dst, i)
      if (s.exists()) {
        if (!d.getParentFile().exists()) {
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

  /**
   * Copy files by pattern file.
   */
  def copy_list(src: String, dst: String, includesfile: String) {
    val f = Source.fromFile(includesfile, "UTF-8")
    for (l <- f.getLines) {
      copy(src, dst, l)
    }
    f.close()
  }

  def class_available(cls: String): Boolean = {
    try {
      Class.forName(cls)
      true
    } catch {
      case ex: ClassNotFoundException => false
    }
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
object Properties {

  val m = scala.collection.mutable.Map[String, String]()
  m("basedir") = new File(".").getAbsolutePath

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
   * Read XML property file to global properties.
   */
  def read_xml_properties(props: String) {
    if (new File(props).exists()) {
      val f = new FileInputStream(props)
      val p = new java.util.Properties()
      p.loadFromXML(f)
      f.close()
      for (k <- p.keySet) {
        Properties(k.toString) = p.get(k).toString
      }
    }
  }

  /**
   * Read property file to global properties.
   */
  def read_properties(props: String) {
    if (new File(props).exists()) {
      val f = new FileInputStream(props)
      val p = new java.util.Properties()
      p.load(f)
      f.close()
      for (k <- p.keySet) {
        Properties(k.toString) = p.get(k).toString
      }
    }
  }

}

object History {

  val h = scala.collection.mutable.Set[String]()

  /**
   * Run dependencies. Because you can't compare functions for identity, we
   * pass in the original Ant name of the task.
   */
  def depends(funcs: (String, () => Unit)*) {
    for (f <- funcs) {
      if (!h.contains(f._1)) {
        h += f._1
        f._2()
      }
    }
  }

}