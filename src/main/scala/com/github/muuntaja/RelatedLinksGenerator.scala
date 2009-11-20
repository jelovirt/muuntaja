package com.github.muuntaja

import java.util.logging.Logger
import scala.collection.mutable
import nu.xom.{Document, Element, Attribute, Comment, Nodes}
import java.net.URI
import XOM.{elementsToSeq, nodesToSeq}
import Dita._

/**
 * Generator that processes links and xrefs to
 * 
 * <ul>
 * <li>add link text and description</li>
 * <li>mark dead links</li>
 * </ul>
 */
class RelatedLinksGenerator(val otCompatibility: boolean) extends Generator {
  def this() {
    this(true)
  }
  
  private val topicrefType = DitaType("- map/topicref ")
  private val topicType = DitaType("- topic/topic ")
  private val titleType = DitaType("- topic/title ")
  private val titlealtsType = DitaType("- topic/titlealts ")
  private val bodyType = DitaType("- topic/body ")
  private val prologType = DitaType("- topic/prolog ")
  private val shortdescType = DitaType("- topic/shortdesc ")
  private val abstractType = DitaType("- topic/abstract ")
  private val relatedLinksType = DitaType("- topic/related-links ")
  private val linkType = DitaType("- topic/link ")
  private val linktextType = DitaType("- topic/linktext ")
  private val descType = DitaType("- topic/desc ")
  private val xrefType = DitaType("- topic/xref ")
  private val reltableType = DitaType("- map/reltable ")
  private val relrowType = DitaType("- map/relrow ")
  private val relcellType = DitaType("- map/relcell ")
  private val relheaderType = DitaType("- map/relheader ")
  private val relcolspecType = DitaType("- map/relcolspec ")
 
  private val processed = mutable.HashSet[URI]()
  
  private implicit def elementToDitaElement(e: Element) =
    new DitaElement(e)
  
  var found: mutable.Map[URI, DocInfo] = _
  var logger: Logger = _
  
  def setDocInfo(f: mutable.Map[URI, DocInfo]) {
    found = f
  }
  
  def setLogger(log: Logger) {
    logger = log
  }
  
  def process(ditamap: URI): URI = {
    XMLUtils.parse(ditamap) match {
      case Some(doc) => {
        val relations: List[Relation] = getRelations(doc, ditamap)
        walker(doc.getRootElement, ditamap.resolve("."))
        XMLUtils.serialize(doc, ditamap)
        ditamap
      }
      case None => throw new Exception("Unable to parse " + ditamap.toString)
    }
  }
  
  /**
   * Get relations from root reltable
   */
  private def getRelations(doc: Document, base: URI): List[Relation] = {
    val relations = mutable.HashMap[URI, Relation]()
    val reltables = doc.getRootElement \ reltableType
    // relationship tables
    for (r <- reltables; val reltable = r.asInstanceOf[Element]) {
println("processing reltable")
      val relcells = reltable \ relrowType \ relcellType
      // cells
      for (c <- relcells; val relcell = c.asInstanceOf[Element]) {
        val topics = relcell \ topicrefType
        // topic refereces
        for (t <- topics; val topicref = t.asInstanceOf[Element]) {
          (topicref("href"), topicref("scope")) match {
            case (Some(href), Some("local")) => {
              val url = base.resolve(href)
              val relation = if (relations contains url) {
                               relations(url)
                             } else {
                               val r = new Relation(DitaURI(url))
                               relations(url) = r
                               r
                             }
              val otherCells = relcells.filter(c => !(c eq relcell)).map(_.asInstanceOf[Element])
              processRelations(topicref, otherCells, relation, relations)
println("Relations: " + relation.toString)
            }
            case _ =>
          }
        }
      }
    }
    relations.values.toList
  }
  
  private def processRelations(topicref: Element, otherCells: List[Element], relation: Relation, relations: mutable.Map[URI, Relation]) {
    val base = new URI(topicref.getBaseURI())
    for (c <- otherCells; t <- c \ topicrefType; val target = t.asInstanceOf[Element]) {
      target("href") match {
        case Some(href) => {
          val uri = DitaURI(base.resolve(href))
println("  Add relation to " + uri)
          relation += uri
        }
        case _ =>
      }      
    }
  }
  
