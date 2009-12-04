package com.github.muuntaja

import org.apache.commons.io.FileUtils
import javax.xml.namespace.QName
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import scala.collection.mutable
import java.io.{File, BufferedOutputStream, FileOutputStream, IOException, FileNotFoundException}
import java.net.URI
import org.xml.sax.helpers.{XMLFilterImpl, AttributesImpl}
import org.apache.xml.resolver.tools.ResolvingXMLReader
import org.apache.xml.resolver.CatalogManager
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.{SAXTransformerFactory, SAXSource}
import javax.xml.transform.stream.{StreamSource, StreamResult}
import nu.xom.{Attribute, Document, DocType, Element, Elements, Nodes, Node, Builder, Serializer}
import java.util.logging.Logger

//import XOM._
import XOM.{elementsToSeq, nodesToSeq}
import Dita._
import URIUtils._

/**
 * Preprocessor that normalizes DITA content:
 * 
 * <ul>
 * <li>Parse all DITA files and serialize them into temporary files. The <code>copy-to</code> attributes are respected.</li>
 * <li>Remove doctype declaration</li>
 * <li>Normalize start map with nested map references to a single map</li>
 * <li>Add <code>type</code> attribute
 * <li>Synchronize link text and navigation title from topic to topic references and vice versa, respecting <code>locktitle</code></li>
 * <li>Add processing attributes <code>xtrf</code> and <code>muuntaja:id</code></li>
 * <ul>
 */
class Preprocessor(val resource: File, val temp: File, val logger: Logger) {
  
  private val topicType = DitaType("- topic/topic ")
  private val titleType = DitaType("- topic/title ")
  private val navtitleType = DitaType("- topic/navtitle ")
  private val titlealtsType = DitaType("- topic/titlealts ")
  private val topicLinktextType = DitaType("- topic/linktext ")
  private val mapLinktextType = DitaType("- map/linktext ")
  private val shortdescType = DitaType("- topic/shortdesc ")
  private val abstractType = DitaType("- topic/abstract ")
  private val prologType = DitaType("- topic/prolog ")
  
  private val metadataType = DitaType("- topic/metadata ")
  private val authorType = DitaType("- topic/author ")
  private val sourceType = DitaType("- topic/source ")
  private val publisherType = DitaType("- topic/publisher ")
  private val copyrightType = DitaType("- topic/copyright ")
  private val critdatesType = DitaType("- topic/critdates ")
  private val permissionsType = DitaType("- topic/permissions ")
  private val resourceidType = DitaType("- topic/resourceid ")
  private val dataType = DitaType("- topic/data ")
  private val dataAboutType = DitaType("- topic/data-about ")
  private val foreignType = DitaType("- topic/foreign ")
  private val unknownType = DitaType("- topic/unknown ")
  private val audienceType = DitaType("- topic/audience ")
  private val categoryType = DitaType("- topic/category ")
  private val keywordsType = DitaType("- topic/keywords ")
  private val prodinfoType = DitaType("- topic/prodinfo ")
  private val othermetaType = DitaType("- topic/othermeta ")
  
  private val imageType = DitaType("- topic/image ")
  private val objectType = DitaType("- topic/object ")
  
  private val mapType = DitaType("- map/map ")
  private val topicrefType = DitaType("- map/topicref ")
  private val topicgroupType = DitaType("+ map/topicref mapgroup-d/topicgroup ")
  private val topicmetaType = DitaType("- map/topicmeta ") 
  private val bookmetaType = DitaType("- map/topicmeta bookmap/bookmeta ")
  private val chapterType = DitaType("- map/topicref bookmap/chapter ")
  private val frontmatterType = DitaType("- map/topicref bookmap/frontmatter ")
  private val backmatterType = DitaType("- map/topicref bookmap/backmatter ")
  private val appendicesType = DitaType("- map/topicref bookmap/appendices ")
  private val appendixType = DitaType("- map/topicref bookmap/appendix ")
  
  private val prologContents = List(authorType, sourceType, publisherType, copyrightType, critdatesType, permissionsType,
                                    metadataType, resourceidType, dataType, dataAboutType, foreignType, unknownType)
  private val metadataContents = List(audienceType, categoryType, keywordsType, prodinfoType, othermetaType, dataType,
                                      dataAboutType, foreignType, unknownType)
  
  implicit def elementToDitaElement(e: nu.xom.Element) =
    new DitaElement(e)
  
