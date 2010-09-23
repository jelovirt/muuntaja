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
 * <li>fill related links from relationship tables</li>
 * <li>add link text and description</li>
 * <li>mark dead links</li>
 * </ul>
 */
class RelatedLinksGenerator(val otCompatibility: Boolean = false) extends Generator {
  
  private implicit def elementToDitaElement(e: Element) =
    new DitaElement(e)

  // Private variables ---------------------------------------------------------
  
  //private val processed = mutable.HashSet[URI]()
 
  private var log: Logger = _
  
  // Public variables ----------------------------------------------------------
  
  //var found: mutable.Map[URI, DocInfo] = _
  
  // Public functions ----------------------------------------------------------
  
  //override def setDocInfo(f: mutable.Map[URI, DocInfo]) {
  //  found = f
  //}
  
  override def setLogger(logger: Logger) {
    log = logger
  }
  
  override def process(ditamap: URI): URI = {
    XMLUtils.parse(ditamap) match {
      case Some(doc) => {
        val relations: Map[DitaURI, Relation] = getRelations(doc, ditamap)
        //relations.values.map(println)
        for (
          (u, d) <- found.iterator
          if u.getFragment == null && d.ditaType.isDefined
        ) {
          //println("Relation processing")
          XMLUtils.parse(u) match {
            case Some(doc) => {
              topicWalker(doc.getRootElement, u, relations)
              XMLUtils.serialize(doc, u)
            }
            case _ =>
          }
        }
        XMLUtils.serialize(doc, ditamap)
        ditamap
      }
      case None => throw new Exception("Unable to parse " + ditamap.toString)
    }
  }
  
  // Private functions ---------------------------------------------------------
  
