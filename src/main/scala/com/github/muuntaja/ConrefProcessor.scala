package com.github.muuntaja


import scala.collection.mutable

import java.util.logging.Logger
import java.net.URI
import java.util.regex.{Pattern, Matcher}

import nu.xom.{Document, Element, Attribute}

import XOM._
import Dita._

/**
 * Conref processor.
 * 
 * # Process all conrefs in map
 * # Process every referenced topic for conrefs
 * 
 * @author jelovirt
 */
class ConrefProcessor(val otCompatibility: Boolean = false) extends Generator {
  //var mapChanged = false
  //var topicChanged = false
    
  // Private variables ---------------------------------------------------------
  
  private var found: mutable.Map[URI, DocInfo] = _
  private var log: Logger = _

  // Public functions ----------------------------------------------------------
  
  override def setDocInfo(f: mutable.Map[URI, DocInfo]) {
    found = f
  }
  
  override def setLogger(logger: Logger) {
    log = logger
  }
	
  override def process(ditamap: URI): URI = {
    XMLUtils.parse(ditamap) match {
      case Some(doc) => {
    	val topics = new mutable.HashSet[URI]
        mapWalker(doc.getRootElement, ditamap.resolve("."), topics)
        XMLUtils.serialize(doc, ditamap)
        for (f <- topics) {
          XMLUtils.parse(f) match {
            case Some(doc) => {
              topicWalker(doc.getRootElement, f.resolve("."))
              XMLUtils.serialize(doc, f)
            }
            case None =>
          }
        }      
      }
      case _ =>
    }
    ditamap
  }
  
  // Private functions ---------------------------------------------------------
  
  /**
   * Map walker for conref.
   */
  private def mapWalker(e: Element, base: URI, topics: mutable.Set[URI]) {
    e.getAttributeValue("conref") match {
      case null => {
        if (e isType Map.Topicref) {
          (e.getAttributeValue("href"), e("format"), e("scope")) match {
        	case (null, _, _) =>
            case (href, Some("dita"), Some("local")) => {
              val f = base.resolve((new URI(href)).getPath)
              topics += f
            }
            case _ =>
          }
        }
        for (c <- e.getChildElements) mapWalker(c, base, topics)
      }
      case href => processConref(e)
    }
  }
 
  /**
   * Topic walker for conref.
   */
  private def topicWalker(e: Element, base: URI) {
    e.getAttributeValue("conref") match {
      case null => {
        for (c <- e.getChildElements) topicWalker(c, base)
      } 
      case href => processConref(e)
    } 
  }
  
  private def processConref(e: Element) {
	getReferencedElement(e.getAttribute("conref")) match {
      case Some(elem) => {
        val repl = elem.copy.asInstanceOf[Element]
        for (
          i <- 0 until repl.getAttributeCount;
          val a: Attribute = repl.getAttribute(i)
          if a.getLocalName == "id"
        ){
          repl.removeAttribute(a)
        }
        for (
          i <- 0 until e.getAttributeCount;
          val a: Attribute = e.getAttribute(i)
          if a.getValue != "-dita-use-conref-target" &&
             a.getLocalName != "conref" &&
             a.getLocalName != "class" &&
             a.getLocalName != "xtrf" &&
             a.getLocalName != "xtrc"
        ){
          println("add attribute " + a.getLocalName + "="+a.getValue)
          repl.addAttribute(a.copy.asInstanceOf[Attribute])
        }
        e.getParent.replaceChild(e, repl)
      }
      case _ =>
    }
  }
  
  /**
   * For reference attribute, get referenced element.
   */
  private def getReferencedElement(ref: Attribute): Option[Element] = {
	val p = ref.getParent
	val current = new URI(p.getBaseURI)
	val u = DitaURI(new URI(ref.getValue))
	val ua = u.uri.resolve(current)
    val f = new URI(ref.getBaseURI).resolve(u.topicURI.getPath)
    val doc = if (ua == current) Some(p.getDocument)
              else XMLUtils.parse(ua)
    doc match {
      case Some(doc) => selectElement(doc, u)
	  case _ => None
    }
  }
 
  private def selectElement(doc: Document, uri: DitaURI): Option[Element] = {
    (uri.topic, uri.element) match {
      case (Some(topic), Some(element)) => {
    	  doc.getRootElement.query("//*[@id = '" + topic + "']//*[@id = '" + element + "']").firstOption.asInstanceOf[Option[Element]]
    	  //doc getRootElement \\ Topic.Topic filter {e => e.getAttributeValue("id") == topic } \\ "*" filter {e => e.getAttributeValue("id") == element } firstOption
      } 
      case (Some(topic), None) => {
    	  doc.getRootElement.query("//*[@id = '" + topic + "']").firstOption.asInstanceOf[Option[Element]]
    	  //doc getRootElement \\ Topic.Topic filter {e => e.getAttributeValue("id") == topic } \\ "*" filter {e => e.getAttributeValue("id") == element } firstOption
      }
      case _ => {
    	  Some(doc getRootElement)
      }
    }
  } 
  
}