  private def walker(e: Element, base: URI) {
    if ((e isType topicrefType) && e("href") != None && e("dead", Preprocessor.MUUNTAJA_NS) == None) {
      val f: URI = base.resolve(e("href").get)
      if (!(processed contains f)) {
        XMLUtils.parse(f) match {
          case Some(doc) => {
            topicWalker(e, doc.getRootElement, f)
            XMLUtils.serialize(doc, f)
          }
          case _ =>
        }
        processed += f
      }
    }
    for (c <- e.getChildElements) walker(c, base)
  }
  
  private def topicWalker(topicref: Element, e: Element, base: URI) {
    if (e isType topicType) {
      /*
      val rel = e.getElement(relatedLinksType, List(titleType, titlealtsType, shortdescType, abstractType, prologType, bodyType))
      // ancestors
      var p = topicref.getParent
      while (p != null && p.isInstanceOf[Element]) {
        DitaElement(p.asInstanceOf[Element])("href") match {
          case Some(href) => {
            val link = createElement(linkType)
            link.addAttribute(new Attribute("role", "ancestor"))
            link.addAttribute(new Attribute("href", href))
            //rel.insertChild(link, 0)
            rel.appendChild(link)
          }
          case None =>
        }
        p = p.getParent
      }
      */
    } else if (e.isType(linkType, xrefType)) {
      processLink(e, base)
    }
    for (c <- e.getChildElements) topicWalker(topicref, c, base)   
  }
  
  /**
   * Process link element.
   */
  private def processLink(e: Element, base: URI) {
    val s = e("scope") match {
      case None => e.addAttribute(new Attribute("scope", "local"))
      case Some(t) => 
    }
    val t = e("format") match {
      case None => e.addAttribute(new Attribute("format", "dita"))
      case Some(t) => 
    }
    (e("href"), e("scope")) match {
      case (Some(href), Some("local")) => {
        val h = DitaURI(base.resolve(href))
        val docInfo = if ((found contains h.topicURI) && !h.element.isEmpty) {
          DocInfo(h)
        } else if (found contains h.topicURI) {
          found(h.topicURI)
        } else {
          DocInfo.empty
        }
        if ((found contains h.topicURI) && !docInfo.id.isEmpty) {
          (e("type"), docInfo.ditaType) match {
            case (None, Some(ditaType)) => e.addAttribute(new Attribute("type", ditaType))
            case _ =>
          }
          if (e isType linkType) {
            if ((e \ linktextType).size == 0) {
              docInfo.title match {
                case Some(t) => e.insertChild(createElement(linktextType, t), 0)
                case None => {
                  if (otCompatibility) {
                    e.insertChild(createElement(linktextType, Some(href)), 0)
                  }
                }
              }
            }
          } else if (e isType xrefType) {
            if (e.getChildCount == 0) {
              docInfo.title match {
                case Some(t) => for (c <- t.getChildren) e.appendChild(c.copy)
                case None =>
              }
            }
          }
          if (!(otCompatibility && (e isType xrefType))) { // XXX: OT doens't add desc if linktext is missing.
            if ((e \ descType).size == 0) {
              docInfo.desc match {
                case Some(d) => e.appendChild(createElement(descType, d))
                case None =>
              }
            }
          }
        } else {
          //if (!otCompatibility) {
          e.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
          //}
        }
      }
      case (Some(href), Some("external")) => {
        if (otCompatibility) { // XXX: OT functionality that's wrong I think
          if (e isType linkType) {
            if ((e \ linktextType).size == 0) {
              val h = createElement(linktextType)
              h.appendChild(href)
              e.insertChild(h, 0)
            }
          } else if (e isType xrefType) {
            if (e.getChildCount == 0) {
              e.appendChild(href)
            }
          }
        }
      }
      case _ =>
    }
  }
  
  // private classes
  
  private class Relation(val from: DitaURI) {
    private val to: mutable.ListBuffer[DitaURI] = new mutable.ListBuffer[DitaURI]()
    //def +(add: DitaURI): Relation = {
    //  new Relation(from, add :: to)
    //}
    def +=(add: DitaURI) {
      to += add
    }
    override def toString(): String = {
      val buf = new StringBuffer
      buf.append(from).append(" -> ")
      for (t <- to) {
        buf.append(t).append(" ")
      }
      buf.toString
    }
  }
    
}