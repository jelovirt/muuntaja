package com.github.muuntaja


import scala.collection.immutable

import java.net.URI
import java.util.logging.Logger


class Job(
    val log: Logger,
    val input: URI,
    val base: URI,
    val found: immutable.Map[URI, DocInfo]) {
	
}