package com.github.muuntaja

//import scala.xml.parsing.NoBindingFactoryAdapter
//import scala.xml.{Elem, Node, TopScope}

import java.io.{File, IOException, BufferedOutputStream, FileOutputStream}
import java.net.URI
import javax.xml.parsers.{SAXParserFactory, SAXParser}

import org.xml.sax.{InputSource, XMLReader, Attributes}
import org.xml.sax.helpers.{XMLFilterImpl, AttributesImpl}

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.{ResolvingXMLReader, CatalogResolver}

import nu.xom.{Document, ParentNode, Element, Attribute, Elements, Nodes, Node, ProcessingInstruction, Text, Builder, Serializer, DocType}
import scala.collection.mutable.ListBuffer


class URIUtils(u: URI) {
  def setFragment(fragment: String): URI = 
    new URI(u.getScheme(),
            u.getUserInfo(), u.getHost(), u.getPort(),
            u.getPath(), u.getQuery(),
            fragment)
}
object URIUtils {
  implicit def URI2URIUtils(u: URI) =
    new URIUtils(u)
}
