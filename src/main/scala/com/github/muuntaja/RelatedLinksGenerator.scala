package com.github.muuntaja

import scala.collection.mutable
import scala.collection.immutable.Map
import java.util.logging.Logger
import java.net.URI
import javax.xml.namespace.QName
import nu.xom.{Document, Element, Attribute, Comment, Nodes}
import XOM.{elementsToSeq, nodesToSeq}
import Dita._
import URIUtils._

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
  private val topicmetaType = DitaType("- map/topicmeta ")
  private val mapLinktextType = DitaType("- map/linktext ")
  private val mapShortdescType = DitaType("- topic/shortdesc ")
  private val titleType = DitaType("- topic/title ")
  private val titlealtsType = DitaType("- topic/titlealts ")
  private val bodyType = DitaType("- topic/body ")
  private val prologType = DitaType("- topic/prolog ")
  private val shortdescType = DitaType("- topic/shortdesc ")
  private val abstractType = DitaType("- topic/abstract ")
  private val relatedLinksType = DitaType("- topic/related-links ")
  private val linkType = DitaType("- topic/link ")
  private val linkpoolType = DitaType("- topic/linkpool ")
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
  var log: Logger = _
  
  def setDocInfo(f: mutable.Map[URI, DocInfo]) {
    found = f
  }
  
  def setLogger(logger: Logger) {
    log = logger
  }
  
  def process(ditamap: URI): URI = {
    XMLUtils.parse(ditamap) match {
      case Some(doc) => {
        val relations: Map[DitaURI, Relation] = getRelations(doc, ditamap)
        relations.keys.map(println)
        walker(doc.getRootElement, ditamap.resolve("."), relations)
        XMLUtils.serialize(doc, ditamap)
        ditamap
      }
      case None => throw new Exception("Unable to parse " + ditamap.toString)
    }
  }
  
  /**
   * Get relations from root reltable
   */
  private def getRelations(doc: Document, base: URI): Map[DitaURI, Relation] = {
    val relations = mutable.HashMap[DitaURI, Relation]()
    val reltables = doc.getRootElement \ reltableType
    // relationship tables
    for (r <- reltables; val reltable = r.asInstanceOf[Element]) {
        //println("processing reltable")
        val relrows = reltable \ relrowType
        // rows
        for (row <- relrows; val relrow = row.asInstanceOf[Element]) {
          //println("processing row")
          val relcells = relrow \ relcellType
          // cells
          for (c <- relcells; val relcell = c.asInstanceOf[Element]) {
            val topics = relcell \ topicrefType
            // topic refereces
            for (t <- topics; val topicref = t.asInstanceOf[Element]) {
              if (otCompatibility) {
                if (topicref.getAttribute("toc") == null) {
                  topicref.addAttribute(new Attribute("toc", "no"))
                }
              }
              (topicref("href"), topicref("scope")) match {
                case (Some(href), Some("local")) => {
                  val url = base.resolve(href)
                  val otherCells = relcells.filter(c => !(c eq relcell)).map(_.asInstanceOf[Element])
                  processRelations(topicref, otherCells, relations)
                }
                case _ =>
              }
            }    
          }
      }
    }
    Map.empty ++ relations
  }
  
  /**
   * Process relations for a reltable cell.
   * 
   * @param source source topicref
   */
  private def processRelations(source: Element, otherCells: List[Element], relations: mutable.Map[DitaURI, Relation]) {
    //println("process topicref")
    for {
      c <- otherCells;
      t <- c \ topicrefType;
      val target = t.asInstanceOf[Element]
    } {
      processRelation(source, target, relations)
      //processRelation(target, source, relations)
    }
  }
  private def processRelation(source: Element, target: Element, relations: mutable.Map[DitaURI, Relation]) {
    def getURI(u: URI): DitaURI = {
      // XXX: OT doesn't normalize targets
      DitaURI(if (found contains u) u.setFragment(found(u).id.get) else u)
    }    
    (source("href"), target("href"), source("linking"), target("linking")) match {
      case (_, _, Some("none"), _) =>
      case (_, _, Some("targetonly"), _) =>
      case (_, _, _, Some("sourceonly")) =>
      case (_, _, _, Some("none")) =>
      case (Some(s), Some(t), _, _) => {
        val sourceUrl = getURI(new URI(source.getBaseURI()).resolve(s))
        //val targetUrl = getURI(new URI(target.getBaseURI()).resolve(t))
        val relation = if (relations contains sourceUrl) {
            relations(sourceUrl)
          } else {
            val r = new Relation(sourceUrl)
            relations(sourceUrl) = r
            r
          }        
        //relation += targetUrl
        relation += target
        //println("add relation " + relation)
      }
      case _ =>
    }
  }
  
  /**
   * Map element walker.
   */
  private def walker(e: Element, base: URI, relations: Map[DitaURI, Relation]) {
    if ((e isType topicrefType) && e("href") != None && e("dead", Preprocessor.MUUNTAJA_NS) == None) {
      val f: URI = base.resolve(e("href").get)
      if (!(processed contains f)) {
        XMLUtils.parse(f) match {
          case Some(doc) => {
            topicWalker(e, doc.getRootElement, f, relations)
            XMLUtils.serialize(doc, f)
          }
          case _ =>
        }
        processed += f
      }
    }
    for (c <- e.getChildElements) walker(c, base, relations)
  }
  
  private def topicWalker(topicref: Element, e: Element, base: URI, relations: Map[DitaURI, Relation]) {
    if (e isType topicType) {
      val cur = DitaURI(base.setFragment(e("id").get))
      //println("Walking " + cur)
      if (relations contains cur) {
        val rel = e.getOrCreateElement(relatedLinksType, List(titleType, titlealtsType, shortdescType, abstractType, prologType, bodyType))
        val linkpool = rel.getOrCreateElement(linkpoolType, Nil)
        val relation = relations(cur)
        for (to <- relation.targets) {
          //val l = createElement(linkType, to)
          val l = to.copy.asInstanceOf[Element]
          to("href") match {
            case Some(th) => l.addAttribute(new Attribute("href", base.resolve(".").relativize(new URI(th.toString)).toString))
            case _ =>
          }
          l.addAttribute(new Attribute("role", "friend"))
          if (otCompatibility) {
            l.addAttribute(new Attribute("mapclass", topicref.getAttributeValue(Dita.ClassAttribute)))
          }
          linkpool.appendChild(l)
        }
      }
      /*
      
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
    for (c <- e.getChildElements) topicWalker(topicref, c, base, relations)   
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
  
  /**
   * TODO: Check how duplicate relations are defined in OT.
   * 
   * <p>Relation uses a DitaURI, but topicref elements can only refer to topics,
   * not element level structures. Thus an URI could be used, but for consistency this
   * is easier.</p>
   */
  private class Relation(val from: DitaURI) {
    private val to = new mutable.ListBuffer[Element]()
    def +=(add: DitaURI) {
      //println("add relation with URL")      
      val e = createElement(linkType)
      e.addAttribute(new Attribute("href", add.toString))
      to += e
    }
    def +=(ref: Element) {// linkType      
      val e = createElement(linkType)//, add)

      val atts = new QName("scope") :: new QName("href") :: Dita.inheretableMetadataAttributes
      for (n <- atts) {
        //println("  attr: " + n.getLocalPart + " = " + ref(n.getLocalPart, n.getNamespaceURI))
        ref(n.getLocalPart, n.getNamespaceURI) match {
          case Some(v) => e.addAttribute(new Attribute(n.getLocalPart, n.getNamespaceURI, v))
          case None =>
        } 
      }
      for (lt <- ref \ topicmetaType \ mapLinktextType toList) {
        e.appendChild(createElement(linktextType, lt))
      }
      for (lt <- ref \ topicmetaType \ mapShortdescType toList) {
        e.appendChild(createElement(descType, lt))
      }
      to += e
    }
    override def toString(): String = {
      val buf = new StringBuffer
      buf.append(from).append(" -> ")
      for (t <- to) {
        //buf.append(t("href").get).append(" ")
        buf.append(t.toXML).append(" ")
      }
      buf.toString
    }
    def targets: List[Element] = {
      to.toList
    }
  }
    
}