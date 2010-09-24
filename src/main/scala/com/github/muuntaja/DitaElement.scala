package com.github.muuntaja


import java.net.URI
import java.util.regex.{Pattern, Matcher}
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}


class DitaElement(val element: Element) {
  val cls: Option[DitaType] = element.getAttributeValue(Dita.CLASS_ATTR) match {
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
    elem.getAttributeValue(Dita.CLASS_ATTR) match {
      case null => false
      case a => {
        val cls = new DitaType(a)
        ds.exists(cls matches _)
      }
    }
  }
}