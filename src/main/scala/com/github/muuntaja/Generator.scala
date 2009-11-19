package com.github.muuntaja

import scala.collection.mutable
import java.util.logging.Logger
import java.net.URI

trait Generator {
  def process(ditamap: URI): URI
  def setDocInfo(found: mutable.Map[URI, DocInfo])
  def setLogger(log: Logger)
}
