package com.github.muuntaja

import scala.collection.mutable
import net.jcip.annotations.NotThreadSafe
import java.util.logging.Logger
import java.net.URI

trait Generator {
  @NotThreadSafe
  def process(job: Job): Job
//  def process(ditamap: URI): URI
  //def process(ditamap: URI, base: URI): URI
  //def setDocInfo(found: mutable.Map[URI, DocInfo])
//  def setLogger(log: Logger)
//  var found: mutable.Map[URI, DocInfo] = _
}
