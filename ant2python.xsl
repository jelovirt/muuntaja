<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:x="x"
  exclude-result-prefixes="xs x"
  version="2.0">

  <xsl:import href="ant-base2python.xsl"/>

  <xsl:output method="text"/>
  
  <xsl:strip-space elements="*"/>
  
  <xsl:template match="node() | @*" mode="merge" priority="-1">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*" mode="merge"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="project" mode="merge">
    <xsl:copy>
      <xsl:attribute name="file" select="base-uri(.)"/>
      <xsl:apply-templates select="@*" mode="merge"/>
      <xsl:apply-templates select="*[not(self::import)]" mode="merge"/>
      <xsl:apply-templates select="import[contains(@file, 'org.dita.base')]" mode="merge">
        <xsl:with-param name="include" select="true()"/>
      </xsl:apply-templates>
    </xsl:copy>
    
    <xsl:for-each select="import[not(contains(@file, 'org.dita.base'))]">
      <xsl:sort select="contains(@file, 'org.dita.base')" order="descending"/>
      <xsl:sort select="contains(@file, 'org.dita')" order="descending"/>
      <xsl:apply-templates select="." mode="merge"/>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="import" mode="merge">
    <xsl:param name="include" select="false()"/>
    <xsl:variable name="file"
                  select="if (starts-with(@file, '${dita.plugin.org.dita.pdf2.dir}'))
                          then substring-after(@file, '${dita.plugin.org.dita.pdf2.dir}/')
                          else @file"/>
    <xsl:apply-templates select="if ($include)
                                 then document($file, .)/project/*
                                 else document($file, .)/project" mode="merge"/>
  </xsl:template>
  
  <xsl:key name="target" match="target" use="@name"/>
  
  <xsl:template match="/">
    <xsl:variable name="merged" as="document-node()">
      <xsl:document>
        <xsl:apply-templates select="*" mode="merge"/>
      </xsl:document>
    </xsl:variable>
    <xsl:text>import os.path
import sys
import urllib
import xml.etree.ElementTree
import shutil

import org.dita.dost.pipeline.PipelineHashIO as PipelineHashIO
import org.dita.dost.module.ModuleFactory as ModuleFactory
import org.dita.dost.log.DITAOTJavaLogger as DITAOTJavaLogger
import org.dita.dost.resolver.DitaURIResolverFactory as DitaURIResolverFactory

import java.io.File as File

import javax.xml.transform.TransformerFactory as TransformerFactory 
import javax.xml.transform.stream.StreamSource as StreamSource
import javax.xml.transform.stream.StreamResult as StreamResult

class Properties(dict):
    """Global properties store."""
    def __setitem__(self, name, value):
        if not self.__contains__(name):
            super(Properties, self).__setitem__(name, value)
    def force_setitem(self, name, value):
        super(Properties, self).__setitem__(name, value)
    def __getitem__(self, name):
        if not self.__contains__(name):
            return "${" + name + "}"
        else:
            return super(Properties, self).__getitem__(name)

properties = Properties()

logger = DITAOTJavaLogger()

history = []
def depends(*funcs):
    """Run dependencies."""
    for f in funcs:
        if f not in history:
            history.append(f)
            f()

def read_xml_properties(props):
    """Read XML property file to global properties."""
    f = open(props, "r")
    d = xml.etree.ElementTree.parse(f)
    f.close()
    for p in d.getroot():
        k = p.attrib["key"]
        v = p.text
        if v is None:
            v = ""
        properties.force_setitem(k, v)

def read_properties(p):
    """Read property file to global properties."""
    f = open(p, "r")
    for l in f.readlines():
        if l[0] != "#" and len(l.strip()) > 0:
            k, v = [i.strip() for i in l.split("=", 1)]
            properties[k] = v
    f.close()

properties["basedir"] = os.path.abspath(".")

def class_available(c):
    """Check if class is available."""
    # TODO
    return True

def is_absolute(p):
    """Check if path is absolute."""
    return p[0] == os.sep

def copy(src, dst, includes):
    """Copy files by pattern."""
    for i in includes.split(","):
        s = os.path.join(src, i)
        d = os.path.join(dst, i)
        if os.path.exists(s):
            if not os.path.exists(os.path.dirname(d)):
                os.makedirs(os.path.dirname(d))
            print "Copy " + s + " to " + d
            shutil.copy(s, d)
        else:
            print "Skip copy, " + s + " does not exist"

def copy_list(src, dst, includesfile):
    """Copy files by pattern file."""
    f = open(includesfile, "r")
    for l in f.readlines():
        copy(src, dst, l.strip())
    f.close()

</xsl:text>
    <xsl:apply-templates select="$merged/*"/>
  </xsl:template>
  
  <xsl:template match="project">
    <xsl:variable name="indent" select="'    '"/>
    
    <xsl:variable name="project" select="."/>
    <xsl:variable name="depends" as="xs:string*">
      <xsl:for-each select="target/@depends/tokenize(., ',')">
        <xsl:variable name="v" select="normalize-space(.)"/>
        <xsl:if test="empty(key('target', $v, $project))">
          <xsl:sequence select="x:getClass(key('target', $v, root($project))/../@name)"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    
    <xsl:text>class </xsl:text>
    <xsl:value-of select="x:getClass(@name)"/>
    <xsl:text>(</xsl:text>
    <!--xsl:choose>
      <xsl:when test="@name = 'DOST'">object</xsl:when>
      <xsl:otherwise>DOST</xsl:otherwise>
    </xsl:choose-->
    <xsl:text>object</xsl:text>
    <xsl:text>):&#xA;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text># </xsl:text>
    <xsl:value-of select="@file"/>
    <xsl:text>&#xA;&#xA;</xsl:text>
    
    <xsl:value-of select="$indent"/>
    <xsl:text>def __init__(self):&#xA;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    super(</xsl:text>
    <xsl:value-of select="x:getClass(@name)"/>
    <xsl:text>, self).__init__()&#xA;</xsl:text>
    
    <xsl:value-of select="$indent"/>
    <xsl:text>    properties["ant.file.</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>"] = os.path.abspath("</xsl:text>
    <xsl:value-of select="substring-after(@file, 'file:/Users/jelovirt/Work/SF/dita-ot/src/main/')"/>
    <xsl:text>")&#xA;</xsl:text>
    
    <xsl:for-each select="distinct-values($depends)">
      <xsl:value-of select="$indent"/>
      <xsl:text>    self.</xsl:text>
      <xsl:value-of select="x:getInstance(.)"/>
      <xsl:text> = </xsl:text>
      <xsl:value-of select="."/>
      <xsl:text>()&#xA;</xsl:text>
    </xsl:for-each>
     <xsl:apply-templates select="*[empty(self::target)]">
       <xsl:with-param name="indent" tunnel="yes" select="concat($indent, '    ')"/>
     </xsl:apply-templates>    
    <xsl:text>&#xA;</xsl:text>
    <xsl:apply-templates select="target">
      <xsl:with-param name="indent" tunnel="yes" select="'    '"/>
    </xsl:apply-templates>
  </xsl:template>
  
  <xsl:function name="x:getInstance" as="xs:string">
    <xsl:param name="name"/>
    <xsl:value-of select="concat(lower-case(substring($name, 1, 1)),
                                 substring($name, 2))"/>
  </xsl:function>
  
  <xsl:function name="x:getClass" as="xs:string">
    <xsl:param name="name"/>
    <xsl:value-of>
     <xsl:for-each select="tokenize($name, '[\.-]')">
       <xsl:value-of select="concat(upper-case(substring(., 1, 1)),
                                    substring(., 2))"/>
     </xsl:for-each>
    </xsl:value-of>
  </xsl:function>
  
  <xsl:function name="x:getMethod" as="xs:string">
    <xsl:param name="name"/>
    <xsl:value-of select="replace($name, '[\.-]', '_')"/>
  </xsl:function>
  
  <xsl:template match="project/target">
    <xsl:param name="indent" tunnel="yes" select="''"/>
    <xsl:if test="exists(@desciption)">
      <xsl:value-of select="$indent"/>
      <xsl:text># </xsl:text>
      <xsl:value-of select="@desciption"/>
      <xsl:text>&#xa;</xsl:text>  
    </xsl:if>
    <!--xsl:value-of select="$indent"/>
    <xsl:text>@staticmethod&#xA;</xsl:text-->
    <xsl:value-of select="$indent"/>
    <xsl:text>def </xsl:text>
    <xsl:value-of select="x:getMethod(@name)"/>
    <xsl:text>(self):&#xa;</xsl:text>
    <xsl:if test="exists(@description)">
      <xsl:value-of select="$indent"/>
      <xsl:text>    print "</xsl:text>
      <xsl:value-of select="@description"/>
      <xsl:text>"&#xa;</xsl:text>
    </xsl:if>
    <xsl:variable name="body">
      <xsl:if test="@depends">
        <xsl:value-of select="$indent"/>
        <xsl:text>    depends(</xsl:text>
        <xsl:variable name="t" select="."/>
        <xsl:for-each select="tokenize(@depends, ',')">
          <xsl:variable name="n" select="normalize-space(.)"/>
          <xsl:if test="position() ne 1">, </xsl:if>
          <xsl:text>self.</xsl:text>
          <xsl:if test="empty(key('target', $n, $t/..))">
            <xsl:value-of select="x:getInstance(x:getClass(key('target', $n, root($t))/../@name))"/>
            <xsl:text>.</xsl:text>
          </xsl:if>
          <xsl:value-of select="x:getMethod(normalize-space(.))"/>
        </xsl:for-each>
        <xsl:text>)&#xa;</xsl:text>
      </xsl:if>
      <xsl:if test="@if">
        <xsl:value-of select="$indent"/>
        <xsl:text>    if "</xsl:text>
        <xsl:value-of select="@if"/>
        <xsl:text>" not in properties: return&#xa;</xsl:text>
        <!--xsl:text>        print "  skip for if"&#xa;</xsl:text-->
        <!--xsl:value-of select="$indent"/>
        <xsl:text>        return&#xa;</xsl:text-->
      </xsl:if>
      <xsl:if test="@unless">
        <xsl:value-of select="$indent"/>
        <xsl:text>    if "</xsl:text>
        <xsl:value-of select="@unless"/>
        <xsl:text>" in properties: return&#xa;</xsl:text>
        <!--xsl:text>        print "  skip for unless"&#xa;</xsl:text-->
        <!--xsl:value-of select="$indent"/>
        <xsl:text>        return&#xa;</xsl:text-->
      </xsl:if>
      <xsl:apply-templates select="*">
        <xsl:with-param name="indent" tunnel="yes" select="concat($indent, '    ')"/>
      </xsl:apply-templates>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="normalize-space($body)">
        <xsl:value-of select="$body"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$indent"/>
        <xsl:text>    pass</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="pipeline">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>attrs = {}&#xA;</xsl:text>
    
    <xsl:if test="exists(@basedir)">
     <xsl:value-of select="$indent"/>
     <xsl:text>attrs["basedir"] = </xsl:text>
     <xsl:value-of select="x:value(@basedir)"/>
     <xsl:text>&#xA;</xsl:text>
    </xsl:if>
    <xsl:if test="exists(@inputmap)">
      <xsl:value-of select="$indent"/>
      <xsl:text>attrs["inputmap"] = </xsl:text>
      <xsl:value-of select="x:value(@inputmap)"/>
      <xsl:text>&#xA;</xsl:text>
    </xsl:if>
    <xsl:value-of select="$indent"/>
    <xsl:text>attrs["tempDir"] = </xsl:text>
    <xsl:value-of select="x:value(@tempdir)"/>
    <xsl:text>&#xA;</xsl:text>
    
    <xsl:for-each select="module">
      <xsl:variable name="module-name" as="xs:string">
        <xsl:choose>
          <xsl:when test="count(../module) > 1">
            <xsl:value-of select="concat('module_', string(position()))"/>
          </xsl:when>
          <xsl:otherwise>module</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:value-of select="$indent"/>
      <xsl:text>import </xsl:text>
      <xsl:value-of select="@class"/>
      <xsl:text>&#xA;</xsl:text>
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="$module-name"/>
      <xsl:text> = ModuleFactory.instance().createModule(</xsl:text>
      <xsl:value-of select="@class"/>
      <xsl:text>)&#xA;</xsl:text>
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="$module-name"/>
      <xsl:text>.setLogger(logger)&#xA;</xsl:text>
      
      <xsl:for-each select="param">
        <xsl:if test="exists(@if | @unless)">
          <xsl:value-of select="$indent"/>
          <xsl:text>if </xsl:text>
          <xsl:value-of select="x:value(@if)"/>
          <xsl:text> in properties:&#xa;</xsl:text>
          <xsl:text>    </xsl:text>
        </xsl:if>
        <xsl:value-of select="$indent"/>
        <xsl:text>attrs[</xsl:text>
        <xsl:value-of select="x:value(@name)"/>
        <xsl:text>] = </xsl:text>
        <xsl:value-of select="x:value(@value | @location)"/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
      
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="$module-name"/>
      <xsl:text>_pipelineInput = PipelineHashIO()&#xA;</xsl:text>
      <xsl:value-of select="$indent"/>
      <xsl:text>for k, v in attrs.items():&#xA;</xsl:text>
      <xsl:value-of select="$indent"/>
      <xsl:text>    </xsl:text>
      <xsl:value-of select="$module-name"/>
      <xsl:text>_pipelineInput.setAttribute(k, v)&#xA;</xsl:text>
      
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="$module-name"/>
      <xsl:text>.execute(</xsl:text>
      <xsl:value-of select="$module-name"/>
      <xsl:text>_pipelineInput)&#xA;</xsl:text>
    </xsl:for-each>        
  </xsl:template>
  
  <xsl:template  match="xslt">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>templates = TransformerFactory.newInstance().newTemplates(StreamSource(File(</xsl:text>
    <xsl:value-of select="x:value(@style)"/>
    <xsl:text>)))&#xA;</xsl:text>
    <xsl:choose>
      <xsl:when test="@in">
        <xsl:value-of select="$indent"/>
        <xsl:text>in_file = os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@in)"/>
        <xsl:text>)&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>out_file = os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@out)"/>
        <xsl:text>)&#xA;</xsl:text>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>if not os.path.exists(os.path.dirname(out_file)):&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    os.makedirs(os.path.dirname(out_file))&#xA;</xsl:text>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>transformer = templates.newTransformer()&#xA;</xsl:text>
        <xsl:apply-templates select="param"/>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>source = StreamSource("file://" + urllib.pathname2url(in_file))&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>result = StreamResult("file://" + urllib.pathname2url(out_file))&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>print "Processing " + in_file + " to " + out_file&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>transformer.transform(source, result)&#xA;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$indent"/>
        <xsl:text>base_dir = os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@basedir)"/>
        <xsl:text>)&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>dest_dir = os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@destdir)"/>
        <xsl:text>)&#xA;</xsl:text>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>temp_ext = </xsl:text>
        <xsl:choose>
          <xsl:when test="exists(@extension)">
            <xsl:value-of select="x:value(@extension)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="x:value(substring-after(mapper/@to, '*'))"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>&#xA;</xsl:text>
        <xsl:variable name="move" select="exists(following-sibling::*[1]/self::move)"/>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>includes_file = open(os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@includesfile)"/>
        <xsl:text>), "r")&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>files = [l.strip() for l in includes_file.readlines()]&#xa;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>includes_file.close()&#xA;</xsl:text>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>for l in files:&#xa;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    transformer = templates.newTransformer()&#xA;</xsl:text>
        <xsl:apply-templates select="param">
          <xsl:with-param name="indent" select="concat($indent, '    ')" tunnel="yes"/>
        </xsl:apply-templates>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>    in_file = os.path.abspath(os.path.join(base_dir, l))&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    out_file = os.path.abspath(os.path.join(dest_dir, os.path.splitext(l)[0] + temp_ext))&#xA;</xsl:text>
        
        <xsl:if test="exists(@filenameparameter)">
          <xsl:value-of select="$indent"/>
          <xsl:text>    transformer.setParameter(</xsl:text>
          <xsl:value-of select="x:value(@filenameparameter)"/>
          <xsl:text>, os.path.split(in_file)[1])&#xA;</xsl:text>
        </xsl:if>
        <xsl:if test="exists(@filedirparameter)">
          <xsl:value-of select="$indent"/>
          <xsl:text>    transformer.setParameter(</xsl:text>
          <xsl:value-of select="x:value(@filedirparameter)"/>
          <xsl:text>, os.path.split(in_file)[0])&#xA;</xsl:text>
        </xsl:if>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>    if not os.path.exists(os.path.dirname(out_file)):&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>        os.makedirs(os.path.dirname(out_file))&#xA;</xsl:text>
        
        <xsl:value-of select="$indent"/>
        <xsl:text>    source = StreamSource("file://" + urllib.pathname2url(in_file))&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    result = StreamResult("file://" + urllib.pathname2url(out_file))&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    print "Processing " + in_file + " to " + out_file&#xA;</xsl:text>
        <xsl:value-of select="$indent"/>
        <xsl:text>    transformer.transform(source, result)&#xA;</xsl:text>
        <xsl:if test="$move">
          <xsl:value-of select="$indent"/>
          <xsl:text>for l in files:&#xa;</xsl:text>
          <xsl:value-of select="$indent"/>
          <xsl:text>    src = os.path.join(dest_dir, os.path.splitext(l)[0] + temp_ext)&#xA;</xsl:text>
          <xsl:value-of select="$indent"/>
          <xsl:text>    dst = os.path.join(base_dir, l)&#xA;</xsl:text>
          <xsl:value-of select="$indent"/>
          <xsl:text>    print "Moving " + os.path.join(dest_dir, os.path.splitext(l)[0] + temp_ext) + " to " + os.path.join(base_dir, l)&#xA;</xsl:text>
          <xsl:value-of select="$indent"/>
          <xsl:text>    shutil.move(src, dst)&#xA;</xsl:text>      
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="xslt/param">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:if test="exists(@if | @unless)">
      <xsl:value-of select="$indent"/>
      <xsl:text>if </xsl:text>
      <xsl:value-of select="x:value(@if)"/>
      <xsl:text> in properties:&#xa;</xsl:text>
      <xsl:text>    </xsl:text>
    </xsl:if>
    <xsl:value-of select="$indent"/>
    <xsl:text>transformer.setParameter(</xsl:text>
    <xsl:value-of select="x:value(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="x:value(@expression)"/>
    <xsl:text>)&#xA;</xsl:text>
  </xsl:template>
  
  <xsl:template match="xmlpropertyreader">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>read_xml_properties(</xsl:text>
    <xsl:value-of select="x:value(@file)"/>
    <xsl:text>)&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="dita-ot-echo">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>print get_msg(</xsl:text>
    <xsl:value-of select="x:value(@id)"/>
    <xsl:text>)&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="antcall">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:for-each select="@target | target/@name">
      <xsl:value-of select="$indent"/>
      <xsl:choose>
        <xsl:when test="contains(., '$')">
          <xsl:text># FIXME globals()[</xsl:text>
          <xsl:value-of select="x:value(.)"/>
          <xsl:text>]</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>self.</xsl:text>
          <xsl:value-of select="x:getMethod(.)"/>    
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>()&#xa;</xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="dita-ot-fail">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>if </xsl:text>
    <xsl:apply-templates select="condition/*"/>
    <xsl:text>:&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    print "</xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:text>"&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    sys.exit()&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="config-logger">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>#TODO config_logger()</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="config-URIResolver">
    <xsl:param name="indent" tunnel="yes"/>   
    <xsl:value-of select="$indent"/>
    <xsl:text>path = </xsl:text>
    <xsl:value-of select="x:value(@tempdir)"/>
    <xsl:text>&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>if not os.path.isabs(path):&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    path = os.path.join(</xsl:text>
    <xsl:value-of select="x:value(@basedir)"/>
    <xsl:text>, path)&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>DitaURIResolverFactory.setPath(path)&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="import">
    <xsl:message terminate="yes">import in merged file</xsl:message>
    <xsl:text># import </xsl:text>
    <xsl:value-of select="@file"/>
    <xsl:text> start&#xa;</xsl:text>
    <xsl:variable name="file"
      select="if (starts-with(@file, '${dita.plugin.org.dita.pdf2.dir}'))
              then substring-after(@file, '${dita.plugin.org.dita.pdf2.dir}/')
              else @file"/>
    <xsl:apply-templates select="document($file, .)/*"/>
    <xsl:text># import </xsl:text>
    <xsl:value-of select="@file"/>
    <xsl:text> end&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="target/*" priority="-1" use-when="false()">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text># &lt;</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="taskdef" priority="20"/>

  <!-- Ignore unneccessary code -->
  <xsl:template match="target[@name = ('help', 'all', 'init')]" priority="20"/>

</xsl:stylesheet>