  /**
   * Get relations from root reltable
   */
  private def getRelations(doc: Document, base: URI): Map[DitaURI, Relation] = {
    val relations = mutable.HashMap[DitaURI, Relation]()
    val reltables = doc.getRootElement \ Dita.Map.Reltable
    // relationship tables
    for (r <- reltables; val reltable = r.asInstanceOf[Element]) {
      val relrows = reltable \ Dita.Map.Relrow
      // rows
      for (row <- relrows; val relrow = row.asInstanceOf[Element]) {
        val relcells = relrow \ Dita.Map.Relcell
        // cells
        for (c <- relcells; val relcell = c.asInstanceOf[Element]) {
          val topics = relcell \ Dita.Map.Topicref
          // topic refereces
          for (t <- topics; val topicref = t.asInstanceOf[Element]) {
            if (otCompatibility) {
              if (topicref.getAttribute("toc") == null) {
                topicref.addAttribute(new Attribute("toc", "no"))
              }
              (topicref.getAttribute("linking"), topicref("linking")) match {
              case (null, Some(linking)) => topicref.addAttribute(new Attribute("linking", linking))
              case _ =>
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
      t <- c \ Dita.Map.Topicref;
      val target = t.asInstanceOf[Element]
    } {
      processRelation(source, target, relations)
      //processRelation(target, source, relations)
    }
  }
  
  /**
   * Process a single relationship table relation.
   * 
   * @param source source topic reference
   * @param target target topic reference
   * @param relations relationship table to add relation to
   */
  private def processRelation(source: Element, target: Element, relations: mutable.Map[DitaURI, Relation]) {
    def getURI(u: URI): DitaURI = {
      // XXX: OT doesn't normalize targets
      DitaURI(if (found contains u) u.setFragment(found(u).id.get) else u)
    }
    (source("href"), target("href"), source("linking"), target("linking"), source("format")) match {
      case (_, _, Some("none"), _, _) =>
      case (_, _, Some("targetonly"), _, _) =>
      case (_, _, _, Some("sourceonly"), _) =>
      case (_, _, _, Some("none"), _) =>
      case (Some(s), Some(t), _, _, Some("dita")) => {
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
  /*
  private def walker(e: Element, base: URI, relations: Map[DitaURI, Relation]) {
    if ((e isType Dita.Map.Topicref) && e("href") != None && e("dead", Preprocessor.MUUNTAJA_NS) == None) {
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
  */
  
  /**
   * Walk topic references.
   */
  private def topicWalker(e: Element, base: URI, relations: Map[DitaURI, Relation]) {//topicref: Element, 
    if (e isType Topic.Topic) {
      val cur = DitaURI(base.setFragment(e("id").get))
      //println("Walking " + cur)
      if (relations contains cur) {
        val rel = e.getOrCreateElement(Topic.RelatedLinks, List(Topic.Title, Topic.Titlealts, Topic.Shortdesc, Topic.Abstract, Topic.Prolog, Topic.Body))
        val linkpool = rel.getOrCreateElement(Topic.Linkpool)
        val relation = relations(cur)
        for (to <- relation.targets) {
          val l = createElement(Topic.Link, to)
          //val l = to.copy.asInstanceOf[Element]
          to("href") match {
            case Some(th) => l.addAttribute(new Attribute("href", base.resolve(".").relativize(new URI(th.toString)).toString))
            case _ =>
          }
          l.addAttribute(new Attribute("role", "friend"))
//          if (otCompatibility) {
//            l.addAttribute(new Attribute("mapclass", topicref.getAttributeValue(Dita.ClassAttribute)))
//          }
          linkpool.appendChild(l)
        }
      }
      /*
      
      // ancestors
      var p = topicref.getParent
      while (p != null && p.isInstanceOf[Element]) {
        DitaElement(p.asInstanceOf[Element])("href") match {
          case Some(href) => {
            val link = createElement(Topic.Link)
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
    } else if (e.isType(Topic.Link, Topic.Xref)) {
      processLink(e, base)
    }
    for (c <- e.getChildElements) topicWalker(c, base, relations)//topicref,    
  }
  
  /**
   * Process a link element.
   * 
   * <p>Link elements are <code>xref</code> and <code>link</code> elements and
   * their specializations.</p>
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
    (e("href"), e("scope"), e("format")) match {
      case (Some(href), Some("local"), Some("dita")) => {
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
          if (e isType Topic.Link) {
            if ((e \ Topic.Linktext).size == 0) {
              docInfo.title match {
                case Some(t) => e.insertChild(createElement(Topic.Linktext, t), 0)
                case None => {
                  if (otCompatibility) {
                    e.insertChild(createElement(Topic.Linktext, Some(href)), 0)
                  }
                }
              }
            }
          } else if (e isType Topic.Xref) {
            if (e.getChildCount == 0) {
              docInfo.title match {
                case Some(t) => for (c <- t.getChildren) e.appendChild(c.copy)
                case None =>
              }
            }
          }
          //if (!(otCompatibility && (e isType Topic.Xref))) { // XXX: OT doens't add desc if linktext is missing.
          if ((e \ Topic.Desc).size == 0) {
            docInfo.desc match {
              case Some(d) => e.appendChild(createElement(Topic.Desc, d))
              case None =>
            }
          }
          //}
        } else {
          //if (!otCompatibility) {
          e.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
          //}
        }
      }
      case (Some(href), Some("external"), _) => {
        if (otCompatibility) { // XXX: OT functionality that's wrong I think
          if (e isType Topic.Link) {
            if ((e \ Topic.Linktext).size == 0) {
              val h = createElement(Topic.Linktext)
              h.appendChild(href)
              e.insertChild(h, 0)
            }
          } else if (e isType Topic.Xref) {
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
  /** List of link elements. */
    private val links = new mutable.ListBuffer[Element]()
    /** Add external scope relationship. */
    def +=(add: DitaURI) {
      //println("add relation with URL")      
      val e = createElement(Topic.Link)
      e.addAttribute(new Attribute("href", add.toString))
      links += e
    }
    /** Add local scope relationship. */
    def +=(ref: Element) {// Map.Topicref
      val link = createElement(Topic.Link)//, add)

      val atts = if (otCompatibility) {
              new QName("format") ::
              new QName("scope") ::
              new QName("href") ::
              new QName("importance") ::
              Dita.inheretableMetadataAttributes filterNot { a: QName => a.getLocalPart == "props" && a.getNamespaceURI == ""}
             } else {
              new QName("format") ::
              new QName("scope") ::
              new QName("href") ::
              Dita.inheretableMetadataAttributes
             }
      for (a <- atts) {
        ref(a.getLocalPart, a.getNamespaceURI) match {
          case Some(v) => link.addAttribute(new Attribute(a.getLocalPart, a.getNamespaceURI, v))
          case None =>
        } 
      }
      for (lt <- ref \ Dita.Map.Topicmeta \ Dita.Map.Linktext toList) {
        link.appendChild(createElement(Topic.Linktext, lt))
      }
      for (lt <- ref \ Dita.Map.Topicmeta \ Dita.Map.Shortdesc toList) {
        link.appendChild(createElement(Topic.Desc, lt))
      }
      links += link
    }
    override def toString(): String = {
      val buf = new StringBuffer
      buf.append(from).append(" -> ")
      for (t <- links) {
        //buf.append(t("href").get).append(" ")
        buf.append(t.toXML).append(" ")
      }
      buf.toString
    }
    def targets: List[Element] = {
      links.toList
    }
  }
    
}