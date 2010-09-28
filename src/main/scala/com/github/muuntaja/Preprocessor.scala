package com.github.muuntaja


import scala.collection.mutable

import java.io.{File, BufferedOutputStream, FileOutputStream, IOException, FileNotFoundException}
import java.net.URI
import java.util.logging.Logger

import javax.xml.namespace.QName
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.{SAXTransformerFactory, SAXSource}
import javax.xml.transform.stream.{StreamSource, StreamResult}

import nu.xom.{Attribute, Document, DocType, Element, Elements, Nodes, Node, ProcessingInstruction, Builder, Serializer}

import org.apache.xml.resolver.tools.ResolvingXMLReader
import org.apache.xml.resolver.CatalogManager
import org.apache.commons.io.FileUtils

import org.xml.sax.helpers.{XMLFilterImpl, AttributesImpl}

import XOM.{elementsToSeq, nodesToSeq, getPath}
import Dita._
import Dita.{Topic, Map, Bookmap}
import URIUtils._


/**
 * Preprocessor that normalizes DITA content:
 * 
 * <ul>
 * <li>Parse all DITA files and serialize them into temporary files.
 *   The <code>copy-to</code> attributes are respected.</li>
 * <li>Remove doctype declaration.</li>
 * <li>Normalize start map with nested map references to a single map.</li>
 * <li>Add <code>type</code> attribute.</li>
 * <li>Synchronize link text and navigation title from topic to topic
 *   references and vice versa, respecting <code>locktitle</code>.</li>
 * <li>Add processing attributes <code>xtrf</code> and <code>muuntaja:id</code>.</li>
 * <ul>
 * 
 * <p>The processor is not reusable or thread-safe.</p>
 */
