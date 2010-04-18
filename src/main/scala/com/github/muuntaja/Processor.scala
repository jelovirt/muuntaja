package com.github.muuntaja

// Take DITA input and normalize them into a folder

import java.io.File
import java.net.URI
import java.util.logging.{Logger, Level, ConsoleHandler, SimpleFormatter, LogRecord}

class Processor(val resource: File, val temp: File, val otCompatibility: Boolean) {
  def this(resource: File, temp: File) =
    this(resource, temp, false)
  
  val logger = Logger.getAnonymousLogger
  
  val generators: List[Generator] = List (
    new ConrefProcessor(otCompatibility),
    new KeyrefProcessor(otCompatibility),
    new RelatedLinksGenerator(otCompatibility)
  )
  
  def run(f: URI) {
    val preprocessor = new Preprocessor(resource, temp, logger, otCompatibility)
    var tmpDita = preprocessor.process(f)
    for (g <- generators) {
      g.setLogger(logger)
      g.setDocInfo(preprocessor.found)
      tmpDita = g.process(tmpDita)
    }
  }
  
}