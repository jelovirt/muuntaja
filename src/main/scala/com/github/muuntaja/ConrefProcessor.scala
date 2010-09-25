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
    
  // Private variables ---------------------------------------------------------
  
  private val CONREF_ATTR = "conref"
	
  /**
   * Attributes that should not be copied from conref element to
   * replacing element.
   */
  private val nonCopyAttrs = List(CONREF_ATTR, Dita.CLASS_ATTR, "xtrf", "xtrc")
	
  /** Set of topics to process. */
  //private var found: mutable.Map[URI, DocInfo] = _
  private var log: Logger = _
  private var changed: Boolean = _
  
  // Public functions ----------------------------------------------------------
  
  //override def setDocInfo(f: mutable.Map[URI, DocInfo]) {
  //  found = f
  //}
  
  override def setLogger(logger: Logger) {
    log = logger
  }
	
  override def process(ditamap: URI): URI = {
    //XMLUtils.parse(ditamap) match {
    //  case Some(doc) => {
    	//changed = false
        //mapWalker(doc.getRootElement, ditamap.resolve("."))
    	//if (changed) {
        //  XMLUtils.serialize(doc, ditamap)
    	//}
    	val topics = found.filter(e => { e._1.getFragment == null && e._2.ditaType.isDefined })
        for ((f, d) <- topics) {
          XMLUtils.parse(f) match {
            case Some(doc) => {
              changed = false
              topicWalker(doc.getRootElement, f.resolve("."))
              if (changed) {
                XMLUtils.serialize(doc, f)
              }
            }
            case None =>
          }
        }      
      //}
      //case _ =>
    //}
    ditamap
  }
  
  
  // Private functions ---------------------------------------------------------
  
  /**
   * Map walker for conref.
   */
  /*
  private def mapWalker(e: Element, base: URI, topics: mutable.Set[URI]) {
    e.getAttributeValue(CONREF_ATTR) match {
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
  */
 
  /**
   * Topic walker for conref.
   */
  private def topicWalker(e: Element, base: URI) {
    e.getAttributeValue(CONREF_ATTR) match {
      case null => {
        for (c <- e.getChildElements) topicWalker(c, base)
      } 
      case href => processConref(e)
    }
    
  }
    
  private def processConref(e: Element) {
	changed = true
	getReferencedElement(e.getAttribute(CONREF_ATTR)) match {
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
          if a.getValue != "-dita-use-conref-target" && !(nonCopyAttrs contains a.getLocalName)
        ){
          repl.addAttribute(a.copy.asInstanceOf[Attribute])
        }
        e.getParent.replaceChild(e, repl)
      }
      case _ =>
    }
  }
  
  /**
   * For reference attribute, get referenced element.
   * 
   * @param ref reference attribute
   * @return referenced target element
   */
  private def getReferencedElement(ref: Attribute): Option[Element] = {
	val current = new URI(ref.getBaseURI)
	val u = DitaURI(new URI(ref.getValue))
	val ua = current.resolve(u.uri)
    val doc = if (u.uri.toString == "" || ua.toString == current.toString) Some(ref.getDocument)
              else XMLUtils.parse(ua)
    doc match {
      case Some(doc) => selectElement(doc, u)
	  case _ => None
    }
  }

  /**
   * Get element from document by ID.
   *
   * @param doc document to select from
   * @param uri ID definition of element to select
   * @return selected element
   */
  private def selectElement(doc: Document, uri: DitaURI): Option[Element] = {
    (uri.topic, uri.element) match {
      case (Some(topic), Some(element)) => {
    	  doc.getRootElement.query("//*[@id = '" + topic + "']//*[@id = '" + element + "']").headOption.asInstanceOf[Option[Element]]
    	  //doc getRootElement \\ Topic.Topic filter {e => e.getAttributeValue("id") == topic } \\ "*" filter {e => e.getAttributeValue("id") == element } firstOption
      } 
      case (Some(topic), None) => {
    	  doc.getRootElement.query("//*[@id = '" + topic + "']").headOption.asInstanceOf[Option[Element]]
    	  //doc getRootElement \\ Topic.Topic filter {e => e.getAttributeValue("id") == topic } \\ "*" filter {e => e.getAttributeValue("id") == element } firstOption
      }
      case _ => {
    	  Some(doc getRootElement)
      }
    }
  } 
  
}