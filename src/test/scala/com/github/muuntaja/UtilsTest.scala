package com.github.muuntaja

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.{ResolvingXMLReader, CatalogResolver}
import javax.xml.parsers.{SAXParserFactory, SAXParser}
import org.xml.sax.{InputSource, XMLReader, Attributes}
import nu.xom.{Document, Element, Attribute, Elements, Nodes, Node, Builder, Serializer, DocType}
import java.io.{File, IOException, BufferedOutputStream, FileOutputStream}
import java.net.URI
import org.xml.sax.helpers.{XMLFilterImpl, AttributesImpl}
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Elem, Node, TopScope}
import org.scalatest.Suite

@RunWith(classOf[JUnitRunner])
class XOMSuite extends Suite {
  
  import XOM._
  
  val a = new Element("a")
  val b = new Element("b")
  a.appendChild(b)
  val c1 = new Element("c")
  val c2 = new Element("c")
  b.appendChild(c1)
  b.appendChild(c2)
 
  def testNodeUtil {
    val n = new Nodes
    n.append(a)
    val rb = n \ "b"
    assert(rb.size == 1)
    assert(rb.get(0) eq b)
    val rc = n \ "b" \ "c"
    assert(rc.size == 2)
    assert(rc.get(0) eq c1)
    assert(rc.get(1) eq c2)
  }
}

@RunWith(classOf[JUnitRunner])
class XMLUtilsSuite extends Suite {
  val src = "/Users/jelovirt/Work/personal/Muuntaja"
  val catalog = new File(src, "src/main/dtd/catalog.xml")
  
  def testLoadXML {
    val utils = new XMLUtils
    utils.catalogFiles(catalog)
    val f = new File(src, "src/test/xml/map-titles/in/test.ditamap")
    utils.loadXML(f.toURI) match {
      case Some(e) =>
      case None => fail
    }
  }
}