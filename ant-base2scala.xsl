<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:x="x" exclude-result-prefixes="xs x"
  version="2.0">

  <xsl:template
    match="move[preceding-sibling::*[1]/self::xslt[@includes | @includesfile | includesfile | include]]"
    priority="1000"/>

  <xsl:template match="copy | move">
    <xsl:for-each select="fileset">
      <xsl:value-of select="name(..)"/>
      <xsl:text>(</xsl:text>
      <xsl:value-of select="x:file(@dir)"/>
      <xsl:text>, </xsl:text>
      <xsl:value-of select="x:file(../@todir)"/>
      <xsl:text>, </xsl:text>
      <xsl:if test="empty(@includes | @includesfile | includesfile | include)">
        <xsl:text>listAll(</xsl:text>
        <xsl:value-of select="x:file(@dir)"/>
        <xsl:text>)</xsl:text>
      </xsl:if>
      <xsl:value-of select="x:get-includes(.)"/>
      <!--xsl:if test="exclude">
        <xsl:text>, ("</xsl:text>
        <xsl:value-of select="include/@name" separator="&quot;, &quot;"></xsl:value-of>
        <xsl:text>")</xsl:text>
      </xsl:if-->
      <xsl:text>)&#xA;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="zip">
    <!--zip destfile="${odt.output.tempdir}/${dita.map.filename.root}${odt.suffix}"
      basedir="${odt.output.tempdir}"
      excludes="**/*.list, **/*.log, **/*.temp, **/*.properties, **/*.odt"/-->
    <xsl:text>zip(</xsl:text>
    <xsl:value-of select="x:file((@destfile | @zipfile)[1])"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="x:file(@basedir)"/>
    <xsl:text>, </xsl:text>
    <xsl:if test="empty(@includes | @includesfile | includesfile | include)">
      <xsl:text>listAll(</xsl:text>
      <xsl:value-of select="x:file(@basedir)"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:value-of select="x:get-includes(.)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="dita-ot-copy">
    <xsl:text>ditaOtCopy(</xsl:text>
    <xsl:value-of select="x:file(@todir)"/>
    <xsl:text>, </xsl:text>
    <!--xsl:apply-templates select="@includes"/-->
    <xsl:value-of select="x:file(includesfile/@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:apply-templates select="@relativepaths"/>
    <xsl:text>)&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match="include | exclude">
    <xsl:text>Set("</xsl:text>
    <xsl:value-of select="@name" separator="&quot;, &quot;"/>
    <xsl:text>")</xsl:text>
  </xsl:template>

  <xsl:template match="@includes | @excludes">
    <xsl:message terminate="yes"/>
  </xsl:template>

  <xsl:template match="dita-ot-copy/@relativepaths">
    <!--xsl:choose>
      <xsl:when test="matches(., '^\$\{(.+?)list\}$')">
        <xsl:variable name="id" select="replace(., '^\$\{(.+?)list\}$', '$1')"/>
        <!- -xsl:text>job.getSet("</xsl:text>
        <xsl:value-of select="concat($id, 'list')"/>
        <xsl:text>")</xsl:text- ->
        <xsl:text>job.getFileInfo().filter(</xsl:text>
        <xsl:value-of select="x:getFileInfoFilter($id)"/>
        <xsl:text>).map(_.file).toSet</xsl:text>
      </xsl:when>
      <xsl:otherwise-->
        <!--xsl:text>Set(</xsl:text-->
        <xsl:variable name="node" select="."/>
        <xsl:for-each select="tokenize(., ',')">
          <xsl:if test="position() ne 1">, </xsl:if>
          <xsl:value-of select="x:value(., $node)"/>
          <xsl:text>.split(',')</xsl:text>
        </xsl:for-each>
        <!--xsl:text>)</xsl:text-->
      <!--/xsl:otherwise>
    </xsl:choose-->
  </xsl:template>

  <xsl:function name="x:getFileInfoFilter">
    <xsl:param name="id"/>
    <xsl:choose>
      <xsl:when test="$id = 'fullditatopic'">_.format == "dita"</xsl:when>
      <xsl:when test="$id = 'image'">_.format == "image"</xsl:when>
      <xsl:when test="$id = 'html'">_.format == "html"</xsl:when>
      <xsl:when test="$id = 'fullditamap'">_.format == "ditamap"</xsl:when>
      <xsl:when test="$id = 'resourceonly'">_.isResourceOnly</xsl:when>
      <xsl:when test="$id = 'subtargets'">_.isSubtarget</xsl:when>
      <xsl:when test="$id = 'outditafiles'">_.isOutDita</xsl:when>
      <xsl:otherwise>
        <xsl:text>_.</xsl:text>
        <xsl:value-of select="$id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:template match="@includesfile | @excludesfile">
    <xsl:message terminate="yes"/>
    <xsl:choose>
      <xsl:when test="matches(., '^\$\{dita\.temp\.dir\}(/|\$\{file.separator\})\$\{(.+?)file\}$')">
        <xsl:variable name="id"
          select="replace(., '^\$\{dita\.temp\.dir\}(/|\$\{file.separator\})\$\{(.+?)file\}$', '$2')"/>
        <xsl:choose>
          <xsl:when test="$id = 'user.input.file.list'">
            <xsl:text>Set(new File(job.getInputMap))</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <!--xsl:text>job.getSet("</xsl:text>
            <xsl:value-of select="concat($id, 'list')"/>
            <xsl:text>")</xsl:text-->
            <xsl:text>job.getFileInfo().filter(</xsl:text>
            <xsl:value-of select="x:getFileInfoFilter($id)"/>
            <xsl:text>).map(_.file).toSet</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>readLines(</xsl:text>
        <xsl:value-of select="x:file(.)"/>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="includesfile | excludesfile">
    <xsl:choose>
      <xsl:when
        test="matches(@name, '^\$\{dita\.temp\.dir\}(/|\$\{file.separator\})\$\{(.+?)file\}$')">
        <xsl:variable name="id"
          select="replace(@name, '^\$\{dita\.temp\.dir\}(/|\$\{file.separator\})\$\{(.+?)file\}$', '$2')"/>
        <xsl:choose>
          <xsl:when test="$id = 'user.input.file.list'">
            <xsl:text>Set(new File(job.getInputMap))</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <!--xsl:text>job.getSet("</xsl:text>
            <xsl:value-of select="concat($id, 'list')"/>
            <xsl:text>")</xsl:text-->
            <xsl:text>job.getFileInfo().filter(</xsl:text>
            <xsl:value-of select="x:getFileInfoFilter($id)"/>
            <xsl:text>)</xsl:text>
            <xsl:choose>
              <xsl:when test="ancestor::copy">
                <xsl:text>.map(_.file.getPath)</xsl:text>    
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>.map(_.file)</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
            
            <xsl:text>.toSet</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>readLines(</xsl:text>
        <xsl:value-of select="x:file(@name)"/>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- combine @includes, @includesfile, and includesfile -->
  <xsl:function name="x:get-includes">
    <xsl:param name="current-node" as="element()"/>
    <!-- TODO: Should be a set -->
    <xsl:for-each
      select="$current-node/@includes | $current-node/@includesfile | $current-node/includesfile | $current-node/include">
      <xsl:if test="position() ne 1"> ++ </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:for-each
      select="$current-node/@excludes | $current-node/@excludesfile | $current-node/excludesfile | $current-node/exclude">
      <xsl:text> -- </xsl:text>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:function>


  <xsl:template match="delete">
    <xsl:text>delete(</xsl:text>
    <xsl:choose>
      <xsl:when test="@file">
        <xsl:value-of select="x:file(@file)"/>
      </xsl:when>
      <xsl:when test="fileset">
        <xsl:for-each select="fileset">
          <xsl:value-of select="x:file(@dir)"/>
          <xsl:text>, </xsl:text>
          <xsl:choose>
            <xsl:when test="@includes | @includesfile | includesfile | include">
              <xsl:value-of select="x:get-includes(.)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>listAll(</xsl:text>
              <xsl:value-of select="x:file(@dir)"/>
              <xsl:text>)</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="x:file(@dir)"/>
        <xsl:text>, </xsl:text>
        <xsl:choose>
          <xsl:when test="@includes | @includesfile | includesfile | include">
            <xsl:value-of select="x:get-includes(.)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>listAll(</xsl:text>
            <xsl:value-of select="x:file(@dir)"/>
            <xsl:text>)</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>)&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match="echo">
    <xsl:text>logger.log</xsl:text>
    <xsl:choose>
      <xsl:when test="@level = 'error'">Error</xsl:when>
      <xsl:when test="@level = 'warning'">Warn</xsl:when>
      <xsl:when test="@level = 'info' or empty(@level)">Info</xsl:when>
      <xsl:when test="@level = ('verbose', 'debug')">Debug</xsl:when>
    </xsl:choose>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="x:value(.)"/>
    <xsl:text>)&#xa;</xsl:text>
  </xsl:template>

  <!-- Ignore for now -->
  <xsl:template match="echoproperties"/>

  <xsl:template match="tstamp">
    <xsl:for-each select="format">
      <xsl:value-of select="x:set-property(@property)"/>
      <xsl:text> = </xsl:text>
      <!--xsl:value-of select="$properties"/>
      <xsl:text>(</xsl:text>
      <xsl:value-of select="@property"/>
      <xsl:text>) = </xsl:text-->
      <xsl:text>"20120130"</xsl:text>
      <xsl:text>&#xa;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mkdir">
    <xsl:text>if (!</xsl:text>
    <xsl:value-of select="x:file(@dir)"/>
    <xsl:text>.exists())</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:text/>
    <xsl:value-of select="x:file(@dir)"/>
    <xsl:text>.mkdirs()</xsl:text>
    <xsl:call-template name="x:end-block"/>
  </xsl:template>

  <xsl:template match="property">
    <xsl:value-of select="x:set-property(@name)"/>
    <xsl:text> = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:when test="exists(@location)">
        <xsl:value-of select="x:file(@location)"/>
      </xsl:when>
    </xsl:choose>
    <xsl:text/>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="property[@file]">
    <xsl:value-of select="$properties"/>
    <xsl:text>.readProperties(</xsl:text>
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>)</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="property[@environment]" priority="10"/>

  <xsl:template match="makeurl">
    <xsl:value-of select="x:set-property(@property)"/>
    <xsl:text> = </xsl:text>
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>.toURI().toASCIIString()</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="basename">
    <xsl:value-of select="x:set-property(@property)"/>
    <xsl:text> = </xsl:text>
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>.getName()</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="dirname">
    <xsl:value-of select="$properties"/>
    <xsl:text>("</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:text>") = </xsl:text>
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>.getParent()</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>


  <xsl:template match="condition">
    <xsl:text>if (</xsl:text>
    <xsl:apply-templates select="*"/>
    <xsl:text>)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:value-of select="x:set-property(concat(@property, @name))"/>
    <xsl:text> = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:when test="@property = $instance-variables">true</xsl:when>
      <xsl:otherwise>"true"</xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="x:end-block"/>
    <xsl:if test="@else">
      <xsl:text>else</xsl:text>
      <xsl:call-template name="x:start-block"/>
      <xsl:value-of select="x:set-property(@property)"/>
      <xsl:text> = </xsl:text>
      <xsl:value-of select="x:value(@else)"/>
      <xsl:call-template name="x:end-block"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="target/available" priority="10">
    <xsl:text>if (</xsl:text>
    <xsl:choose>
      <xsl:when test="@file">
        <xsl:value-of select="x:file(@file)"/>
        <xsl:text>.exists()</xsl:text>
        <xsl:if test="@type">
          <xsl:text> &amp;&amp; </xsl:text>
          <xsl:value-of select="x:file(@file)"/>
          <xsl:choose>
            <xsl:when test="@type = 'dir'">
              <xsl:text>.isDirectory()</xsl:text>
            </xsl:when>
            <xsl:when test="@type = 'file'">
              <xsl:text>.isFile()</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message terminate="yes">ERROR: available type <xsl:value-of select="@type"/> not
                supported</xsl:message>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </xsl:when>
      <xsl:when test="@classname">
        <xsl:text>class_available("</xsl:text>
        <xsl:value-of select="x:value(@classname)"/>
        <xsl:text>")</xsl:text>
      </xsl:when>
    </xsl:choose>
    <xsl:text>)</xsl:text>
    <xsl:call-template name="x:start-block"/>
    <xsl:value-of select="$properties"/>
    <xsl:text>("</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:value-of select="@name"/>
    <xsl:text>") = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:when test="@property = $instance-variables">true</xsl:when>
      <xsl:otherwise>"true"</xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="x:end-block"/>
  </xsl:template>

  <xsl:template match="or">
    <xsl:text>(</xsl:text>
    <xsl:for-each select="*">
      <xsl:if test="position() ne 1"> || </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="and">
    <xsl:text>(</xsl:text>
    <xsl:for-each select="*">
      <xsl:if test="position() ne 1"> &amp;&amp; </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="not">
    <xsl:if test="empty(isset | equals)">
      <xsl:text>!</xsl:text>
      <xsl:text>(</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="*"/>
    <xsl:if test="empty(isset | equals)">
      <xsl:text>)</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="equals">
    <xsl:choose>
      <xsl:when test="matches(@arg1, '^\$\{.+?list\}$') and @arg2 = ''">
        <!--xsl:text>job.getSet("</xsl:text>
        <xsl:value-of select="replace(@arg1, '^\$\{(.+?list)\}$', '$1')"/>
        <xsl:text>").isEmpty()</xsl:text-->
        <xsl:text>job.getFileInfo().filter(</xsl:text>
        <xsl:value-of select="x:getFileInfoFilter(replace(@arg1, '^\$\{(.+?list)\}$', '$1'))"/>
        <xsl:text>).isEmpty()</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="x:value(@arg1)"/>
        <xsl:choose>
          <xsl:when test="parent::not">!=</xsl:when>
          <xsl:otherwise>==</xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="x:value(@arg2)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="istrue">
    <xsl:variable name="n" select="x:value(@value)"/>
    <xsl:choose>
      <xsl:when test="$n = $instance-variables">
        <xsl:value-of select="$n"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$n"/>
        <xsl:text> == "true"</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="isfalse">
    <xsl:variable name="n" select="x:value(@value)"/>
    <xsl:choose>
      <xsl:when test="$n = $instance-variables">
        <xsl:text>!</xsl:text>
        <xsl:value-of select="$n"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$n"/>
        <xsl:text> != "true"</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="isabsolute">
    <xsl:value-of select="x:file(@path)"/>
    <xsl:text>.isAbsolute</xsl:text>
  </xsl:template>

  <xsl:template match="os">
    <xsl:text>System.getProperty("os.name") == </xsl:text>
    <xsl:value-of select="x:value(@arch | @family)"/>
  </xsl:template>

  <xsl:template match="contains">
    <xsl:value-of select="x:value(@string)"/>
    <xsl:text>.indexOf(</xsl:text>
    <xsl:value-of select="x:value(@substring)"/>
    <xsl:text>) != -1</xsl:text>
  </xsl:template>

  <xsl:template match="isset" name="isset">
    <xsl:param name="property" select="@property"/>
    <xsl:choose>
      <xsl:when test="$property = 'noConref'">
        <xsl:text>job.getFileInfo().find(_.hasConref).isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noMap'">
        <xsl:text>job.getFileInfo().find(_.format == "ditamap").isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noTopic'">
        <xsl:text>job.getFileInfo().find(_.format == "dita").isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noImagelist'">
        <xsl:text>job.getFileInfo().find(_.format == "image").isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noHtmllist'">
        <xsl:text>job.getFileInfo().find(_.format == "html").isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noSublist'">
        <xsl:text>job.getFileInfo().find(_.format == "data").isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noConrefPush'">
        <xsl:text>job.getFileInfo().find(_.isConrefPush).isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noKeyref'">
        <xsl:text>job.getFileInfo().find(_.hasKeyref).isEmpty</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noCoderef'">
        <xsl:text>job.getFileInfo().find(_.hasCoderef).isEmpty</xsl:text>
      </xsl:when>

      <xsl:when test="$property = ($instance-variables, $string-variables)">
        <xsl:value-of select="x:getMethod($property)"/>
        <!--xsl:text> != null</xsl:text-->
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$properties"/>
        <xsl:text>.contains("</xsl:text>
        <xsl:value-of select="$property"/>
        <xsl:text>")</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="not/isset" name="unset">
    <xsl:param name="property" select="@property"/>
    <xsl:choose>
      <xsl:when test="$property = 'noConref'">
        <xsl:text>job.getFileInfo().find(_.hasConref).isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noMap'">
        <xsl:text>job.getFileInfo().find(_.format == "ditamap").isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noTopic'">
        <xsl:text>job.getFileInfo().find(_.format == "dita").isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noImagelist'">
        <xsl:text>job.getFileInfo().find(_.format == "image").isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noHtmllist'">
        <xsl:text>job.getFileInfo().find(_.format == "html").isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noSublist'">
        <xsl:text>job.getFileInfo().find(_.format == "data").isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noConrefPush'">
        <xsl:text>job.getFileInfo().find(_.isConrefPush).isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noKeyref'">
        <xsl:text>job.getFileInfo().find(_.hasKeyref).isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = 'noCoderef'">
        <xsl:text>job.getFileInfo().find(_.hasCoderef).isDefined</xsl:text>
      </xsl:when>
      <xsl:when test="$property = ($instance-variables, $string-variables)">
        <xsl:text>!</xsl:text>
        <xsl:value-of select="x:getMethod($property)"/>
        <!--xsl:text> == null</xsl:text-->
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>!</xsl:text>
        <xsl:value-of select="$properties"/>
        <xsl:text>.contains("</xsl:text>
        <xsl:value-of select="$property"/>
        <xsl:text>")</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template
    match="condition//length[@file = '${dita.temp.dir}/${conreffile}' and @length = '0']">
    <xsl:text>job.getFileInfo().exists(_.hasConref)</xsl:text>
  </xsl:template>
  <xsl:template match="condition//length[@file = '${dita.temp.dir}/${htmlfile}' and @length = '0']">
    <xsl:text>job.getFileInfo().exists(_.format == "html")</xsl:text>
  </xsl:template>
  <xsl:template match="condition//length[@file = '${dita.temp.dir}/${subtargetsfile}' and @length = '0']">
    <xsl:text>job.getFileInfo().exists(_.format == "data")</xsl:text>
  </xsl:template>
  <xsl:template match="condition//length[@file = '${dita.temp.dir}/${fullditamapfile}' and @length = '0']">
    <xsl:text>job.getFileInfo().exists(_.format == "ditamap")</xsl:text>
  </xsl:template>
  <xsl:template match="condition//length[@file = '${dita.temp.dir}/${fullditatopicfile}' and @length = '0']">
    <xsl:text>job.getFileInfo().exists(_.format == "dita")</xsl:text>
  </xsl:template>

  <xsl:template match="condition//available[@file]">
    <xsl:value-of select="x:file(@file)"/>
    <xsl:text>.exists()</xsl:text>
  </xsl:template>

  <xsl:template match="available[@classname]">
    <xsl:text>class_available(</xsl:text>
    <xsl:value-of select="x:value(@classname)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:function name="x:file" as="xs:string">
    <xsl:param name="value" as="node()"/>
    <xsl:value-of select="x:file(string($value), $value)"/>
  </xsl:function>

  <xsl:function name="x:file" as="xs:string">
    <xsl:param name="value" as="xs:string"/>
    <xsl:param name="node" as="node()"/>
    <xsl:variable name="antcall-parameters"
      select="$node/ancestor-or-self::target[1]/antcall-parameter"/>
    <xsl:variable name="v">
      <xsl:choose>
        <xsl:when test="string-length($value) = 0">""</xsl:when>
        <xsl:otherwise>
          <xsl:variable name="v" as="xs:string*">
            <xsl:analyze-string select="$value" regex="(\$\{{(.+?)\}}|(/))">
              <xsl:matching-substring>
                <xsl:choose>
                  <xsl:when test="regex-group(2) = 'file.separator' or regex-group(3) = '/'">
                    <xsl:text>File.separator</xsl:text>
                  </xsl:when>
                  <xsl:when test="exists($antcall-parameters[@name = regex-group(2)])">
                    <xsl:value-of select="regex-group(2)"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="x:get-property(regex-group(2))"/>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:matching-substring>
              <xsl:non-matching-substring>
                <xsl:if test="string-length(.) gt 0">
                  <xsl:value-of
                    select="concat('&quot;', replace(replace(., '&#xA;', '\\n'), '&quot;', '\\&quot;'), '&quot;')"
                  />
                </xsl:if>
              </xsl:non-matching-substring>
            </xsl:analyze-string>
          </xsl:variable>
          <xsl:for-each select="$v">
            <xsl:if test="position() ne 1"> + </xsl:if>
            <xsl:value-of select="."/>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="res" select="replace($v, '\\', '\\\\')"/>
    <xsl:choose>
      <xsl:when test="$res = 'ditaDir'">
        <xsl:value-of select="$res"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('new File(', $res, ')')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="x:value" as="xs:string">
    <xsl:param name="value" as="node()"/>
    <xsl:value-of select="x:value(string($value), $value)"/>
  </xsl:function>

  <xsl:function name="x:value" as="xs:string">
    <xsl:param name="value" as="xs:string"/>
    <xsl:param name="node" as="node()"/>
    <xsl:variable name="antcall-parameters"
      select="$node/ancestor-or-self::target[1]/antcall-parameter"/>
    <xsl:variable name="v">
      <xsl:choose>
        <xsl:when test="string-length($value) = 0">""</xsl:when>
        <xsl:otherwise>
          <xsl:variable name="v" as="xs:string*">
            <xsl:analyze-string select="$value" regex="\$\{{(.+?)\}}">
              <xsl:matching-substring>
                <xsl:choose>
                  <xsl:when test="exists($antcall-parameters[@name = regex-group(1)])">
                    <xsl:value-of select="regex-group(1)"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="x:get-property(regex-group(1))"/>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:matching-substring>
              <xsl:non-matching-substring>
                <xsl:if test="string-length(.) gt 0">
                  <xsl:value-of
                    select="concat('&quot;', replace(replace(., '&#xA;', '\\n'), '&quot;', '\\&quot;'), '&quot;')"
                  />
                </xsl:if>
              </xsl:non-matching-substring>
            </xsl:analyze-string>
          </xsl:variable>
          <xsl:for-each select="$v">
            <xsl:if test="position() ne 1"> + </xsl:if>
            <xsl:value-of select="."/>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:value-of select="replace($v, '\\', '\\\\')"/>
  </xsl:function>

  <xsl:function name="x:get-property" as="xs:string">
    <xsl:param name="name" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="$name = 'user.input.dir'">
        <xsl:text>job.getInputMap()</xsl:text>
      </xsl:when>
      <xsl:when test="$name = 'user.input.file'">
        <xsl:text>job.getInputMap()</xsl:text>
      </xsl:when>
      <xsl:when test="$name = ($instance-variables, $string-variables)">
        <xsl:value-of select="x:getMethod($name)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat($properties, '(&quot;', $name, '&quot;)')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="x:set-property">
    <xsl:param name="name" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="$name = $instance-variables">
        <xsl:value-of select="x:getMethod($name)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$properties"/>
        <xsl:text>("</xsl:text>
        <xsl:value-of select="$name"/>
        <xsl:text>")</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:template match="typedef"/>

  <xsl:template match="path[@id = ('dost.jar.path', 'dost.class.path')]"/>

  <!-- XXX: Ignore for now, implement later when needed -->
  <xsl:template match="defaultexcludes"/>

  <xsl:template match="*" priority="-2">
    <xsl:message>No mapping for <xsl:for-each select="(ancestor-or-self::*)">/<xsl:value-of
          select="name()"/></xsl:for-each></xsl:message>
    <xsl:apply-templates select="*"/>
  </xsl:template>

  <xsl:template name="x:start-block">
    <xsl:text> {&#xa;</xsl:text>
  </xsl:template>

  <xsl:template name="x:end-block">
    <!--xsl:text>&#xa;</xsl:text-->
    <xsl:text>}&#xa;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
