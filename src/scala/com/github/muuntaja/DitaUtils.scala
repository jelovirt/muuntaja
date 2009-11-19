package com.github.muuntaja

import java.net.URI
import org.scalatest.Suite
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}
import java.util.regex.{Pattern, Matcher}
import scala.xml.Elem

/**
 * DITA elemet class.
 */
case class DitaType(val domain: Boolean, val cls: List[(String,String)]) {
  override val toString = (if (domain) "+ " else "- ") + (cls.map(t => t._1 + "/" + t._2) mkString " ") + " "
  val localName = cls.last._2
  def this(cls: String) {
    this(cls.charAt(0) == '+', cls.substring(1).trim().split("\\s+").map(s => {
        val sa = s.split("/")                                                                     
        (sa(0), sa(1))
      }).toList)
  }
  override def hashCode(): Int = {
    toString.hashCode
  }
  override def equals(other: Any) = other match {
    case that: DitaType => that.toString == this.toString
    case _ => false
  }
  def matches(elem: nu.xom.Element): Boolean = {
    elem.getAttributeValue(Dita.ClassAttribute) match {
      case null => false
      case c => matches(new DitaType(c))
    }
  }
  def matches(ref: DitaType): Boolean = {
    toString.substring(1).startsWith(ref.toString.substring(1))
    //toString.startsWith(ref.toString)
  }
}
object DitaType {
  def apply(cls: String): DitaType = {
    new DitaType(cls)
  }
  def apply(e: Element): DitaType = {
    e.getAttributeValue(Dita.ClassAttribute) match {
      case null => throw new IllegalArgumentException("Element %s does not have a DITA class attribute: %s".format(e.getLocalName, e.toXML))
      case cls => new DitaType(cls)
    }
  }
}
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

class DitaElement(val element: Element) {
  val cls: Option[DitaType] = element.getAttributeValue(Dita.ClassAttribute) match {
    case null => None
    case a => Some(new DitaType(a))
  }
  
  /**
   * Get attribute in null namespace. For DITA inheritable attributes parents are searched.
   * 
   * @param n attribute local name
   */
  def apply(n: String): Option[String] =
    apply(n, "")

  /**
   * Get attribute in namespace. For DITA inheritable attributes parents are searched.
   * 
   * @param n attribute local name
   * @param uri attribute namespace URI
   */
  def apply(n: String, uri: String): Option[String] = {
    if (Dita.inheretableAttributes.exists(qn => qn.getLocalPart == n && qn.getNamespaceURI == uri)) {
      var e: ParentNode = element
      while (e != null && e.isInstanceOf[Element]) {
        e.asInstanceOf[Element].getAttributeValue(n, uri) match {
          case null => 
          case v: String => return Some(v)
        }
        e = e.getParent
      }
      return None
    } else {
      element.getAttributeValue(n, uri) match {
        case null => return None
        case v: String => return Some(v)
      }
    } 
  }
  def removeAttribute(n: String) {
    element.getAttribute(n) match {
      case null => ()
      case a => element.removeAttribute(a)
    }
  }
  def removeAttribute(localName: String, namespaceUri: String) {
    element.getAttribute(localName, namespaceUri) match {
      case null => ()
      case a => element.removeAttribute(a)
    }
  }
  def update(n: String, v: String) {
    element.addAttribute(new Attribute(n, v))
  }
  /**
   * Get children projection.
   */
  @Deprecated
  def getChildren =
    for (n <- 0 until element.getChildCount) yield element.getChild(n)
  /**
   * Get child elements of given type.
   */
  def getChildElements(ds: DitaType*) = {
    val es = element.getChildElements
    for {
      i <- 0 until es.size
      val c = es.get(i)
      if ds.exists(_ matches c)
    } yield c
  }
  /**
   * Get first child element of given type.
   */
  def getFirstChildElement(ds: DitaType*): Option[nu.xom.Element] = {
    val es = element.getChildElements
    for (i <- 0 until es.size) {
      val e = es.get(i)
      val c = DitaType(e)
      if (ds.exists(c matches _)) {
        return Some(e)
      }
    }
    None
  }
  
  /**
   * Get existing child element or create a new one.
   * 
   * @param cls class of the element to return
   * @param before list of elements classes that are before the target element
   */
  def getOrCreateElement(cls: DitaType, before: List[DitaType]): nu.xom.Element = {
    this.getFirstChildElement(cls) match {
      case Some(e) => e // return existing if found
      case None => {
        val newElem = new Element(cls.localName)
        newElem.addAttribute(new Attribute(Dita.ClassAttribute, cls.toString))
        this.getIndex(before) match {
          case Some(index) => {
            element.insertChild(newElem, index + 1);
          }
          case None => {
            element.insertChild(newElem, 0);
          }
        }
        return newElem
      }
    }
  }
  
