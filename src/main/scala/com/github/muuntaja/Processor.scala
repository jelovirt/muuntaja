package com.github.muuntaja


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
 * @param logger process logger
 */
class Processor(
  //val resourceDir: File,
  val catalog: File,
  val tempDir: File,
  val otCompatibility: Boolean = false,
  val logger: Logger = Logger.getAnonymousLogger) {
	
  protected val generators: Array[Generator] = Array(
    new Preprocessor(catalog, tempDir, otCompatibility),
    new ConrefProcessor(otCompatibility),
    new KeyrefProcessor(otCompatibility),
    new RelatedLinksGenerator(otCompatibility))
  
  protected var tmpDita: URI = _
  protected var found: immutable.Map[URI, DocInfo] = _
    
  // Public methods ------------------------------------------------------------

  /**
   * Process a DITA map file.
   * 
   * @param f input DITA map file
   */
  def run(f: URI) {
    found = immutable.HashMap.empty
    tmpDita = f
    var job = new Job(logger, f, f.resolve("."), found)
    for (g <- generators) {
      logger.fine("Preprocessing with " + g.getClass.getName);
//      g.setLogger(logger)
      //g.setDocInfo(preprocessor.found)
//      g.found = found
//      tmpDita = g.process(tmpDita)
//      found = g.found
      job = g.process(job)
    }
  }

}