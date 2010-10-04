package com.github.muuntaja


import scala.collection.immutable

import java.net.URI
import java.util.logging.Logger


class Job(
  val log: Logger,
  val input: URI,
  val base: URI,
  val found: immutable.Map[URI, DocInfo]) {

  def this(log: Logger, input: URI) {
    this(log, input, input.resolve("."), immutable.Map.empty)
  }
  
}