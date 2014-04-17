<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita="http://dita-ot.sourceforge.net" 
                xmlns:x="x"
                exclude-result-prefixes="xs x dita"
                version="2.0">

  <xsl:import href="ant-base2scala.xsl"/>

  <xsl:output method="text"/>
  
  <xsl:strip-space elements="*"/>
  
  <xsl:param name="includes" select="()" as="xs:string?"/>
  <xsl:param name="base-class" select="()" as="xs:string?"/>
  <xsl:param name="debug" select="'false'"/>
  <xsl:param name="class"  as="xs:string"/>
  <xsl:param name="transtype" select="()" as="xs:string?"/>

  <xsl:key name="target" match="target" use="@name"/>  

  <xsl:variable name="d" select="$debug = 'true'"/>
  <xsl:variable name="properties" select="'$'"/>
  <!-- Instance variables that replace properties -->
  <xsl:variable name="boolean-instance-variables" as="xs:string*"
                select="(
                (:Preprocess:) 'noTopic', 'noConref', 'noMap', 'noConrefPush', 'noImagelist', 'noHtmllist', 'noSublist',
                               'noKeyref', 'noCoderef', 'inner.transform', 'old.transform', 'is64bit', 'is32bit',
                (:EclipseHelp:) 'noPlugin')"/>
  <xsl:variable name="string-instance-variables" as="xs:string*"
                select="('current.date')"/>
  <xsl:variable name="file-instance-variables" as="xs:string*"
                select="('output.dir', 'base.temp.dir', 'dita.temp.dir')"/>
    <xsl:variable name="file-instance-variable-names" as="xs:string*">
    <xsl:for-each select="$file-instance-variables">
      <xsl:sequence select="x:getMethod(.)"/>
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="instance-variables" as="xs:string*"
                select="($boolean-instance-variables, $string-instance-variables, $file-instance-variables)"/>  
  <xsl:variable name="string-variables" as="xs:string*"
                select="('transtype', 'dita.dir')"/>

  <xsl:variable name="ant-only-variables" as="xs:string*"
                select="('outditafilesfile', 'fullditamapandtopicfile', 'fullditatopicfile', 'fullditamapfile',
                         'hrefditatopicfile', 'conreffile', 'imagefile', 'flagimagefile', 'htmlfile', 'hreftargetsfile',
                         'canditopicsfile', 'skipchunkfile', 'subjectschemefile', 'conreftargetsfile', 'copytosourcefile',
                         'subtargetsfile', 'conrefpushfile', 'keyreffile', 'codereffile', 'resourceonlyfile')"/>

  <xsl:variable name="ignore-variables" as="xs:string*"
                select="(
                (:Preprocess:) 'noConref', 'noMap', 'noTopic', 'noImagelist', 'noHtmllist', 'noSublist', 'noConrefPush', 'noKeyref', 'noCoderef',
                               'dita.dir', 'collator', 'xslt.parser', 'xml.parser')"/>

  <xsl:variable name="ignore-targets" as="xs:string*"
                select="('help', 'all', 'init', 'start-process', 'init-logger', 'output-msg', 'output-css-warn-message', 'use-init')"/>

  <!-- merge -->
  
  <xsl:template match="node() | @*" mode="merge" priority="-1">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*" mode="merge"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="project" mode="merge">
    <xsl:copy>
      <xsl:attribute name="file" select="base-uri(.)"/>
      <xsl:apply-templates select="@*" mode="merge"/>
      <xsl:if test="exists($includes)">
        <xsl:for-each select="tokenize($includes, ',')">
          <xsl:copy-of select="document(.)/project/*[empty(self::target | self::import)]"/>
        </xsl:for-each>
      </xsl:if>
      <xsl:if test="exists($includes)">
        <xsl:for-each select="tokenize($includes, ',')">
          <xsl:copy-of select="document(.)/project/target"/>
        </xsl:for-each>
      </xsl:if>
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
  
  <!-- preprocess -->
  
  <xsl:key name="antcall" match="antcall" use="@target"/>
  
  <xsl:template match="node() | @*" mode="preprocess" priority="-1">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*" mode="preprocess"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="target" mode="preprocess">
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="preprocess"/>
      <xsl:if test="@name = concat('dita2', $transtype)">
        <xsl:attribute name="name">run</xsl:attribute>
      </xsl:if>
      <xsl:for-each-group select="key('antcall', @name, /)/param" group-by="@name">
        <antcall-parameter name="{current-grouping-key()}"/>
      </xsl:for-each-group>
      <xsl:apply-templates select="node()" mode="preprocess"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="@includesfile | @excludesfile | @includes | @excludes" mode="preprocess"/>
  <xsl:template match="*[@includesfile | @excludesfile | @includes | @excludes]" mode="preprocess">
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="preprocess"/>
      <xsl:for-each select="@includesfile | @excludesfile">
        <xsl:element name="{name()}">
          <xsl:attribute name="name" select="."/>
        </xsl:element>
      </xsl:for-each>
      <xsl:for-each select="@includes">
        <xsl:element name="include">
          <xsl:attribute name="name" select="."/>
        </xsl:element>
      </xsl:for-each>
      <xsl:for-each select="@excludes">
        <xsl:element name="exclude">
          <xsl:attribute name="name" select="."/>
        </xsl:element>
      </xsl:for-each>
      <xsl:apply-templates select="node()" mode="preprocess"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Ignore unneccessary code -->
  <xsl:template match="target[@name = $ignore-targets]" mode="preprocess" priority="20"/>
  
  <!-- Scala -->
  
  <xsl:template match="/">
    <xsl:variable name="merged" as="document-node()">
      <xsl:document>
        <xsl:apply-templates select="*" mode="merge"/>
      </xsl:document>
    </xsl:variable>
    <xsl:variable name="preprocessed" as="document-node()">
      <xsl:document>
        <xsl:apply-templates select="$merged/*" mode="preprocess"/>
      </xsl:document>
    </xsl:variable>
    
    <xsl:text>package org.dita.dost.module
      
