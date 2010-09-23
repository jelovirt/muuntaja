package com.github.muuntaja

import java.net.URI
import java.util.regex.{Pattern, Matcher}
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}

/**
 * DITA elemet class.
 */
case class DitaType(val domain: Boolean, val cls: List[(String,String)]) {
  /**
   * Constructor from a DITA class.
   * 
   * @param cls DITA class
   */
  def this(cls: String) {
    this(cls.charAt(0) == '+', cls.substring(1).trim().split("\\s+").map(s => {
        val sa = s.split("/")                                                                     
        (sa(0), sa(1))
      }).toList)
  }
  override lazy val toString = (if (domain) "+ " else "- ") + (cls.map(t => t._1 + "/" + t._2) mkString " ") + " "
  /** Element name. */
  val localName = cls.last._2
  override def hashCode(): Int = {
    toString.hashCode
  }
  override def equals(other: Any) = other match {
    case that: DitaType => that.toString == this.toString
    case _ => false
  }
  /**
   * Test if argument is a subtype of this type 
   * @param elem DITA element
   * @return true if is subtype of this type, otherwise false
   */
  def matches(elem: nu.xom.Element): Boolean = {
    elem.getAttributeValue(Dita.ClassAttribute) match {
      case null => false
      case c => matches(new DitaType(c))
    }
  }
  /**
   * Test if argument is a subtype of this type 
   * @param ref DITA type
   * @return true if is subtype of this type, otherwise false
   */
  def matches(ref: DitaType): Boolean = {
    toString.substring(1).startsWith(ref.toString.substring(1))
    //toString.startsWith(ref.toString)
  }
}
object DitaType {
  /**
   * Construct new DITA type from a DITA class.
   * 
   * @param cls DITA class
   */
  def apply(cls: String): DitaType = {
    new DitaType(cls)
  }
  /**
   * Construct new DITA type from a DITA element
   * 
   * @param e DITA element
   */
  def apply(e: Element): DitaType = {
    e.getAttributeValue(Dita.ClassAttribute) match {
      case null => throw new IllegalArgumentException("Element %s does not have a DITA class attribute: %s".format(e.getLocalName, e.toXML))
      case cls => new DitaType(cls)
    }
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
  @Deprecated
  def apply(n: String): Option[String] =
    attr(n, "")

  /**
   * Get attribute in null namespace. For DITA inheritable attributes parents are searched.
   * 
   * @param n attribute local name
   */
  def attr(n: String): Option[String] =
    attr(n, "")
    
  /**
   * Get attribute in namespace. For DITA inheritable attributes parents are searched.
   * 
   * @param n attribute local name
   * @param uri attribute namespace URI
   */
  @Deprecated
  def apply(n: String, uri: String): Option[String] =
	  attr(n, uri)
	  
  /**
   * Get attribute in namespace. For DITA inheritable attributes parents are searched.
   * 
   * @param n attribute local name
   * @param uri attribute namespace URI
   */
  def attr(n: String, uri: String): Option[String] = {
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
  
  /*
  def removeAttribute(n: String) {
    element.getAttribute(n) match {
      case null => ()
      case a => element.removeAttribute(a)
    }
  }
  */
  def removeAttribute(localName: String, namespaceUri: String = "") {
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
  def getOrCreateElement(cls: DitaType, before: List[DitaType] = Nil): nu.xom.Element = {
    this.getFirstChildElement(cls) match {
      case Some(e) => e // return existing if found
      case None => {
    	val newElem = Dita.createElement(cls)
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

object Dita {

  val Namespace = "http://dita.oasis-open.org/architecture/2005/"
  val ClassAttribute = "class"
  
  object Topic {
    lazy val Abstract = DitaType("- topic/abstract ")
    lazy val Audience = DitaType("- topic/audience ")
    lazy val Author = DitaType("- topic/author ")
    lazy val Body = DitaType("- topic/body ")
    lazy val Category = DitaType("- topic/category ")
    lazy val Copyright = DitaType("- topic/copyright ")
    lazy val Critdates = DitaType("- topic/critdates ")
    lazy val Data = DitaType("- topic/data ")
    lazy val DataAbout = DitaType("- topic/data-about ")
    lazy val Desc = DitaType("- topic/desc ")
    lazy val Foreign = DitaType("- topic/foreign ")
    lazy val Image = DitaType("- topic/image ")
    lazy val Keyword = DitaType("- topic/keyword ")
    lazy val Keywords = DitaType("- topic/keywords ")
    lazy val Link = DitaType("- topic/link ")
    lazy val Linkpool = DitaType("- topic/linkpool ")
    lazy val Linktext = DitaType("- topic/linktext ")
    lazy val Metadata = DitaType("- topic/metadata ")
    lazy val Navtitle = DitaType("- topic/navtitle ")
    lazy val Object = DitaType("- topic/object ")
    lazy val Othermeta = DitaType("- topic/othermeta ")
    lazy val Permissions = DitaType("- topic/permissions ")
    lazy val Prodinfo = DitaType("- topic/prodinfo ")
    lazy val Prolog = DitaType("- topic/prolog ")
    lazy val Publisher = DitaType("- topic/publisher ")
    lazy val RelatedLinks = DitaType("- topic/related-links ")
    lazy val Resourceid = DitaType("- topic/resourceid ")
    lazy val Searchtitle = DitaType("- topic/searchtitle ")
    lazy val Shortdesc = DitaType("- topic/shortdesc ")
    lazy val Source = DitaType("- topic/source ")
    lazy val Title = DitaType("- topic/title ")
    lazy val Titlealts = DitaType("- topic/titlealts ")
    lazy val Topic = DitaType("- topic/topic ")
    lazy val Unknown = DitaType("- topic/unknown ")
    lazy val Xref = DitaType("- topic/xref ")
  }
  object Map {
    lazy val Linktext = DitaType("- map/linktext ")
    lazy val Map = DitaType("- map/map ")
    lazy val Reltable = DitaType("- map/reltable ")
    lazy val Relrow = DitaType("- map/relrow ")
    lazy val Relcell = DitaType("- map/relcell ")
    lazy val Relheader = DitaType("- map/relheader ")
    lazy val Relcolspec = DitaType("- map/relcolspec ")
    lazy val Searchtitle = DitaType("- map/searchtitle ")
    lazy val Shortdesc = DitaType("- map/shortdesc ")
    lazy val Topicgroup = DitaType("+ map/topicref mapgroup-d/topicgroup ")
    lazy val Topichead = DitaType("+ map/topicref mapgroup-d/topichead ")
    lazy val Topicmeta = DitaType("- map/topicmeta ")
    lazy val Topicref = DitaType("- map/topicref ")
    lazy val Keydef = DitaType("+ map/topicref mapgropup-d/keydef ")
  }
  object Bookmap {
    lazy val Appendices = DitaType("- map/topicref bookmap/appendices ")
    lazy val Appendix = DitaType("- map/topicref bookmap/appendix ")
    lazy val Backmatter = DitaType("- map/topicref bookmap/backmatter ")
    lazy val Bookmeta = DitaType("- map/topicmeta bookmap/bookmeta ")
    lazy val Chapter = DitaType("- map/topicref bookmap/chapter ")
    lazy val Frontmatter = DitaType("- map/topicref bookmap/frontmatter ")
  }
  
  /** Topic ID pattern: $1 URI, $2 topic ID, $3 element ID */
  val TopicIdPattern = Pattern.compile("^(.*?)(?:#(.+?)(?:/(.+))?)?$")
  /** Map ID pattern: $1 URI, $2 element ID */
  val MapIdPattern = Pattern.compile("^(.*?)(?:#(.+?))?$")
  
  implicit def nodesToDitaNodesUtil(nodes: Nodes): DitaNodesUtil =
    new DitaNodesUtil(nodes)
  implicit def elementToDitaNodesUtil(elem: Element): DitaNodesUtil =
    new DitaNodesUtil(new Nodes(elem))
  implicit def elementToDitaElement(e: nu.xom.Element) =
    new DitaElement(e)
  
  class DitaNodesUtil(override val nodes: Nodes) extends XOM.NodesUtil(nodes) {
    def \(that: DitaType): Nodes = {
      find((e) => DitaElement.isClassType(e, that))
    }
  }

  def createElement(cls: DitaType): nu.xom.Element = {
    val e = new Element(cls.localName)
    e.addAttribute(new Attribute(Dita.ClassAttribute, cls.toString))
    if (false) { // debug
      val st = (new RuntimeException).getStackTrace.dropWhile(s => s.getClassName == this.getClass.getCanonicalName)
      e.addAttribute(new Attribute("xtrc", st map {t => t.getFileName + ":" + t.getLineNumber} mkString "; "))
    }
    e
  }
  @Deprecated
  def createElement(cls: String): nu.xom.Element = {
    createElement(new DitaType(cls))
  }
  def createElement(cls: DitaType, ref: Element): nu.xom.Element = {
    val e = createElement(cls)
    for (i <- 0 until ref.getAttributeCount) { // TODO: This should loop through inheritable attributes too
      val a = ref.getAttribute(i)
      if (!((a.getLocalName == Dita.ClassAttribute && a.getNamespaceURI == "")
            || (a.getLocalName == "id" && a.getNamespaceURI == "")
            || (a.getLocalName == "id" && a.getNamespaceURI == Preprocessor.MUUNTAJA_NS))) {
        e.addAttribute(a.copy.asInstanceOf[Attribute])
      }
    }
    for (i <- 0 until ref.getChildCount)
      e.appendChild(ref.getChild(i).copy)
    return e
  }  
  @Deprecated
  def createElement(cls: DitaType, ref: Node): nu.xom.Element =
    createElement(cls, ref.asInstanceOf[Element])
  /** Create element with given text content. */
  def createElement(cls: DitaType, text: Option[String]): nu.xom.Element = {
    val e = createElement(cls)
    text match {
      case Some(t) => e.appendChild(t)
      case _ =>
    }
    e
  }
  def createElement(n: Node): Element =
    createElement(n.asInstanceOf[Element])
  def createElement(e: Element): Element =
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
  
  lazy val inheretableMetadataAttributes = List[QName](
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

  lazy val inheretableAttributes = List[QName](
    new QName("linking"),
    new QName("toc"),
    new QName("print"),
    new QName("search"),
    new QName("format"),
    new QName("scope"),
    new QName("type"),
    new QName(XMLConstants.XML_NS_URI, "lang", "xml"),
    new QName("dir"),
    new QName("importance"),
    new QName("translate")) ::: inheretableMetadataAttributes

  /**
   * List of inheritable prolog elements. Boolean part is a flag that denotes if the element may occur multiple times.
   */
  lazy val inheretablePrologElements = List[(DitaType, Boolean)](
    (Topic.Author, true),
    (Topic.Publisher, false),
    (Topic.Copyright, true),
    (Topic.Critdates, false),
    (Topic.Permissions, false))
  lazy val inheretableMetadataElements = List[(DitaType, Boolean)](
    (Topic.Audience, true),
    (Topic.Category, true),
    (Topic.Prodinfo, true))
  lazy val inheretableMetaElements =
    inheretablePrologElements ::: inheretableMetadataElements
  
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
