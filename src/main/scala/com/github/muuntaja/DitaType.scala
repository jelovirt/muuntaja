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
    elem.getAttributeValue(Dita.CLASS_ATTR) match {
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
    e.getAttributeValue(Dita.CLASS_ATTR) match {
      case null => throw new IllegalArgumentException("Element %s does not have a DITA class attribute: %s".format(e.getLocalName, e.toXML))
      case cls => new DitaType(cls)
    }
  }
}