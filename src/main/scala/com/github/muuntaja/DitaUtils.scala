package com.github.muuntaja


import java.net.URI
import java.util.regex.{Pattern, Matcher}
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}


object Dita {

  val Namespace = "http://dita.oasis-open.org/architecture/2005/"
  val CLASS_ATTR = "class"
  
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
    lazy val Keydef = DitaType("+ map/topicref mapgroup-d/keydef ")
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
    e.addAttribute(new Attribute(Dita.CLASS_ATTR, cls.toString))
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
      if (!((a.getLocalName == Dita.CLASS_ATTR && a.getNamespaceURI == "")
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