import scala.collection.JavaConversions._

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

import javax.xml.transform.TransformerFactory
import javax.xml.transform.Transformer
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

import org.dita.dost.util.Constants._
import org.dita.dost.log.DITAOTJavaLogger
import org.dita.dost.pipeline.PipelineHashIO
import org.dita.dost.util.FileUtils
</xsl:text>
    <xsl:if test="//xmlpropertyreader">
      <xsl:text>import org.dita.dost.util.Job&#xA;</xsl:text>
    </xsl:if>
    <xsl:text>&#xA;</xsl:text>
    <xsl:apply-templates select="$preprocessed/*"/>
  </xsl:template>
  
  <xsl:template match="project">
    <xsl:variable name="project" select="."/>
    <!--xsl:variable name="depends" as="xs:string*">
      <xsl:for-each select="target/@depends/tokenize(., ',')">
        <xsl:variable name="v" select="normalize-space(.)"/>
        <xsl:if test="empty(key('target', $v, $project))">
          <xsl:sequence select="x:getClass(key('target', $v, root($project))/../@name)"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable-->
    <xsl:if test="string-length(normalize-space($transtype)) eq 0">
      <xsl:text>abstract </xsl:text>
    </xsl:if>
    <xsl:text>class </xsl:text>
    <xsl:value-of select="$class (:x:getClass(@name):)"/>
    <xsl:text>(ditaDir: File)</xsl:text>
    <xsl:if test="exists($base-class)">
      <xsl:text> extends </xsl:text>
      <xsl:value-of select="$base-class"/>
      <xsl:text>(ditaDir)</xsl:text>
    </xsl:if>
    <xsl:call-template name="x:start-block"/>
    <xsl:text>&#xa;</xsl:text>
    <xsl:if test="$d">
      <xsl:value-of select="concat('// ', @file, '&#xA;&#xA;')"/>
    </xsl:if>
    <!--xsl:value-of select="x:getClass(@name)"/>
    <xsl:text>()</xsl:text>
    <xsl:call-template name="x:start-block"/-->
    <!--
    <xsl:text>super(</xsl:text>
    <xsl:value-of select="x:getClass(@name)"/>
    <xsl:text>, self).__init__()&#xA;</xsl:text>
    -->
    <!--xsl:text>    super()&#xA;</xsl:text-->
    <xsl:value-of select="$properties"/>
    <xsl:text>("ant.file.</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>") = new File("</xsl:text>
    <xsl:value-of select="substring-after(@file, 'file:/Users/jelovirt/Work/github/muuntaja/src/main/')"/>
    <xsl:text>")&#xA;</xsl:text>
    
    <xsl:choose>
      <xsl:when test="$base-class = 'Transtype'">
        <xsl:text>&#xa;</xsl:text>
        <xsl:for-each select="$boolean-instance-variables[not(. = $ignore-variables)]">
          <xsl:text>var </xsl:text>
          <xsl:value-of select="x:getMethod(.)"/>
          <xsl:text>: Boolean = false&#xa;</xsl:text>
        </xsl:for-each>
        <!--xsl:for-each select="$string-instance-variables[not(. = $ignore-variables)]">
          <xsl:text>var </xsl:text>
          <xsl:value-of select="x:getMethod(.)"/>
          <xsl:text>: String = null&#xa;</xsl:text>
        </xsl:for-each-->
        <!--xsl:for-each select="$file-instance-variables[not(. = $ignore-variables)]">
          <xsl:text>override val </xsl:text>
          <xsl:value-of select="x:getMethod(.)"/>
          <xsl:text>: File&#xa;</xsl:text>
        </xsl:for-each-->
        
        <xsl:text>&#xa;</xsl:text>
      </xsl:when>
      <xsl:when test="normalize-space($transtype)">
        <xsl:text>override val transtype = "</xsl:text>
        <xsl:value-of select="$transtype"/>
        <xsl:text>"&#xA;</xsl:text>
      </xsl:when>
    </xsl:choose>
    
    <!--xsl:for-each select="distinct-values($depends)">
      <!- -xsl:value-of select="$indent"/- ->
      <xsl:text>val </xsl:text>
      <xsl:value-of select="x:getInstance(.)"/>
      <xsl:text> = new </xsl:text>
      <xsl:value-of select="."/>
      <xsl:text>()&#xA;</xsl:text>
    </xsl:for-each-->
    <xsl:apply-templates select="*[empty(self::target)]"/>
    <xsl:text>&#xa;</xsl:text>    
    <xsl:apply-templates select="target"/>
    <xsl:call-template name="x:end-block"/>
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
    <xsl:value-of>
      <xsl:variable name="tokens" select="tokenize($name, '[\.-]')"/>
      <xsl:value-of select="$tokens[1]"/>
      <xsl:for-each select="$tokens[position() > 1]">
        <xsl:value-of select="upper-case(substring(., 1, 1))"/>
        <xsl:value-of select="substring(., 2)"/>
      </xsl:for-each>
    </xsl:value-of>
    <!--xsl:value-of select="replace($name, '[\.-]', '_')"/-->
  </xsl:function>
  
  <xsl:template match="project/target[ends-with(@name, '-check')]" priority="1000" />
  
  <xsl:template match="project/target">
    <xsl:text>&#xa;</xsl:text>
    <xsl:if test="exists(@description)">
      <xsl:text>/** </xsl:text>
      <xsl:value-of select="normalize-space(@description)"/>
      <xsl:text> */&#xa;</xsl:text>
    </xsl:if>
    
    <xsl:if test="@name = 'run'">
      <xsl:text>override </xsl:text>
    </xsl:if>
    <xsl:text>def </xsl:text>
    <xsl:value-of select="x:getMethod(@name)"/>
    <xsl:text>(</xsl:text>
    <xsl:for-each select="antcall-parameter">
      <xsl:if test="position() ne 1">
        <xsl:text>, </xsl:text>
      </xsl:if>
      <xsl:value-of select="@name"/>
      <xsl:text>: String = </xsl:text>
      <xsl:value-of select="$properties"/>
      <xsl:text>("</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>")</xsl:text>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    
    <xsl:text>logger.info("</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>:")&#xa;</xsl:text>
    
    <!-- dependencies -->
    <xsl:if test="@depends | @dita:depends">
      <xsl:variable name="t" select="."/>
      <xsl:variable name="dependencies" as="xs:string*">
        <xsl:for-each select="tokenize(@depends | @dita:depends, ',')">
          <xsl:if test="not(normalize-space(.) = $ignore-targets)">
            <xsl:value-of select="normalize-space(.)"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:for-each select="$dependencies[not(position() eq last()) and ends-with(., '-check')]">
        <xsl:message terminate="yes">ERROR: check target not last: <xsl:value-of select="."/></xsl:message>
      </xsl:for-each>
      <xsl:variable name="active-dependencies" select="$dependencies[not(position() eq last() and ends-with(., '-check'))]"/>
      <xsl:if test="exists($active-dependencies)">
        <xsl:text>depends(</xsl:text>
        <xsl:for-each select="$active-dependencies">
          <xsl:if test="position() ne 1">, </xsl:if>
          <xsl:text>("</xsl:text>
          <xsl:value-of select="."/>
          <xsl:text>", </xsl:text>
          <!--xsl:if test="empty(key('target', $n, $t/..))">
            <xsl:value-of select="x:getInstance(x:getClass(key('target', $n, root($t))/../@name))"/>
            <xsl:text>.</xsl:text>
          </xsl:if-->
          <xsl:value-of select="x:getMethod(.)"/>
          <xsl:text>)</xsl:text>
        </xsl:for-each>
        <xsl:text>)&#xa;</xsl:text>
      </xsl:if>
      
      <!-- inline check target -->
      <xsl:variable name="root" select="/"/>
      <xsl:variable name="checks" select="$dependencies[position() eq last() and ends-with(., '-check')]"/>
      <xsl:for-each select="$checks">
        <xsl:if test="$d">
          <xsl:value-of select="concat('// start ', ., '&#xa;')"/>
        </xsl:if>
        <!--xsl:text>logger.info("</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>:")&#xa;</xsl:text-->
        <xsl:apply-templates select="$root//target[@name = current()]/*"/>
        <xsl:if test="$d">
          <xsl:value-of select="concat('// end ', ., '&#xa;')"/>
        </xsl:if>
      </xsl:for-each>
      <xsl:if test="$checks">
        <xsl:text>&#xa;</xsl:text>
      </xsl:if>
    </xsl:if>
    
    <!-- run conditions -->
    <xsl:if test="@if">
      <xsl:text>if (</xsl:text>
      <xsl:call-template name="unset">
        <xsl:with-param name="property" select="@if"/>
      </xsl:call-template>
      <xsl:text>)</xsl:text>
      <xsl:call-template name="x:start-block"/>
      <xsl:text>return</xsl:text>
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
    <xsl:if test="@unless">
      <xsl:text>if (</xsl:text>
      <xsl:call-template name="isset">
        <xsl:with-param name="property" select="@unless"/>
      </xsl:call-template>
      <xsl:text>)</xsl:text>
      <xsl:call-template name="x:start-block"/>
      <xsl:text>return</xsl:text>
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
    <xsl:if test="@if or @unless">
      <xsl:text>&#xa;</xsl:text>
    </xsl:if>
        
    <xsl:apply-templates select="*"/>
    
    <xsl:call-template name="x:end-block"/>
  </xsl:template>
  
  <xsl:template match="pipeline">
    <xsl:if test="param">
      <xsl:message terminate="yes">Module <xsl:value-of select="module/@class"/> has params</xsl:message>
    </xsl:if>
    <xsl:apply-templates select="module | xslt"/>
  </xsl:template>
  
  <xsl:template match="module">
    <xsl:variable name="module-name" as="xs:string">
      <xsl:choose>
        <xsl:when test="count(../module) > 1">
          <xsl:value-of select="concat('module_', string(position()))"/>
        </xsl:when>
        <xsl:otherwise>module</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:text>import </xsl:text>
    <xsl:value-of select="@class"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:text>val </xsl:text>
    <xsl:value-of select="$module-name"/> = new <xsl:value-of select="@class"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:value-of select="$module-name"/>
    <xsl:text>.setLogger(new DITAOTJavaLogger)&#xA;</xsl:text>
    <xsl:value-of select="$module-name"/>
    <xsl:text>.setJob(job)&#xA;</xsl:text>
    
    <xsl:variable name="pipeline-name" select="concat($module-name, 'PipelineInput')"/>
    <xsl:text>val </xsl:text>
    <xsl:value-of select="$pipeline-name"/>
    <xsl:text> = new PipelineHashIO&#xA;</xsl:text>
    <xsl:if test="exists(../@basedir)">
      <xsl:value-of select="$pipeline-name"/>
      <xsl:text>.setAttribute("basedir", </xsl:text>
      <xsl:value-of select="x:value(../@basedir)"/>
      <xsl:text>)&#xA;</xsl:text>
    </xsl:if>
    <xsl:if test="exists(../@inputmap)">
      <xsl:value-of select="$pipeline-name"/>
      <xsl:text>.setAttribute("inputmap", </xsl:text>
      <xsl:value-of select="x:value(../@inputmap)"/>
      <xsl:text>)&#xA;</xsl:text>
    </xsl:if>
    <xsl:if test="exists(../@tempdir)">
      <xsl:value-of select="$pipeline-name"/>
      <xsl:text>.setAttribute("tempDir", </xsl:text>
      <xsl:value-of select="x:value(../@tempdir)"/>
      <xsl:text>)&#xA;</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="param">
      <xsl:with-param name="pipeline-name" select="$pipeline-name"/>
    </xsl:apply-templates>
    
    <xsl:value-of select="$module-name"/>
    <xsl:text>.execute(</xsl:text>
    <xsl:value-of select="$module-name"/>
    <xsl:text>PipelineInput)&#xA;</xsl:text>
  </xsl:template>
  
  <xsl:template match="pipeline/param | module/param">
    <xsl:param name="pipeline-name"/>
    <xsl:if test="exists(@if | @unless)">
      <xsl:text>if (</xsl:text>
      <xsl:if test="@if">
        <xsl:call-template name="isset">
          <xsl:with-param name="property" select="@if"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="@if and @unless"> &amp;&amp; </xsl:if>
      <xsl:if test="@unless">
        <xsl:call-template name="unset">
          <xsl:with-param name="property" select="@un"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:text>)</xsl:text>
      <xsl:call-template name="x:start-block"/>
    </xsl:if>
    <xsl:value-of select="$pipeline-name"/>
    <xsl:text>.setAttribute(</xsl:text>
    <xsl:value-of select="x:value(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="x:value(@value | @location)"/>
    <xsl:text>)&#xA;</xsl:text>
    <xsl:if test="exists(@if | @unless)">
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="xslt[@in]">
    <xsl:if test="following-sibling::xslt">
      <xsl:text>try</xsl:text>
      <xsl:call-template name="x:start-block"/>
    </xsl:if>
    <xsl:text>val templates = compileTemplates(</xsl:text>
    <xsl:value-of select="x:file(@style)"/>
    <xsl:text>)&#xA;</xsl:text>
    <xsl:text>val inFile = </xsl:text>
    <xsl:value-of select="x:file(@in)"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:text>val outFile = </xsl:text>
    <xsl:value-of select="x:file(@out)"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:text>if (!outFile.getParentFile.exists)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:text>outFile.getParentFile.mkdirs()</xsl:text>
    <xsl:call-template name="x:end-block"/>
    <xsl:text>val transformer = templates.newTransformer()&#xA;</xsl:text>
    <xsl:apply-templates select="param | dita:extension"/>
    <xsl:text>val source = getSource(inFile)&#xA;</xsl:text>
    <xsl:text>val result = getResult(outFile)&#xA;</xsl:text>
    <xsl:text>logger.info("Processing " + inFile + " to " + outFile)&#xA;</xsl:text>
    <xsl:text>transformer.transform(source, result)&#xA;</xsl:text>
    <xsl:if test="following-sibling::xslt">
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="xslt[@includes | @includesfile | includesfile | include]">
    <xsl:if test="following-sibling::xslt">
      <xsl:text>try</xsl:text>
      <xsl:call-template name="x:start-block"/>
    </xsl:if>
    <xsl:variable name="same" select="empty(@dest) and empty(@extension)" as="xs:boolean"/>
    <xsl:text>val templates = compileTemplates(</xsl:text>
    <xsl:value-of select="x:file(@style)"/>
    <xsl:text>)&#xA;</xsl:text>
    <xsl:text>val baseDir = </xsl:text>
    <xsl:value-of select="x:file(@basedir)"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:text>val destDir = </xsl:text>
    <xsl:value-of select="x:file(if (exists(@destdir)) then @destdir else @basedir)"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:variable name="ext" as="xs:string?">
      <xsl:choose>
        <xsl:when test="exists(@extension)">
          <xsl:value-of select="x:value(@extension)"/>
        </xsl:when>
        <xsl:when test="mapper[@type = 'glob' and @from = '*' and starts-with(@to, '*.')]">
          <xsl:text>"</xsl:text>
          <xsl:value-of select="substring-after(x:value(mapper/@to), '*')"/>    
        </xsl:when>
        <!--xsl:otherwise>
          <xsl:message terminate="yes">ERROR: <xsl:value-of select="@type"/> mapper not supported</xsl:message>
        </xsl:otherwise-->          
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="exists($ext) and normalize-space($ext)">
      <xsl:text>val tempExt = </xsl:text>
      <xsl:value-of select="$ext"/>
      <xsl:text>&#xA;</xsl:text>
    </xsl:if>
    <xsl:variable name="move" select="exists(following-sibling::*[1]/self::move) or $same"/>
    <xsl:text>val files = </xsl:text>
    <xsl:value-of select="x:get-includes(.)"/>
    <!--xsl:if test="count(@includesfile | includesfile | include) gt 1">
      <xsl:text> toSet;</xsl:text>  
    </xsl:if-->
    <xsl:text>&#xA;</xsl:text>
    <xsl:if test="@reloadstylesheet">
      <xsl:text/>var transformer: Transformer = if (!<xsl:value-of select="x:value(@reloadstylesheet)"/>.toBoolean) templates.newTransformer() else null&#xA;<xsl:text/>
    </xsl:if>
    <xsl:text>for (l &lt;- files)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:choose>
      <xsl:when test="@reloadstylesheet">
        <xsl:text/>if (<xsl:value-of select="x:value(@reloadstylesheet)"/>.toBoolean) {
        transformer = templates.newTransformer()
        }&#xA;<xsl:text/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>val transformer = templates.newTransformer()&#xA;</xsl:text>        
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="param | dita:extension"/>
    <xsl:text>val inFile = new File(baseDir, l.getPath)&#xA;</xsl:text>
    <xsl:text>val outFile = </xsl:text>
    <xsl:call-template name="xslt-dest-file">
      <xsl:with-param name="ext" select="$ext"/>
      <xsl:with-param name="same" select="$same"/>
    </xsl:call-template>
    
    <xsl:if test="exists(@filenameparameter)">
      <xsl:text>transformer.setParameter(</xsl:text>
      <xsl:value-of select="x:value(@filenameparameter)"/>
      <xsl:text>, inFile.getName)&#xA;</xsl:text>
    </xsl:if>
    <xsl:if test="exists(@filedirparameter)">
      <xsl:text>transformer.setParameter(</xsl:text>
      <xsl:value-of select="x:value(@filedirparameter)"/>
      <xsl:text>, inFile.getParent)&#xA;</xsl:text>
    </xsl:if>
    <xsl:text>if (!outFile.getParentFile.exists)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:text>outFile.getParentFile.mkdirs()</xsl:text>
    <xsl:call-template name="x:end-block"/>
    <xsl:text>val source = getSource(inFile)&#xA;</xsl:text>
    <xsl:text>val result = getResult(outFile)&#xA;</xsl:text>
    <xsl:text>logger.info("Processing " + inFile + " to " + outFile)&#xA;</xsl:text>
    <xsl:text>transformer.transform(source, result)</xsl:text>
    <xsl:call-template name="x:end-block"/>
    <xsl:if test="$move">
      <xsl:text>for (l &lt;- files)</xsl:text>
      <xsl:call-template name="x:start-block"/>
      <xsl:text>val src = </xsl:text>
      <xsl:call-template name="xslt-dest-file">
        <xsl:with-param name="ext" select="$ext"/>
        <xsl:with-param name="same" select="$same"/>
      </xsl:call-template>
      <xsl:text>val dst = new File(baseDir, l.getPath)&#xA;</xsl:text>
      <!--xsl:text>logger.info("Moving " + src + " to " + dst)&#xA;</xsl:text-->
      <xsl:text>FileUtils.moveFile(src, dst)</xsl:text>
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
    <xsl:if test="following-sibling::xslt">
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="xslt-dest-file">
    <xsl:param name="ext" as="xs:string?"/>
    <xsl:param name="same" as="xs:boolean"/>
    <xsl:choose>
      <xsl:when test="mapper and not(normalize-space($ext))">
        <xsl:text>new File(globMap(new File(destDir, l.getPath).getAbsolutePath, </xsl:text>
        <xsl:value-of select="x:value(mapper/@from)"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="x:value(mapper/@to)"/>
        <xsl:text>))&#xA;</xsl:text>
      </xsl:when>
      <xsl:when test="$same">
        <xsl:text>new File(destDir, l.getPath + ".tmp")&#xA;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>new File(destDir, FileUtils.replaceExtension(l.getPath, tempExt))&#xA;</xsl:text>
      </xsl:otherwise>
    </xsl:choose> 
  </xsl:template>
  
  <xsl:template match="xslt/param">
    <xsl:if test="exists(@if | @unless)">
      <xsl:text>if (</xsl:text>
      <xsl:call-template name="isset">
        <xsl:with-param name="property" select="@if"/>
      </xsl:call-template>
      <xsl:text>)</xsl:text>
      <xsl:call-template name="x:start-block"/>
    </xsl:if>
    <xsl:text>transformer.setParameter(</xsl:text>
    <xsl:value-of select="x:value(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="x:value(@expression)"/>
    <xsl:text>)&#xA;</xsl:text>
    <xsl:if test="exists(@if | @unless)">
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="xslt/dita:extension[@behavior = 'org.dita.dost.platform.InsertAction']">
    <xsl:text>getExtensionParameters("</xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:text>").foreach((key: String, value: String) => transformer.setParameter(key, value))&#xA;</xsl:text>
  </xsl:template>
  
  <xsl:template match="dita:extension[@id = ('dita.conductor.target', 'dita.conductor.target.relative')]" priority="10"/>
  
  <xsl:template match="dita:extension" priority="-1">
    <xsl:text>// dita:extension </xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>
  
  <xsl:template match="xmlpropertyreader">
    <xsl:text>&#xa;</xsl:text>
    <xsl:text>job = new Job(new File(</xsl:text>
    <xsl:value-of select="$properties"/>
    <xsl:text>("dita.temp.dir")))&#xa;</xsl:text>
    <xsl:value-of select="$properties"/>
    <xsl:text>.readXmlProperties(</xsl:text>
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>)&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="dita-ot-echo">
    <xsl:text>logger.info(get_msg(</xsl:text>
    <xsl:value-of select="x:value(@id)"/>
    <xsl:text>))&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="antcall">
    <xsl:for-each select="@target | target/@name">
      <xsl:choose>
        <xsl:when test="contains(., '$')">
          <xsl:text>// FIXME globals()[</xsl:text>
          <xsl:value-of select="x:value(.)"/>
          <xsl:text>]</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="x:getMethod(.)"/>    
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>(</xsl:text>
      <xsl:for-each select="../param">
        <xsl:if test="position() > 1">, </xsl:if>
        <xsl:value-of select="@name"/>
        <xsl:text> = </xsl:text>
        <xsl:value-of select="x:value(@value | @location)"/>
      </xsl:for-each>
      <xsl:text>)&#xa;</xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="xmlcatalog[@id]">
    <!--
    <xsl:text>import org.apache.xml.resolver.CatalogManager&#xA;</xsl:text>
    <xsl:text>val catalogManager = new CatalogManager(</xsl:text>
    <xsl:value-of select="x:value(catalogpath/@path)"/>
    <xsl:text>)&#xA;</xsl:text>
    -->
  </xsl:template>
  
  <xsl:template match="dita-ot-fail">
    <xsl:text>if (</xsl:text>
    <xsl:apply-templates select="condition/*"/>
    <xsl:text>)</xsl:text>
    <xsl:call-template name="x:start-block"></xsl:call-template>
    <xsl:text>logger.error("</xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:text>")&#xa;</xsl:text>
    <xsl:text>throw new IllegalArgumentException</xsl:text>
    <xsl:call-template name="x:end-block"/>
  </xsl:template>
  
  <xsl:template match="config-logger">
    <xsl:text>//TODO config_logger()</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="config-URIResolver">   
    <xsl:text>var path = </xsl:text>
    <xsl:value-of select="x:file(@tempdir)"/>
    <xsl:text>&#xa;</xsl:text>
    <xsl:if test="@basedir">
      <xsl:text>if (!path.isAbsolute)</xsl:text>
      <xsl:call-template name="x:start-block"/>
      <xsl:text>path = new File(</xsl:text>
      <xsl:value-of select="x:value(@basedir)"/>
      <xsl:text>, path.getPath)&#xa;</xsl:text>
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
    <xsl:text>DitaURIResolverFactory.setPath(path.getAbsolutePath)</xsl:text>
  </xsl:template>

  <xsl:template match="import">
    <xsl:message terminate="yes">import in merged file</xsl:message>
    <xsl:text>// import </xsl:text>
    <xsl:value-of select="@file"/>
    <xsl:text> start&#xa;</xsl:text>
    <xsl:variable name="file"
      select="if (starts-with(@file, '${dita.plugin.org.dita.pdf2.dir}'))
              then substring-after(@file, '${dita.plugin.org.dita.pdf2.dir}/')
              else @file"/>
    <xsl:apply-templates select="document($file, .)/*"/>
    <xsl:text>// import </xsl:text>
    <xsl:value-of select="@file"/>
    <xsl:text> end&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="target/*" priority="-1" use-when="false()">
    <xsl:text>// &lt;</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>>&#xa;</xsl:text>
  </xsl:template>  

  <xsl:template match="macrodef[@name = 'job-helper']"/>
  <xsl:template match="job-helper"/>

  <xsl:template match="condition[@property = $ignore-variables] |
                       dirname[@property = 'ant.file.DOST.dir']"/>

  <xsl:template match="taskdef" priority="20"/>

  <xsl:template match="antcall-parameter"/>

  <xsl:template match="target[@name = $ignore-targets]" priority="20"/>

  <xsl:template match="target[@name = 'check-arg']/echo |
                       target[@name = 'check-arg']/echoproperties" priority="1000"/>

  <xsl:template match="property[@name = $ant-only-variables]"/>

</xsl:stylesheet>