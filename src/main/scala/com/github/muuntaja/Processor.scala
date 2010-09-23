package com.github.muuntaja

import scala.collection
import scala.collection.mutable
import scala.collection.immutable
import scala.xml.{ Elem, XML }
import scala.xml.dtd.{ DocType, SystemID }

import java.io.File
import java.io.{ Writer, FileWriter, IOException }
import java.net.URI
import java.util.logging.{ Logger, Level, ConsoleHandler, SimpleFormatter, LogRecord }

import Dita.{ Topic, Map }

/**
 * DITA conversion processor.
 * 
 * @param resourceDir resource directory
 * @param tempDir temporary directory
 * @param otCompatibility DITA-OT compatibility mode
 * @param generators list of processing steps
 */
class Processor(
  val resourceDir: File,
  val tempDir: File,
  val otCompatibility: Boolean = false,
  val logger: Logger = Logger.getAnonymousLogger) {

  private val generators: List[Generator] = List(
    new Preprocessor(resourceDir, tempDir, logger, otCompatibility),
    new ConrefProcessor(otCompatibility),
    new KeyrefProcessor(otCompatibility),
    new RelatedLinksGenerator(otCompatibility))
  
  // Public methods ------------------------------------------------------------

  /**
   * Process a DITA map file.
   * 
   * @param f input DITA map file
   */
  def run(f: URI) {
    var found: mutable.Map[URI, DocInfo] = mutable.HashMap[URI, DocInfo]()
    var tmpDita = f
    for (g <- generators) {
      logger.fine("Preprocessing with " + g.getClass.getName);
      g.setLogger(logger)
      //g.setDocInfo(preprocessor.found)
      g.found = found
      tmpDita = g.process(tmpDita)
      found = g.found
    }
    if (otCompatibility) {
      generateProperties(tmpDita, found)
    }
  }

  // Private methods -----------------------------------------------------------

  /** DITA-OT property list file prefix. */
  val LIST_PREFIX = "list"
  val FILE_PREFIX = "file"

  private def generateProperties(tmpDita: URI, found: mutable.Map[URI, DocInfo]) {
    val tmp = tempDir.toURI
    val all = getDitaProperties

    val topics = uniqueFiles(found.filter(matches(Topic.Topic)).keys)
    writeList("fullditatopic." + LIST_PREFIX, topics)
    all += "fullditatopic" + LIST_PREFIX -> topics.mkString(",")

    val maps = uniqueFiles(found.filter(matches(Map.Map)).keys)
    writeList("fullditamap." + LIST_PREFIX, maps)
    all += "fullditamap" + LIST_PREFIX -> maps.mkString(",")

    val mapsandtopics = uniqueFiles(found.keys)
    writeList("fullditamapanttopic." + LIST_PREFIX, mapsandtopics)
    all += "fullditamapandtopic" + LIST_PREFIX -> mapsandtopics.mkString(",")

    val input = uniqueFiles(List(tmpDita))
    writeList("user.input.file." + LIST_PREFIX, input)
    writeList("usr.input.file." + LIST_PREFIX, input)
    all += "user.input." + FILE_PREFIX -> tmp.relativize(tmpDita).toString
    all += "user.input.dir" -> tempDir.getAbsolutePath

    writeMap("dita." + LIST_PREFIX, all)
    writeXmlMap("dita.xml.properties", all)
  }

  private def getDitaProperties: mutable.HashMap[String, String] = {
    val all = new mutable.HashMap[String, String]()
    all += "fullditatopic" + FILE_PREFIX -> ("fullditatopic." + LIST_PREFIX)
    all += "fullditamapandtopic" + FILE_PREFIX -> ("fullditamapandtopic." + LIST_PREFIX)
    all += "fullditamap" + FILE_PREFIX -> ("fullditamap." + LIST_PREFIX)
    all += "hrefditatopic" + FILE_PREFIX -> ("hrefditatopic." + LIST_PREFIX)
    all += "copytotarget2sourcemap" + FILE_PREFIX -> ("copytotarget2sourcemap." + LIST_PREFIX)
    all += "subtargets" + FILE_PREFIX -> ("subtargets." + LIST_PREFIX)
    all += "outditafiles" + FILE_PREFIX -> ("outditafiles." + LIST_PREFIX)
    all += "chunkedditamap" + FILE_PREFIX -> ("chunkedditamap." + LIST_PREFIX)
    all += "conreftargets" + FILE_PREFIX -> ("conreftargets." + LIST_PREFIX)
    all += "resourceonly" + FILE_PREFIX -> ("resourceonly." + LIST_PREFIX)
    all += "canditopics" + FILE_PREFIX -> ("canditopics." + LIST_PREFIX)
    all += "chunkedtopic" + FILE_PREFIX -> ("chunkedtopic." + LIST_PREFIX)
    all += "keyref" + FILE_PREFIX -> ("keyref." + LIST_PREFIX)
    all += "subjectscheme" + FILE_PREFIX -> ("subjectscheme." + LIST_PREFIX)
    all += "coderef" + FILE_PREFIX -> ("coderef." + LIST_PREFIX)
    all += "key" + FILE_PREFIX -> ("key." + LIST_PREFIX)
    all += "html" + FILE_PREFIX -> ("html." + LIST_PREFIX)
    all += "image" + FILE_PREFIX -> ("image." + LIST_PREFIX)
    all += "conref" + FILE_PREFIX -> ("conref." + LIST_PREFIX)
    all += "skipchunk" + FILE_PREFIX -> ("skipchunk." + LIST_PREFIX)
    all += "relflagimage" + FILE_PREFIX -> ("relflagimage." + LIST_PREFIX)
    all += "copytosource" + FILE_PREFIX -> ("copytosource." + LIST_PREFIX)
    all += "hreftargets" + FILE_PREFIX -> ("hreftargets." + LIST_PREFIX)
    all += "flagimage" + FILE_PREFIX -> ("flagimage." + LIST_PREFIX)
    all += "conrefpush" + FILE_PREFIX -> ("conrefpush." + LIST_PREFIX)
    all += "user.input.file.list" + FILE_PREFIX -> ("usr.input.file." + LIST_PREFIX)

    all
  }

  private def uniqueFiles(list: Iterable[URI]): Set[String] = {
    val tmp = tempDir.toURI
    new immutable.HashSet ++
      list.toList.map(u => tmp.relativize(new URI(u.getScheme, u.getUserInfo, u.getHost, u.getPort, u.getPath, u.getQuery, null)).toString)
  }

  private def matches(t: DitaType)(v: Tuple2[URI, DocInfo]): Boolean = {
    v._2.cls match {
      case Some(c) => c matches t
      case _ => false
    }
  }

  private def writeList(name: String, list: Iterable[String]) {
    val topicList = new FileWriter(new File(tempDir, name))
    try {
      //for (l <- list.iterator) {
      //  topicList.write(l)
      //  topicList.write('\n')
      //}
      val i = list.iterator
      while (i.hasNext) {
        topicList.write(i.next)
        if (i.hasNext) {
          topicList.write('\n')
        }
      }

    } catch {
      case e: IOException => e.printStackTrace()
    }
    finally {
      topicList.close()
    }
  }

  private def writeMap(name: String, map: collection.Map[String, String]) {
    val topicList = new FileWriter(new File(tempDir, name))
    try {
      for ((k, v) <- map.iterator) {
        topicList.write(k)
        topicList.write('=')
        topicList.write(v)
        topicList.write('\n')
      }
    } catch {
      case e: IOException => e.printStackTrace()
    }
    finally {
      topicList.close()
    }
  }

  private def writeXmlMap(name: String, map: collection.Map[String, String]) {
    val r = <properties>{
      map.iterator.map(e => <entry key={ e._1 }>{ e._2 }</entry>)
      //for ((k, v) <- map.iterator) {
      //  <entry key={k}>{ v }</entry>
      //}
    }</properties>
    scala.xml.XML.save(new File(tempDir, name).getAbsolutePath, r, "UTF8", true,
      new DocType("properties", new SystemID("http://java.sun.com/dtd/properties.dtd"), Nil))
  }

}