  /**
   * Insert element before last of the listed elements.
   * 
   * @param elem element to insert, must be a DITA element
   * @param before list of elements classes that are before the target element
   */
  def insertChildAfter(elem: Element, before: List[DitaType], replace: Boolean) {
    this.getIndex(before) match {
      case Some(index) => {
        val e = element.getChild(index).asInstanceOf[Element]
        if (DitaType(elem) matches e) { // is same
          if (replace) {
            element.removeChild(index)
            element.insertChild(elem, index);             
          } else {
            element.insertChild(elem, index);
          }
        } else {
          element.insertChild(elem, index + 1);
        }
      }
      case None => {
        element.insertChild(elem, 0);
      }
    }
  }

  /**
   * Get index of the first occurrance of the listed types in reverse order.
   */
  def getIndex(before: List[DitaType]): Option[Int] = {
    for (b <- before.reverse) {
      this.getFirstChildElement(b) match {
        case Some(ins) => {
          return Some(element.indexOf(ins))
        }
        case None => ()
      }
    }
    return None
  }
  
  /**
   * Test if element is of given type
   * 
   * @param ds types to match
   */
  def isType(ds: DitaType*): Boolean = {
    cls match {
      case Some(c) => ds.exists(c matches _)
      case None => false
    }
  }
}
object DitaElement {
  def apply(e: nu.xom.Element) = {
    new DitaElement(e)
  }
  def isClassType(elem: nu.xom.Element, ds: DitaType*): Boolean = {
    elem.getAttributeValue(Dita.ClassAttribute) match {
      case null => false
      case a => {
        val cls = new DitaType(a)
        ds.exists(cls matches _)
      }
    }
  }
}
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

object Dita {

  val Namespace = "http://dita.oasis-open.org/architecture/2005/"
  val ClassAttribute = "class"
  
  /** Topic ID pattern: $1 URI, $2 topic ID, $3 element ID */
  val TopicIdPattern = Pattern.compile("^(.*?)(?:#(.+?)(?:/(.+))?)?$")
  /** Map ID pattern: $1 URI, $2 element ID */
  val MapIdPattern = Pattern.compile("^(.*?)(?:#(.+?))?$")
  
  implicit def nodesToDitaNodesUtil(nodes: Nodes): DitaNodesUtil =
    new DitaNodesUtil(nodes)
  implicit def elementToDitaNodesUtil(elem: Element): DitaNodesUtil =
    new DitaNodesUtil(new Nodes(elem))
  
  class DitaNodesUtil(override val nodes: Nodes) extends XOM.NodesUtil(nodes) {
    def \(that: DitaType): Nodes = {
      find((e) => DitaElement.isClassType(e, that))
    }
  }
  
  @Deprecated
  def createElement(cls: String): nu.xom.Element = {
    createElement(new DitaType(cls))
  }
  def createElement(cls: DitaType): nu.xom.Element = {
    createElement(cls, None)
  }
  def createElement(cls: DitaType, ref: Element): nu.xom.Element = {
    val e = createElement(cls)
    for (i <- 0 until ref.getAttributeCount) {
      val a = ref.getAttribute(i)
      if (!((a.getLocalName == "id" && a.getNamespaceURI == "")
            || (a.getLocalName == "id" && a.getNamespaceURI == Preprocessor.MUUNTAJA_NS))) {
        e.addAttribute(a.copy.asInstanceOf[Attribute])
      }
    }
    for (i <- 0 until ref.getChildCount)
      e.appendChild(ref.getChild(i).copy)
    return e
  }
  def createElement(cls: DitaType, ref: Node): nu.xom.Element =
    createElement(cls, ref.asInstanceOf[Element])
  
  def createElement(cls: DitaType, text: Option[String]): nu.xom.Element = {
    val e = new Element(cls.localName)
    e.addAttribute(new Attribute(Dita.ClassAttribute, cls.toString))
    text match {
      case Some(t) => e.appendChild(t)
      case _ =>
    }
    return e
  }
  
  def copyElement(n: Node): Element =
    copyElement(n.asInstanceOf[Element])
  
  def copyElement(e: Element): Element =
    createElement(DitaType(e), e)

