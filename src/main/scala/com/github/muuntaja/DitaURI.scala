package com.github.muuntaja


import java.net.URI
import java.util.regex.{Pattern, Matcher}
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import nu.xom.{Element, Attribute, NodeFactory, ParentNode, Nodes, Node}


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
