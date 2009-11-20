package com.github.muuntaja

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.net.URI
import org.scalatest.Suite
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}
import java.util.regex.{Pattern, Matcher}
import scala.xml.Elem

@RunWith(classOf[JUnitRunner])
class DitaTypeSuite extends Suite {
  val topicCls = new DitaType("- topic/topic ")
  val conceptCls = new DitaType("- topic/topic concept/concept ")
  val topicrefCls = new DitaType("- map/topicref ")
  val topicheadCls = new DitaType("+ map/topicref mapgroup-d/topichead ") 
  
  def testEquals {
    assert(conceptCls == new DitaType("- topic/topic concept/concept"))
    assert(conceptCls != topicCls)
  }

  def testHashCode {
    assert(conceptCls.hashCode == DitaType("- topic/topic concept/concept").hashCode)
    assert(conceptCls.hashCode != topicCls.hashCode)
  }
  
  def testDitaTypeToString {
    val cls = new DitaType("-  topic/topic  concept/concept  ")
    
    assert(topicCls.toString == "- topic/topic ")
    assert(cls.toString == "- topic/topic concept/concept ")
  }

  def testDitaTypeLocalName {
    val cls = new DitaType("-  topic/topic  concept/concept  ")
    
    assert(topicCls.localName == "topic")
    assert(cls.localName == "concept")
  }
  
  def testDitaTypeMatches {
    val topicElem = new Element(topicCls.localName)
    topicElem.addAttribute(new Attribute(Dita.ClassAttribute, topicCls.toString))
    val conceptElem = new Element(conceptCls.localName)
    conceptElem.addAttribute(new Attribute(Dita.ClassAttribute, conceptCls.toString))
    
    assert(topicCls matches topicElem)
    assert(conceptCls matches topicElem)
    assert(!(topicCls matches conceptCls))
    
    assert(conceptCls matches topicElem)
    assert(conceptCls matches conceptElem)
    assert(!(topicCls matches conceptElem))
        
    assert(topicheadCls matches topicrefCls)
  }
}

@RunWith(classOf[JUnitRunner])
class DitaElementSuite extends Suite {
  val topicCls = new DitaType("- topic/topic ")
  val topicDitaElem = DitaElement(Dita.createElement(topicCls))
  val conceptCls = new DitaType("- topic/topic concept/concept ")
  val conceptDitaElem = DitaElement(Dita.createElement(conceptCls))
  
  def testApply {
    val map = new Element("map")
    map.addAttribute(new Attribute("scope", "peer"))
    map.addAttribute(new Attribute("type", "topic"))
    map.addAttribute(new Attribute("xml:lang", XMLConstants.XML_NS_URI, "en-US"))
    val topicgroup = new Element("topicgroup")
    val topicref = new Element("topicref")
    topicref.addAttribute(new Attribute("type", "concept"))
    topicgroup.appendChild(topicref)
    map.appendChild(topicgroup)
    val e = DitaElement(topicref)
    
    e("scope") match {
      case Some("peer") =>
      case None => fail()
    }
    e("type") match {
      case Some("concept") =>
      case None => fail()
    }
    e("format") match {
      case Some(f) => fail()
      case None =>
    }
    e("lang", XMLConstants.XML_NS_URI) match {
      case Some("en-US") =>
      case None => fail()
    }
  }
  def testIsType {
    assert(conceptDitaElem isType topicCls)
    assert(conceptDitaElem isType conceptCls)
    assert(!(topicDitaElem isType conceptCls))
  }
}

@RunWith(classOf[JUnitRunner])
class DitaSuite extends Suite {
  import Dita._
  val base = new URI("file://qux/")
  
  def testParseTopicHref() {
    parseTopicHref("foo#bar/baz", base) match {
      case (Some(uri), Some("bar"), Some("baz")) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    parseTopicHref("foo#bar", base) match {
      case (Some(uri), Some("bar"), None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    parseTopicHref("foo", base) match {
      case (Some(uri), None, None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    parseTopicHref("#bar/baz", base) match {
      case (None, Some("bar"), Some("baz")) => ()
      case m => fail()
    }
    parseTopicHref("#bar", base) match {
      case (None, Some("bar"), None) => ()
      case m => fail()
    }
  }
  def testParseMapHref() {
    parseMapHref("foo#bar", base) match {
      case (Some(uri), Some("bar")) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    parseMapHref("foo", base) match {
      case (Some(uri), None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    parseMapHref("#bar", base) match {
      case (None, Some("bar")) => ()
      case m => fail()
    }
  }
  
  def testDitaNodeUtil {    
    val a = createElement("- topic/topic concept/concept ")
    val b = createElement("- topic/body concept/conbody ")
    a.appendChild(b)
    val c1 = createElement("- topic/p ")
    val c2 = createElement("- topic/p")
    b.appendChild(c1)
    b.appendChild(c2)
    
    val n = new Nodes
    n.append(a)
    val body = new DitaType("- topic/body ")
    val rb = n \ body
    assert(rb.size == 1)
    assert(rb.get(0) eq b)
    val p = new DitaType("- topic/p ")
    val rc = n \ body \ p
    assert(rc.size == 2)
    assert(rc.get(0) eq c1)
    assert(rc.get(1) eq c2)
  }
  
}

@RunWith(classOf[JUnitRunner])
class DitaURISuite extends Suite {
  import Dita._
  
  val base = new URI("file://qux/")
  def testDitaURI() {
    DitaURI(base.resolve("foo#bar/baz")) match {
      case DitaURI(uri, Some("bar"), Some("baz")) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    DitaURI(base.resolve("foo#bar")) match {
      case DitaURI(uri, Some("bar"), None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    DitaURI(base.resolve("foo")) match {
      case DitaURI(uri, None, None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    DitaURI(base.resolve("#bar/baz")) match {
      case DitaURI(uri, Some("bar"), Some("baz")) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
    DitaURI(base.resolve("#bar")) match {
      case DitaURI(uri, Some("bar"), None) => if (uri.toString != "file://qux/foo") fail()
      case m => fail()
    }
  }
}