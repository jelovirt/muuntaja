package com.github.muuntaja


import scala.collection.mutable

import java.util.logging.Logger
import java.net.URI
import java.util.regex.{Pattern, Matcher}

import nu.xom.{Document, Element, Attribute, ProcessingInstruction}

import XOM.{elementsToSeq, nodesToSeq}
import Dita._

/**
 * Conref processor.
 * 
 * # Process all conrefs in map
 * # Process every referenced topic for conrefs
 * 
 * @author jelovirt
 */
class KeyrefProcessor(val otCompatibility: Boolean = false) extends Generator {
    
  // Private variables ---------------------------------------------------------
  
  /**
   * Attributes that should not be copied from conref element to
   * replacing element.
   */
  private val nonCopyAttrs = List("conref", "class", "xtrf", "xtrc")
  
  private val copyAttrs = (if (otCompatibility) List("linking") else Nil) :::
	  				      List("href", "scope", "format")
    
  /** Set of topics to process. */
  private var found: mutable.Map[URI, DocInfo] = _
  private var log: Logger = _
  private var changed: Boolean = _
  
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
        val keydefs = new mutable.HashMap[String, Element] 
        changed = false
        mapWalker(doc.getRootElement, ditamap.resolve("."), keydefs, topics)
        //if (changed) {
        //  XMLUtils.serialize(doc, ditamap)
        //}
        for (k <- keydefs.keys) println(k + " -> " + keydefs(k).toXML)
        for (f <- topics) {
          XMLUtils.parse(f) match {
            case Some(doc) => {
              changed = false
              topicWalker(doc.getRootElement, f.resolve("."), keydefs)
              if (changed) {
                XMLUtils.serialize(doc, f)
              }
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
   * Map walker for keyref.
   */
  private def mapWalker(e: Element, base: URI, keydefs: mutable.Map[String, Element], topics: mutable.Set[URI]) {
    if (e isType Map.Topicref) {
      (e.getAttributeValue("processing-role"), e.getAttributeValue("href"), e("format"), e("scope")) match {
        case (_, null, _, _) =>
        case ("resource-only", _, _, _) =>
        case (_, href, Some("dita"), Some("local")) => {
          val f = base.resolve((new URI(href)).getPath)
          topics += f
        }
        case _ =>
      }
    }
    e.getAttributeValue("keys") match {
      case null =>
      case keys => {
    	val keydef = getKeyDefinition(e)
        for (k <- keys.trim.split("\\s+")) {
          if (!(keydefs contains k)) {
            keydefs += (keys -> keydef)
          }
        } 
      }
    }
    // process map child elements
    for (c <- e.getChildElements) mapWalker(c, base, keydefs, topics)
  }
  
  private def getKeyDefinition(src: Element): Element = {
    val keydef = new Element("keydef")
	(src \ Map.Topicmeta \ Topic.Keywords \ Topic.Keyword).toList.headOption.asInstanceOf[Option[Element]] match {
      case Some(k) => {
        for (i <- 0.until(k.getChildCount)) {
          keydef.appendChild(k.getChild(i).copy)
        }
      }
      case _ =>
    }
    for (n <- copyAttrs) {
      src.getAttribute(n) match {
        case null =>
        case a => keydef.addAttribute(a.copy.asInstanceOf[Attribute])
      }
    }
    keydef
  }
  
 
  /**
   * Topic walker for keyref.
   */
  private def topicWalker(e: Element, base: URI, keydefs: mutable.Map[String, Element]) {
    e.getAttributeValue("keyref") match {
      case null => 
      case href => processKeyref(e, keydefs)
    }
    for (c <- e.getChildElements) topicWalker(c, base, keydefs)
  }
  
  /*
  val withHref = List(Topic.Author, Topic.Data, Topic.Data-about, Topic.Image, Topic.Link,
		  			  Topic.Lq, Topic.Navref, Topic.Publisher, Topic.Source, Map.Topicref, Topic.Xref)
  
  val withOutHref = List(Topic.Cite, Topic.Dt, Topic.Keyword, Topic.Term, Topic.Ph,
		  			     Topic.Indexterm, Topic.Index-base, Topic.Indextermref)
  */
  
  private def processKeyref(e: Element, keydefs: mutable.Map[String, Element]) {
    changed = true
    if (keydefs contains e.getAttributeValue("keyref")) {
      val src = keydefs(e.getAttributeValue("keyref"))
      /*
      if (withOutHref contains {t => e isType t}) {
    	// XXX: OT doens't replace content, keyref is processed only if element is empty
        if (e.getChildCount == 0 && src.getChildCount > 0) {
          //for (i <- 0.until(e.getChildCount).reverse) {
          //  e.removeChild(i)
          //}
          for (i <- 0.until(src.getChildCount)) {
            e.appendChild(src.getChild(i).copy)
          }
        }
        for (i <- 0.until(src.getAttributeCount)) {
          e.addAttribute(src.getAttribute(i).copy.asInstanceOf[Attribute])
        }
      } else if (withHref contains {t => e isType t}) {
        for (i <- 0.until(src.getAttributeCount)) {
          e.addAttribute(src.getAttribute(i).copy.asInstanceOf[Attribute])
        }
      } else {
        throw new RuntimeException("Unhandled keyref element " + e.getLocalName)
      }
      */
      // XXX: OT doens't replace content, keyref is processed only if element is empty
      if (e.getChildCount == 0 && src.getChildCount > 0) {
        //for (i <- 0.until(e.getChildCount).reverse) {
        //  e.removeChild(i)
        //}
    	e.appendChild(new ProcessingInstruction("foo", "bar"))
        for (i <- 0.until(src.getChildCount)) {
          e.appendChild(src.getChild(i).copy)
        }
      }
      for (i <- 0.until(src.getAttributeCount)) {
        e.addAttribute(src.getAttribute(i).copy.asInstanceOf[Attribute])
      }
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