class Preprocessor(
  val catalog: File,
  val temp: File,
  //val logger: Logger,
  val otCompatibility: Boolean = false) extends Generator {
  
  private val prologContents = List(Topic.Author, Topic.Source, Topic.Publisher, Topic.Copyright, Topic.Critdates, Topic.Permissions,
                                    Topic.Metadata, Topic.Resourceid, Topic.Data, Topic.DataAbout, Topic.Foreign, Topic.Unknown)
  private val metadataContents = List(Topic.Audience, Topic.Category, Topic.Keywords, Topic.Prodinfo, Topic.Othermeta, Topic.Data,
                                      Topic.DataAbout, Topic.Foreign, Topic.Unknown)
  private val topicMetaContents = List(Topic.Navtitle,
                                       Topic.Linktext, Topic.Searchtitle, Topic.Shortdesc, // topic classes
                                       Map.Linktext, Map.Searchtitle, Map.Shortdesc, // map classes
                                       Topic.Author, Topic.Source, Topic.Publisher, Topic.Copyright, Topic.Critdates, Topic.Permissions,
                                       Topic.Metadata, Topic.Audience, Topic.Category, Topic.Keywords, Topic.Prodinfo, Topic.Othermeta,
                                       Topic.Resourceid, Topic.Data, Topic.DataAbout, Topic.Foreign, Topic.Unknown)
                                        
  implicit def elementToDitaElement(e: nu.xom.Element) =
    new DitaElement(e)

  private var logger: Logger = _
  
  private val xmlUtils = new XMLUtils()
  xmlUtils.catalogFiles(catalog)
  /** Target directory. */
  private val normalized = (new File(temp, "normalized")).toURI
  /** Source document directory. */
  private var sharedBase: URI = _
  /**
   * Files processed thus far. The reason for not using the found map is that
   * it is filled only after the document is processed
   * and in depth first processing this will result in not detecting recursion.
   */
  private val processedFiles: mutable.Set[URI] = new mutable.HashSet[URI] 
  
  // Public methods ------------------------------------------------------------
  
  override def setLogger(logger: Logger) {
    this.logger = logger
  }
  
  /**
   * Walk through all local links in file, normalizing or copying target files.
   * 
   * @param f DITA map URI
   * @return preprocessed DITA map URI in a temporary directory
   */
  def process(f: URI): URI = {
    if ((new File(f)).exists) {
      logger.info("Processing start file " + f)
      val base = f.resolve(".")
      sharedBase = base
      xmlUtils.parseResolving(f, true, otCompatibility) match {
        case Some(d) => {
          val out = normalized.resolve(base.relativize(f))
          processedFiles += out
          
          val root = d.getRootElement
          root.addNamespaceDeclaration("muuntaja", Preprocessor.MUUNTAJA_NS)
          if (root isType Map.Map) {
            mapWalker(root, f, base, Set(), List())
          } else {
          topicWalker(root, null, base, out)
          }
          
          val docInfo = DocInfo(d)
          found += (out -> docInfo)
          found += (out.setFragment(root.getLocalName) -> docInfo)
          
          // serialize
//println(d.toXML)
          XMLUtils.serialize(d, out)
          
          for ((uri, docInfo) <- found.elements) logger.fine("Included: " + uri)
          for (uri <- processedFiles) logger.fine("Processed: " + uri)
          return out
        }
        case None => throw new Exception("Unable to parse " + f.toString)
      }
    } else {
      throw new FileNotFoundException(f.toString)
    }
  }
    
  // Private methods -----------------------------------------------------------
  
  /**
   * Process nested map
   * 
   * @param e topic reference element
   * @param mapUri map URI
   * @param targetURI target map URI
   * @param base base URI of the document set
   */
  private def map(topicref: nu.xom.Element, mapUri: URI, targetUri: URI, base: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    val newFile = normalized.resolve(targetUri)
    if (!(processedFiles contains newFile)) { 
      logger.info("Processing map " + mapUri)
      xmlUtils.parseResolving(mapUri, true, otCompatibility) match {
        case Some(doc) => {
          processedFiles += newFile
          val root = doc.getRootElement
          // map modifications
          mapWalker(root, mapUri, base, metaAttrs, metaElems)
          // topicref modifications
//          addTopicMeta(root, topicref, metaAttrs, metaElems)
//          topicref.addAttribute(new Attribute("href", targetUri.toString))// + "#" + root.getAttributeValue("id")
          found += (newFile -> DocInfo(doc))
          // serialize
          XMLUtils.serialize(doc, newFile)
          
          mergeMap(topicref, mapUri, doc)
        }
        case None => {
          topicref.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
        }
      }
    } else {
      //xmlUtils.parseResolving(newFile, true, otCompatibility) match {
      logger.fine("Reparsing map " + mapUri)
      xmlUtils.parse(newFile) match {
        case Some(doc) => {
          mergeMap(topicref, mapUri, doc)
        }
        case None =>
      }
    }
  }
  
  /**
   * Process map merge.
   */
  private def mergeMap(topicref: nu.xom.Element, mapUri: URI, doc: Document) {
    val insertTarget = if (otCompatibility) {
      topicref
    } else {
      // replacement wrapper
      val group = createElement(Map.Topicgroup)
      //group.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":href", Preprocessor.MUUNTAJA_NS, mapUri.toString))
      new DitaElement(topicref).getFirstChildElement(Map.Topicmeta) match {
        case Some(m) => group.appendChild(m.copy)
        case None =>
      }
      topicref.getParent.insertChild(group, topicref.getParent.indexOf(topicref))
      topicref.getParent.removeChild(topicref)
      val ins = new ProcessingInstruction("insert-point", "")
      group.appendChild(ins)
      ins
    }
    // process children
    val p = insertTarget.getParent
    for (c <- doc.getRootElement.getChildElements) {
      if ((c isType Map.Topicmeta) || (c isType Bookmap.Bookmeta)) {
        // TODO process nested map metadata
      } else if (c isType Map.Topicref) {
        val cc = c.copy.asInstanceOf[Element]
        if (!cc.isType(Bookmap.Frontmatter, Bookmap.Backmatter, Bookmap.Appendices, Bookmap.Appendix)) {
          if (cc.isType(Bookmap.Chapter)) {
            cc.setLocalName(Map.Topicref.localName)
            cc.addAttribute(new Attribute(Dita.CLASS_ATTR, Map.Topicref.toString))
          }
          
          p.insertChild(cc, p.indexOf(insertTarget))
        }
      }
    }
    insertTarget.getParent.removeChild(insertTarget)
  }
  
  /**
   * Map element walker.
   * 
   * @param e element to walk
   * @param f base URI for the current element
   * @param base base URI for the start file
   * @param inherited metadata
   */
  private def mapWalker(e: nu.xom.Element, base: URI, startBase: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    val rb = getBase(e, base)
    val ma = Preprocessor.readMetaAttsr(e, metaAttrs)    
    val me = Preprocessor.readMetaElems(e, metaElems)
    if (e isType Map.Topichead) {
      if (!otCompatibility) {
        addTopicrefMetaFromDocInfo(e, None, me)
      }
      for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
    } else if (e isType Map.Topicgroup) {
      //if (!otCompatibility) {
      //  addTopicrefMetaDocInfo(e, None, me)
      //}
      for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
    } else if (e isType Map.Topicref) {
      // add defaults
      if (e.attr("format") == None) {
        e.addAttribute(new Attribute("format", "dita"))
      }
      addDefaultScope(e)
      // process
      val b = rb.resolve(".")
      (e.attr("href"), e.attr("format"), e.attr("scope")) match {
        case (Some(href), Some("ditamap"), Some("local")) => { // local nested map
          val h = parseMapHref(href, b)
          val th = parseMapHref(e.getAttribute("copy-to") match {
            case null => href
            case a => e.removeAttribute(a); a.getValue
          }, b)
          val topicUri = h._1.get
          val targetUri = startBase.relativize(th._1.get)
          
          map(e, topicUri, targetUri, startBase, ma, me)
          // XXX: Should nested elements be walked?
        }
        case (Some(href), Some("dita"), Some("local")) => { // local topic
          val h = parseMapHref(href, b)
          val th = parseMapHref(e.getAttribute("copy-to") match {
            case null => href
            case a => e.removeAttribute(a); a.getValue
            }, b)
          val topicUri = h._1.get
          val targetUri = startBase.relativize(th._1.get)
          
          topic(e, topicUri, targetUri, startBase, ma, me)
          for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
        }
        case _ => { // other
          if (e.getAttribute("navtitle") != null) {
            //logger.fine("Add navtitle to peer/external")
            addTopicrefMetaFromDocInfo(e, None, me)
          }
          for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
        }
      }
      //if (!otCompatibility) {
      //  e.removeAttribute("navtitle")
      //  e.removeAttribute("locktitle")
      //}
    //} else if (e isType mapType) {
    } else {
      for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
    }
    DitaElement(e).removeAttribute("base", XMLConstants.XML_NS_URI)
  }
    
  /**
   * Process topic.
   * 
   * @param topicref topic reference element
   * @param topicUri absolute topic URI
   * @param targetUri relative target URI
   * @param base base URI of the document set
   * @param meta inherited metadata
   */
  private def topic(topicref: nu.xom.Element, topicUri: URI, targetUri: URI, base: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    val newFile = normalized.resolve(targetUri)
    (found.get(newFile), processedFiles.contains(newFile)) match {
      case (None, false) => {
      //if (!(processedFiles contains newFile)) {  
        logger.info("Processing topic " + topicUri)
        xmlUtils.parseResolving(topicUri, true, otCompatibility) match {
          case Some(doc) => {
            processedFiles += newFile
            val root = doc.getRootElement
            // topicref modifications
            val docInfo = DocInfo(doc)
            if (topicref isType Map.Topicref) {
              addTopicMeta(root, topicref, metaAttrs, metaElems)
//              topicref.addAttribute(new Attribute("href", targetUri.toString))// + "#" + root.getAttributeValue("id")
              if (topicref("type") == None) {
                topicref.addAttribute(new Attribute("type", root.getLocalName))
              }
              addTopicrefMetaFromDocInfo(topicref, Some(docInfo), metaElems)
            }
            found += (newFile -> docInfo) // add before walking to make docinfo available to recursion
            // topic modifications
            root.addNamespaceDeclaration("muuntaja", Preprocessor.MUUNTAJA_NS)
            topicWalker(root, null, topicUri.resolve("."), newFile)//.resolve(".")
            // serialize
            XMLUtils.serialize(root.getDocument, newFile)
          }
          case None => {
            topicref.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
            if (otCompatibility) {
              if (topicref isType Map.Topicref) {
                addTopicrefMetaFromDocInfo(topicref, None, metaElems)
              }
            }
          }
        }
      }
      case (Some(docInfo), true) => {
        logger.fine("Skip reparsing, use document info " + newFile)
          // topicref modifications
          if (topicref isType Map.Topicref) {
            topicref.addAttribute(new Attribute("href", targetUri.toString))// + "#" + root.getAttributeValue("id")
            if (topicref("type") == None) {
              topicref.addAttribute(new Attribute("type", docInfo.ditaType.get))
            }
            addTopicrefMetaFromDocInfo(topicref, Some(docInfo), metaElems)
          }
          // else if (topicref isType Topic.Xref) {
          //  if (topicref("type") == None) {
          //    topicref.addAttribute(new Attribute("type", docInfo.ditaType.get))
          //  } 
          //}
      }
      case _ => {
        topicref.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
          if (otCompatibility) {
            if (topicref isType Map.Topicref) {
              addTopicrefMetaFromDocInfo(topicref, None, metaElems)
            }
          }
      }
    }
  }
  
  /**
   * Topic walker.
   * 
   * <ul>
   * <li>add <code>muuntaja:id</code> attribute</li>
   * </ul>
   * 
   * @param e element to process
   * @param topicId ID of the parent document
   * @param src source file URI
   * @param dest target URI for output
   */
  private def topicWalker(e: nu.xom.Element, topicId: String, src: URI, dest: URI) {
    //processConref(e);
    val ti = if (e isType Topic.Topic) e.getAttributeValue("id") else topicId
    if (!(e isType Topic.Topic)) {
      e.attr("id") match {
        case Some(a) => {
          if (!(e isType Topic.Resourceid)) {
            e.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":id", Preprocessor.MUUNTAJA_NS, ti + "__" + a))
          }
        }
        case None => ()
      }
    }
    if (e isType Topic.Topic) {
      //found += (Preprocessor.changeFragment(dest, e.getAttributeValue("id")) -> DocInfo(e))
      found += (dest.setFragment(e.getAttributeValue("id")) -> DocInfo(e))
    } else if ((e isType Topic.Xref) || (e isType Topic.Link)) {
      addDefaultScope(e)
      (e.attr("href"), e.attr("scope")) match {
        case (Some(href), Some("local")) => {
          //val uri = src.resolve(href)
          val h = parseMapHref(href, src)
          val topicUri = h._1 match {
            case Some(u) => u
            case None => new URI(e.getBaseURI)
          }
          //val base = dest.resolve(".")
          val targetUri = sharedBase.relativize(topicUri)        
          topic(e, topicUri, targetUri, sharedBase, Set.empty, Nil)
        }
        case _ =>
      }

    } else if (e isType Topic.Image) {
      addDefaultScope(e)
      (e.attr("href"), e.attr("scope")) match {
        case (Some(href), Some("local")) => {
          val uri = src.resolve(href)
          found += (uri -> DocInfo.empty)
          val in = new File(uri)
          if (in.exists) {
            val out = new File(dest.resolve(href))
            //logger.debug("Copying " + in + " to " + out)
            FileUtils.copyFile(in, out)
          } else {
            e.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
          }
        }
        case _ =>
      }
    } else if (e isType Topic.Object) {
      e.attr("data") match {
        case Some(href) => {
          val uri = dest.resolve(href)
          found += (uri -> DocInfo.empty)
        }
        case _ =>
      }
    }
    for (c <- e.getChildElements) topicWalker(c, ti, src, dest)
  }
  
  private def addDefaultScope(e: Element) {
    //if (e.attr("scope") == None) { // DITA 1.2: image scope was introduced in 1.2
    //  e.addAttribute(new Attribute("scope", "local"))
    //}
    (e.getAttribute("scope"), e.attr("scope")) match {
      case (null, Some(a)) => if (otCompatibility) e.addAttribute(new Attribute("scope", a))
      case (null, None) => e.addAttribute(new Attribute("scope", "local"))
      case _ =>
    }
  }
  
  /**
   * Add meta from topicref to topic.
   * 
   * @param root root of the topic to add meta to
   * @param topicref topic reference
   * @param metaAttrs meta attributes to add
   * @param metaElement meta elements to add
   */
  private def addTopicMeta(root: Element, topicref: Element, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    (topicref.attr("locktitle"), topicref.attr("navtitle")) match {
      case (Some("yes"), Some(t)) => {
        val titlealts = (new DitaElement(root)).getOrCreateElement(Topic.Titlealts, List(Topic.Title))
        val navtitle = titlealts.getOrCreateElement(Topic.Navtitle, Nil)
        navtitle.removeChildren()
        navtitle.appendChild(t)
      }
      case _ => ()
    }
    for (met <- metaAttrs) {
      root.getAttribute(met.getLocalName, met.getNamespaceURI) match {
        case null => {
          root.addAttribute(new Attribute(met.getLocalName, met.getNamespaceURI, met.getValue))
        }
        case a => ()
      }
    }
    if (!metaElems.isEmpty) {
      val prolog = root.getOrCreateElement(Topic.Prolog, List(Topic.Title, Topic.Titlealts, Topic.Shortdesc, Topic.Abstract))
      for (metCls <- Dita.inheretablePrologElements) {
        for (met <- metaElems; if metCls._1 matches met) {
          val before = prologContents.takeWhile(t => !(t matches met)) ::: List(met.cls.get)
          prolog.insertChildAfter(met.copy.asInstanceOf[Element], before, !metCls._2)
        }
      }
      val metadata = prolog.getOrCreateElement(Topic.Metadata, List(Topic.Author, Topic.Source, Topic.Publisher, Topic.Copyright, Topic.Critdates, Topic.Permissions))
      for (metCls <- Dita.inheretableMetadataElements) {
        for (met <- metaElems; if metCls._1 matches met) {
          val before = metadataContents.takeWhile(t => !(t matches met)) ::: List(met.cls.get) 
          metadata.insertChildAfter(met.copy.asInstanceOf[Element], before, !metCls._2)
        }
      }
    }
  }
  
  /**
   * Add meta from topic to topicref
   * 
   * @param topicref topicref to add meta to
   * @param rootElement topic to read meta from
   * @param metaElement meta elements to add
   */
  private def addTopicrefMetaFromDocInfo(topicref: Element, docInfo: Option[DocInfo], metaElems: List[Element]) {
    val topicmeta = topicref.getOrCreateElement(Map.Topicmeta)
      
    // topic reference titles
    val linktext: Option[Element] = topicmeta.getFirstChildElement(Map.Linktext) match {
      case None => docInfo match {
        case Some(info) => info.title match {
        case Some(title) => {
          val lt = createElement(Map.Linktext, title)
          topicmeta.appendChild(lt)
          //topicmeta.appendChild(new ProcessingInstruction("ditaot", "gentext"))
          Some(lt)
        }
        case None => None
        }
        case None => None
      }
      case lt => lt 
    }
    
    // navigation title
    (docInfo, topicref.attr("locktitle"), topicref.attr("navtitle")) match {
      case (_, Some("yes"), Some(t)) => { // locked navtitle attribute from map
        val n = createElement(Topic.Navtitle)
        n.appendChild(t)
        topicmeta.insertChild(n, 0)
      }
      case (None, _, Some(t)) => { // navtitle without topic source
        if (linktext.isEmpty) {
          val l = createElement(Topic.Linktext, Some(t))
          topicmeta.insertChild(l, 0)
        }
        if ((topicmeta \ Topic.Navtitle).size == 0) { 
          val n = createElement(Topic.Navtitle, Some(t))
          topicmeta.insertChild(n, 0)
        }
      }
      case (Some(info), _, navtitleAttr) => {
        val nt: Option[Node] = info.navTitle// XXX: This should be info.navTitle //(root \ Topic.Titlealts \ Topic.Navtitle).toList.headOption
        val title: Option[Node] = info.title //(root \ Topic.Title).toList.headOption
        (nt, linktext, navtitleAttr, title) match {
          case (Some(n), _, _, _) => { // navtitle from topic
            // XXX: OT prefers navtitle from topic
            topicmeta.getFirstChildElement(Topic.Navtitle) match {
              case Some(tl) => topicmeta.removeChild(tl)
              case None =>
            }
            val nt = createElement(Topic.Navtitle, n)
            topicmeta.insertChild(nt, 0)
          }
          case (_, Some(lt), _, Some(t)) => { // title from topic
            // XXX: OT prefers navtitle from topic
            topicmeta.getFirstChildElement(Topic.Navtitle) match {
              case Some(tl) => topicmeta.removeChild(tl)
              case None =>
            }
            val n = createElement(Topic.Navtitle, t)
            topicmeta.insertChild(n, 0)
          }
          case (_, _, Some(t), _) => { // navtitle attribute
            val n = createElement(Topic.Navtitle)
            n.appendChild(t)
            topicmeta.insertChild(n, 0)
          }
          case (_, Some(lt), _, _) => { // copy of linktext
            val n = createElement(Topic.Navtitle, lt)
            topicmeta.insertChild(n, 0)
          }
          case _ =>
        }
        /*
        if (nt.size > 0) {
          topicmeta.insertChild(nt.first.copy.asInstanceOf[Element], 0)
        } else {
          linktext match {
            case Some (lt) => {
              val n = lt.copy.asInstanceOf[Element]
              n.setLocalName(navtitleType.localName)
              n.addAttribute(new Attribute(Dita.CLASS_ATTR, navtitleType.toString))
              topicmeta.insertChild(n, 0)
            }
            case _ =>
          }  
        }
        */
      }
      case _ =>
    }
    
    // short description
    topicmeta.getFirstChildElement(Map.Shortdesc) match {
      case None => docInfo match {
        case Some(info) => {
          info.desc match {
            case Some(s) => {
              topicmeta.appendChild(createElement(Map.Shortdesc, s))
            }
            case _ =>
          }
        }
        case _ =>
      }
      case _ =>
    }
    
    // FIXME: E.g. searchtitle comes in topic and map base, we should reclass here to correct base
    if (!metaElems.isEmpty) {
      for (metCls <- Dita.inheretableMetaElements) {
        for (met <- metaElems; if metCls._1 matches met) {
          val before = topicMetaContents.takeWhile(t => !(t matches met)) ::: List(met.cls.get) 
          topicmeta.insertChildAfter(met.copy.asInstanceOf[Element], before, !metCls._2)
        }
      }
    }

    //topicref.insertChild(topicmeta, 0)
  }
  
  /**
   * Add meta from topic to topicref
   * 
   * @param topicref topicref to add meta to
   * @param rootElement topic to read meta from
   * @param metaElement meta elements to add
   */
  @Deprecated
  private def addTopicrefMeta(topicref: Element, rootElement: Option[Element], metaElems: List[Element]) {
    val topicmeta = topicref.getOrCreateElement(Map.Topicmeta)
    
    // topic reference titles
    val linktext = topicmeta.getFirstChildElement(Map.Linktext) match {
      case None => rootElement match {
        case Some(root) => {
          val lt = createElement(Map.Linktext, root \ Topic.Title head)
          topicmeta.appendChild(lt)
          Some(lt)
        }
        case None => None
      }
      case lt => lt 
    }
    
    // navigation title
    (rootElement, topicref.attr("locktitle"), topicref.attr("navtitle")) match {
      case (_, Some("yes"), Some(t)) => { // locked navtitle attribute from map
        val n = createElement(Topic.Navtitle)
        n.appendChild(t)
        topicmeta.insertChild(n, 0)
      }
      case (None, _, Some(t)) => { // navtitle without topic source
        if (linktext.isEmpty) {
          val l = createElement(Topic.Linktext, Some(t))
          topicmeta.insertChild(l, 0)
        }
        if ((topicmeta \ Topic.Navtitle).size == 0) { 
          val n = createElement(Topic.Navtitle, Some(t))
          topicmeta.insertChild(n, 0)
        }
      }
      case (Some(root), _, navtitleAttr) => {
        val nt: Option[Node] = (root \ Topic.Titlealts \ Topic.Navtitle).toList.headOption
        val title: Option[Node] = (root \ Topic.Title).toList.headOption
        (nt, linktext, navtitleAttr, title) match {
          case (Some(n), _, _, _) => { // navtitle from topic
            // XXX: OT prefers navtitle from topic
            topicmeta.getFirstChildElement(Topic.Navtitle) match {
              case Some(tl) => topicmeta.removeChild(tl)
              case None =>
            }
            val nt = createElement(Topic.Navtitle, n)
            topicmeta.insertChild(nt, 0)
          }
          case (_, Some(lt), _, Some(t)) => { // title from topic
            // XXX: OT prefers navtitle from topic
            topicmeta.getFirstChildElement(Topic.Navtitle) match {
              case Some(tl) => topicmeta.removeChild(tl)
              case None =>
            }
            val n = createElement(Topic.Navtitle, t)
            topicmeta.insertChild(n, 0)
          }
          case (_, _, Some(t), _) => { // navtitle attribute
            val n = createElement(Topic.Navtitle)
            n.appendChild(t)
            topicmeta.insertChild(n, 0)
          }
          case (_, Some(lt), _, _) => { // copy of linktext
            val n = createElement(Topic.Navtitle, lt)
            topicmeta.insertChild(n, 0)
          }
          case _ =>
        }
        /*
        if (nt.size > 0) {
          topicmeta.insertChild(nt.first.copy.asInstanceOf[Element], 0)
        } else {
          linktext match {
            case Some (lt) => {
              val n = lt.copy.asInstanceOf[Element]
              n.setLocalName(navtitleType.localName)
              n.addAttribute(new Attribute(Dita.CLASS_ATTR, navtitleType.toString))
              topicmeta.insertChild(n, 0)
            }
            case _ =>
          }  
        }
        */
      }
      case _ =>
    }
    
    // short description
    topicmeta.getFirstChildElement(Map.Shortdesc) match {
      case None => rootElement match {
        case Some(root) => {
          DitaElement(root).getFirstChildElement(Topic.Shortdesc) match {
            case Some(s) => {
              topicmeta.appendChild(createElement(s))
            }
            case _ =>
          }
        }
        case _ =>
      }
      case _ =>
    }
    
    // FIXME: E.g. searchtitle comes in topic and map base, we should reclass here to correct base
    if (!metaElems.isEmpty) {
      for (metCls <- Dita.inheretableMetaElements) {
        for (met <- metaElems; if metCls._1 matches met) {
          val before = topicMetaContents.takeWhile(t => !(t matches met)) ::: List(met.cls.get) 
          topicmeta.insertChildAfter(met.copy.asInstanceOf[Element], before, !metCls._2)
        }
      }
    }

    //topicref.insertChild(topicmeta, 0)
  }
  
  
  /**
   * Get element base URL.
   */
  private def getBase(e: Element, base: URI): URI = {
    e.getAttributeValue("base", XMLConstants.XML_NS_URI) match {
      case null => return base
      case a => {
        val u = new URI(a)
        if (u.isAbsolute) {
          return u
        } else {
          return base.resolve(u) 
        }
      }
    }
  }
  
}
object Preprocessor {