  /*
  def getTopicURI(href: String): URI =
    getTopicURI(new URI(href))
  
  def getTopicURI(href: URI): URI = {
    val f = href.getRawFragment.trim.split('/')
    if (f.length > 0)
      new URI(href.getScheme(),
              href.getUserInfo(), href.getHost(), href.getPort(),
              href.getPath(), href.getQuery(),
              f(0))
    else
      new URI(href.getScheme(),
              href.getUserInfo(), href.getHost(), href.getPort(),
              href.getPath(), href.getQuery(),
              null)
  }
  */
    
  /**
   * Parse URI in DITA topic.
   * 
   * TODO: Use URI for parsing
   * 
   * @param href URI to parse
   * @param base base URI
   */
  def parseTopicHref(href: String, base: URI): (Option[URI], Option[String], Option[String]) = {
    val matcher = TopicIdPattern.matcher(href)
    if (matcher.matches) {
      (matcher.group(1) match {
        case null => None
        case s => if (s.length == 0) None else Some(base.resolve(new URI(s)))
       },
       matcher.group(2) match {
        case null => None
        case s => Some(s)
       },
       matcher.group(3) match {
        case null => None
        case s => Some(s)
       })
    } else {
      (None, None, None)
    }
  }
  
  /**
   * Parse URI in DITA map.
   * 
   * TODO: Use URI for parsing
   * 
   * @param href URI to parse
   * @param base base URI
   */
  def parseMapHref(href: String, base: URI): (Option[URI], Option[String]) = {
    val matcher = TopicIdPattern.matcher(href)
    if (matcher.matches) {
      (matcher.group(1) match {
        case null => None
        case s => if (s.length == 0) None else Some(base.resolve(new URI(s)))
       },
       matcher.group(2) match {
        case null => None
        case s => Some(s)
       })
    } else {
      (None, None)
    }
  }
  
  val inheretableMetadataAttributes = List[QName](
    new QName("audience"),
    new QName("platform"),
    new QName("product"),
    new QName("otherprops"),
    new QName("rev"),
    new QName("props")
    //new QName("linking"),
    //new QName("toc"),
    //new QName("print"),
    //new QName("search"),
    //new QName("format"),
    //new QName("scope"),
    //new QName("type"),
    //new QName(XMLConstants.XML_NS_URI, "lang", "xml"),
    //new QName("dir"),
    //new QName("translate")
  )

  val inheretableAttributes = List[QName](
    new QName("linking"),
    new QName("toc"),
    new QName("print"),
    new QName("search"),
    new QName("format"),
    new QName("scope"),
    new QName("type"),
    new QName(XMLConstants.XML_NS_URI, "lang", "xml"),
    new QName("dir"),
    new QName("translate"))

  /**
   * List of inheritable prolog elements. Boolean part is a flag that denotes if the element may occur multiple times.
   */
  val inheretablePrologElements = List[(DitaType, Boolean)](
    (DitaType("- topic/author "), true),
    (DitaType("- topic/publisher "), false),
    (DitaType("- topic/copyright "), true),
    (DitaType("- topic/critdates "), false),
    (DitaType("- topic/permissions "), false))
  val inheretableMetadataElements = List[(DitaType, Boolean)](
    (DitaType("- topic/audience "), true),
    (DitaType("- topic/category "), true),
    (DitaType("- topic/prodinfo "), true))
  val inheretableMetaElements =
    inheretablePrologElements ::: inheretableMetadataElements
  
}
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

case class DitaURI(val uri: URI, val topic: Option[String], val element: Option[String]) {
  override val toString: String = {
    val buf = new StringBuilder
    buf.append(uri)
    if (!topic.isEmpty) {
      buf.append('#')
      buf.append(topic.get)
      if (!element.isEmpty) {
        buf.append('/')
        buf.append(element.get)
      }
    }
    buf.toString
  }
  val topicURI: URI =
    new URI(uri.getScheme(),
            uri.getUserInfo(), uri.getHost(), uri.getPort(),
            uri.getPath(), uri.getQuery(),
            if (topic.isEmpty) null else topic.get)
}
object DitaURI {
  def apply(href: URI): DitaURI = {
    val u = href.normalize
    val res = new URI(u.getScheme(),
                      u.getUserInfo(), u.getHost(), u.getPort(),
                      u.getPath(), u.getQuery(),
                      null)
    val (t, e) = u.getRawFragment match {
      case null => (None, None)
      case s => s.trim match {
        case "" => (None, None)
        case ss => {
          val sa = ss.split('/')
          if (sa.length == 2) {
            (Some(sa(0)), Some(sa(1)))
          } else {
            (Some(sa(0)), None)
          }
        }
      }
    }
    new DitaURI(res, t, e)
  }
}
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