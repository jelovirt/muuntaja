package com.github.muuntaja

import nu.xom.{Document, Element}
import java.net.URI
import java.util.regex.{Pattern, Matcher}

import XOM._

class ConrefProcessor {

  private val topicrefType = new DitaType("- map/topicref ")
  
  private val xmlUtils = new XMLUtils()
  
  private implicit def elementToDitaElement(e: nu.xom.Element) =
    new DitaElement(e)
  
  var mapChanged = false
  var topicChanged = false
  
  def process(ditamap: URI) {
    XMLUtils.parse(ditamap) match {
      case Some(doc) => {
        mapWalker(doc.getRootElement, ditamap.resolve("."))
        XMLUtils.serialize(doc, new URI(ditamap.toString + ".conref.xml"))
      }
      case None => ()
    }
  }
  
  /**
   * Map walker for conref.
   */
  private def mapWalker(e: Element, base: URI) {
    e("conref") match {
      case Some(href) => {
        val f = base.resolve((new URI(href)).getPath)
        XMLUtils.parse(f) match {
          case Some(doc) => {
            // TODO
          }
          case None => ()
        }
      }
      case _ => {
        if (e isType topicrefType) {
          (e("href"), e("format"), e("scope")) match {
            case (Some(href), Some("dita"), Some("local")) => {
              val f = base.resolve((new URI(href)).getPath)
              XMLUtils.parse(f) match {
                case Some(doc) => {
                  topicWalker(doc.getRootElement, f.resolve("."))
                }
                case None => ()
              }
            }
            case _ => ()
          }
        }
        for (c <- e.getChildElements) mapWalker(c, base)
      }
    }
  }
 
  /**
   * Topic walker for conref.
   */
  private def topicWalker(e: Element, base: URI) {
    e("conref") match {
      case Some(href) => {
        val f = base.resolve((new URI(href)).getPath)
        XMLUtils.parse(f) match {
          case Some(doc) => {
            // TODO
          }
          case None => ()
        }
      }
      case _ => {
        for (c <- e.getChildElements) topicWalker(c, base)
      }
    } 
  }
}