  private val normalized = (new File(temp, "normalized")).toURI
  val xmlUtils = new XMLUtils()
  xmlUtils.catalogFiles(new File(resource, "dtd" + File.separator + "catalog.xml"))
  
  /** Files that have been found */
  //private val found = mutable.Set[Tuple2[URI, String]]()
  //val found = mutable.HashMap[URI, String]()
  val found = mutable.HashMap[URI, DocInfo]()
  
  // Public methods ------------------------------------------------------------
  
  /**
   * Walk through all local links in file, normalizing target files 
   */
  def process(f: URI): URI = {
    if ((new File(f)).exists) {
      val base = f.resolve(".")
      xmlUtils.parseResolving(f, true) match {
        case Some(d) => {
          d.getRootElement.addNamespaceDeclaration("muuntaja", Preprocessor.MUUNTAJA_NS)
          mapWalker(d.getRootElement, f, base, Set(), List())
          //d.getRootElement.addAttribute(new Attribute(XMLConstants.XML_NS_PREFIX + ":base", XMLConstants.XML_NS_URI, normalized.toString))
          // serialize
          val out = normalized.resolve(base.relativize(f))
          /*
          val info = new DocInfo(d.getRootElement.getLocalName,
                                 d.query("*[contains(@class, ' map/topicmeta ')]/ *[contains(@class, ' map/linktext ')]/node()"),
                                 d.query("*[contains(@class, ' map/topicmeta ')]/ *[contains(@class, ' map/shortdesc ')]/node()"))
          */
          //found += (out -> d.getRootElement.getLocalName)
          val docInfo = DocInfo(d)
          found += (out -> docInfo)
          //found += (Preprocessor.changeFragment(out, d.getRootElement.getLocalName) -> docInfo)
          found += (out.setFragment(d.getRootElement.getLocalName) -> docInfo)
          XMLUtils.serialize(d, out)
          //for ((uri, docInfo) <- found.elements) logger.info("Included: " + uri)
          //found.foreach(f => logger.info("Included: " + f))
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
   * Element walker
   * 
   * @param e element to walk
   * @param f base URI for the current element
   * @param base base URI for the start file
   * @param inherited metadata
   */
  private def mapWalker(e: nu.xom.Element, base: URI, startBase: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    val rb = getBase(e, base)
    val ma = readMetaAttsr(e, metaAttrs)    
    val me = readMetaElems(e, metaElems)
    if (e isType topicrefType) {
      // add defaults
      if (e("format") == None) {
        e.addAttribute(new Attribute("format", "dita"))
      }
      if (e("scope") == None) {
        e.addAttribute(new Attribute("scope", "local"))
      } 
      // process
      val b = rb.resolve(".")
      (e("href"), e("format"), e("scope")) match {
        case (Some(href), Some("ditamap"), Some("local")) => { // local nested map
          val h = parseMapHref(href, b)
          nestedMap(e, h._1.get, startBase, ma, me)
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
            logger.fine("Add navtitle to peer/external")
            addTopicrefMeta(e, None)
          }
          for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
        }
      }
      if (false) { // OT retains these
        e.removeAttribute("navtitle")
        e.removeAttribute("locktitle")
      }
    //} else if (e isType mapType) {
    } else {
      for (c <- e.getChildElements) mapWalker(c, rb, startBase, ma, me)
    }
    e.removeAttribute("base", XMLConstants.XML_NS_URI)
  }
  
  /*
  private def processConref(e: Element) {
    e("conref") match {
      case Some(href) => {
        // TODO
      }
    }
  }
  */
  
  /**
   * Read inheritable metadata attributes.
   */
  private def readMetaAttsr(e: nu.xom.Element, oldAttrs: Set[Attribute]): Set[Attribute] = {
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
    DitaElement(ref).getFirstChildElement(topicmetaType) match {
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
  
  /**
   * Process topic
   * 
   * @param topicref topic reference element
   * @param topicUri absolute topic URI
   * @param targetUri relative target URI
   * @param base base URI of the document set
   * @param meta inherited metadata
   */
  private def topic(topicref: nu.xom.Element, topicUri: URI, targetUri: URI, base: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    logger.fine("Processing topic " + topicUri)
    val r: Option[Element] = topicref("scope") match {
      case Some("local") =>
        xmlUtils.parseResolving(topicUri, true) match {
          case Some(doc) => Some(doc.getRootElement)
          case _ => None
        }
      case _ => None
    }
    r match {
      case Some(root) => {
        // topic modifications
        root.addNamespaceDeclaration("muuntaja", Preprocessor.MUUNTAJA_NS)
        val newFile = normalized.resolve(targetUri)
        topicWalker(root, null, topicUri.resolve("."), newFile)//.resolve(".")
        addTopicMeta(root, topicref, metaAttrs, metaElems)
        // topicref modifications
        topicref.addAttribute(new Attribute("href", targetUri.toString))// + "#" + root.getAttributeValue("id")
        if (topicref("type") == None) {
          topicref.addAttribute(new Attribute("type", root.getLocalName))
        }
        addTopicrefMeta(topicref, r)
        // serialize
        XMLUtils.serialize(root.getDocument, newFile)
        //found += (newFile -> root.getLocalName)
        found += (newFile -> DocInfo(root.getDocument))
      }
      case None => {
        //topicref.removeAttribute(topicref.getAttribute("href"))
        topicref.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
      }
    }
    
    // walk topicref contents
    //for (child <- topicref.getChildElements) mapWalker(child, topicUri, base, metaAttrs, metaElems)
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
    val ti = if (e isType topicType) e.getAttributeValue("id") else topicId
    if (!(e isType topicType)) {
      e("id") match {
        case Some(a) => {
          if (!(e isType DitaType("- topic/resourceid "))) {
            e.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":id", Preprocessor.MUUNTAJA_NS, ti + "__" + a))
          }
        }
        case None => ()
      }
    }
    if (e isType topicType) {
      //found += (Preprocessor.changeFragment(dest, e.getAttributeValue("id")) -> DocInfo(e))
      found += (dest.setFragment(e.getAttributeValue("id")) -> DocInfo(e))
    } else if (e isType imageType) {
      if (e("scope") == None) { // DITA 1.2: image scope was introduced in 1.2
        e.addAttribute(new Attribute("scope", "local"))
      } 
      (e("href"), e("scope")) match {
        case (Some(href), Some("local")) => {
          val uri = src.resolve(href)
          //found += (uri -> "image")
          found += (uri -> new DocInfo(None, None, None, None))
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
    } else if (e isType objectType) {
      e("data") match {
        case Some(href) => {
          //found += (base.resolve(href) -> "data")
          found += (dest.resolve(href) -> new DocInfo(None, None, None, None))
        }
        case _ =>
      }
    }
    for (c <- e.getChildElements) topicWalker(c, ti, src, dest)
  }
  
  /**
   * Add meta from topicref to topic
   * 
   * @param d topic to add meta to
   * @param metaAttrs meta attributes to add
   */
  private def addTopicMeta(root: Element, topicref: Element, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    (topicref("locktitle"), topicref("navtitle")) match {
      case (Some("yes"), Some(t)) => {
        val titlealts = (new DitaElement(root)).getOrCreateElement(titlealtsType, List(titleType))
        val navtitle = titlealts.getOrCreateElement(navtitleType, Nil)
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
      val prolog = root.getOrCreateElement(prologType, List(titleType, titlealtsType, shortdescType, abstractType))
      for (metCls <- Dita.inheretablePrologElements) {
          for (met <- metaElems; if metCls._1 matches met) {
            val before = prologContents.takeWhile(t => !(t matches met)) ::: List(met.cls.get)
            prolog.insertChildAfter(met.copy.asInstanceOf[Element], before, !metCls._2)
          }
        
      }
      val metadata = prolog.getOrCreateElement(metadataType, List(authorType, sourceType, publisherType, copyrightType, critdatesType, permissionsType))
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
   * @param root topic to read meta from
   */
  private def addTopicrefMeta(topicref: Element, rootElement: Option[Element]) {
      val topicmeta = createElement(topicmetaType)
      
      // link text
      val linktext = rootElement match {
        case Some(root) => {
          val lt = createElement(mapLinktextType, root \ titleType first)
          topicmeta.appendChild(lt)
          Some(lt)
        }
        case None => None
      }
      
      // navigation title
      (rootElement, topicref("locktitle"), topicref("navtitle")) match {
        case (_, Some("yes"), Some(t)) => { // locked navtitle
          val n = createElement(navtitleType)
          n.appendChild(t)
          topicmeta.insertChild(n, 0)
        }
        case (None, _, Some(t)) => { // navtitle without topic source
          val n = createElement(navtitleType, Some(t))
          topicmeta.insertChild(n, 0)
        }
        case (Some(root), _, na) => {
          val nt: List[Node] = root.query("titlealts/navtitle").toList
          (nt, linktext, na) match {
            case (n :: ns, _, _) => { // navtitle from topic
              val nt = createElement(navtitleType, n)//createElement(n)
              topicmeta.insertChild(nt, 0)
            }
            case (_, _, Some(t)) => { // navtitle attribute
              val n = createElement(navtitleType)
              n.appendChild(t)
              topicmeta.insertChild(n, 0)
            }
            case (_, Some(lt), _) => { // copy of linktext
              val n = createElement(navtitleType, lt)
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
                n.addAttribute(new Attribute(Dita.classAttribute, navtitleType.toString))
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
      rootElement match {
        case Some(root) => {
          DitaElement(root).getFirstChildElement(shortdescType) match {
            case Some(s) => {
              topicmeta.appendChild(createElement(s))
            }
            case _ =>
          }
        }
        case _ =>
      }

      topicref.insertChild(topicmeta, 0)
    }
  
  /**
   * Process nested map
   * 
   * @param e topic reference element
   * @param f map URI
   * @param base base URI of the document set
   */
  private def nestedMap(topicref: nu.xom.Element, f: URI, base: URI, metaAttrs: Set[Attribute], metaElems: List[Element]) {
    val i = topicref.getParent.indexOf(topicref)
    xmlUtils.parseResolving(f, true) match {
      case Some(d) => {
        // replacement wrapper
        val group = createElement(topicgroupType)
        group.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":href", Preprocessor.MUUNTAJA_NS, f.toString))
        (new DitaElement(topicref)).getFirstChildElement(topicmetaType) match {
          case Some(m) => group.appendChild(m.copy)
          case None =>
        }
        // process children
        for (c <- d.getRootElement.getChildElements) {
          if ((c isType topicmetaType) || (c isType bookmetaType)) {
            // TODO process nested map metadata
          } else if (c isType topicrefType) {
            val cc = c.copy.asInstanceOf[Element]
            if (!cc.isType(frontmatterType, backmatterType, appendicesType, appendixType)) {
              if (cc.isType(chapterType)) {
                cc.setLocalName(topicrefType.localName)
                cc.addAttribute(new Attribute(Dita.ClassAttribute, topicrefType.toString))
              }
              group.appendChild(cc)
              mapWalker(cc, f, base, metaAttrs, metaElems)
            }
          }
        }
        topicref.getParent.insertChild(group, i)
        topicref.getParent.removeChild(topicref)
      }
      case None => {
        //topicref.removeAttribute(e.getAttribute("href"))
        topicref.addAttribute(new Attribute(Preprocessor.MUUNTAJA_PREFIX + ":dead", Preprocessor.MUUNTAJA_NS, "true"))
      }
    }  
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
   
  /**
   * Parse XML file using a catalog manager and serialize.
   * 
   * @param in input XML file
   * @param out output XML file
   */
  /*
  private def normalizeXML(in: File, out: File) {
    val parser: XMLReader = getResolvingParser
    val factory = TransformerFactory.newInstance
    val handler = (factory.asInstanceOf[SAXTransformerFactory]).newTransformerHandler()
    handler.setResult(new StreamResult(out))
    parser.setContentHandler(handler)
    parser.parse(new InputSource(in.toURL.toString))
  }
  */
  /*
  private def createElement(cls: String): nu.xom.Element = {
    createElement(new DitaType(cls))
  }
  
  private def createElement(cls: DitaType): nu.xom.Element = {
    val e = new Element(cls.localName)
    e.addAttribute(new Attribute(Dita.classAttribute, cls.toString))
    return e
  }
  */
}
object Preprocessor {

  val MUUNTAJA_NS = "http://github.com/jelovirt/muuntaja"
  val MUUNTAJA_PREFIX = "muuntaja"
  
  //val TopicmetaType = new DitaType("- map/topicmeta ")
  val TitleType = new DitaType("- topic/title ")
  val ShortdescType = new DitaType("- topic/shortdesc ")

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
   * Change URI fragment.
   * 
   * @param u URI to use as basis
   * @param fragment new fragment
   * @return URI with a new fragment
   */
  //private def changeFragment(u: URI, fragment: String): URI = 
  //  new URI(u.getScheme(),
  //          u.getUserInfo(), u.getHost(), u.getPort(),
  //          u.getPath(), u.getQuery(),
  //          fragment)
  
}
 