  val MUUNTAJA_NS = "http://github.com/jelovirt/muuntaja"
  val MUUNTAJA_PREFIX = "muuntaja"
  
  /**
   * Read inheritable metadata attributes.
   */
  def readMetaAttsr(e: nu.xom.Element, oldAttrs: Set[Attribute]): Set[Attribute] = {
    val met = e.getChildElements()
    var attrs = Set[Attribute]() ++ oldAttrs
    for (n <- Dita.inheretableMetadataAttributes) {
      e.getAttribute(n.getLocalPart, n.getNamespaceURI()) match {
        case null => ()
        case a => attrs = attrs + new Attribute(a)
      } 
    }
    return attrs
  }
  
  /**
   * Read inheritable metadata elements from topicmeta.
   */
  private def readMetaElems(ref: nu.xom.Element, oldElems: List[Element]): List[Element] = {
    val elems = new mutable.ListBuffer[Element]()
    elems ++= oldElems
    DitaElement(ref).getFirstChildElement(Map.Topicmeta) match {
      case Some(tm) => {
        for (n <- Dita.inheretableMetaElements) {
          for (me <- (new DitaElement(tm)).getChildElements(n._1)) {
            elems += me
          }
        }
      }
      case None => ()
    }
    return elems.toList 
  }
  
}