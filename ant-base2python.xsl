<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:x="x"
                exclude-result-prefixes="xs x"
                version="2.0">

  <xsl:template match="copy">
    <xsl:param name="indent" tunnel="yes"/>
    
    <xsl:for-each select="fileset">
      <xsl:value-of select="$indent"/>
      <xsl:choose>
        <xsl:when test="@includesfile">copy_list</xsl:when>
        <xsl:otherwise>copy</xsl:otherwise>
      </xsl:choose>
      <xsl:text>(</xsl:text>
      <xsl:value-of select="x:value(@dir)"/>
      <xsl:text>, </xsl:text>
      <xsl:value-of select="x:value(../@todir)"/>
      <xsl:text>, </xsl:text>
      <xsl:value-of select="x:value(@includes | @includesfile)"/>
      <xsl:text>)&#xA;</xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="echo">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>print </xsl:text>
    <xsl:value-of select="x:value(.)"/>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="tstamp">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:for-each select="format">
      <xsl:value-of select="$indent"/>
      <xsl:text>properties[</xsl:text>
      <xsl:value-of select="x:value(@property)"/>
      <xsl:text>] = </xsl:text>
      <xsl:text>"20120130"</xsl:text>
      <xsl:text>&#xa;</xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  
  <xsl:template match="mkdir">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>if not os.path.exists(</xsl:text>
    <xsl:value-of select="x:value(@dir)"/>
    <xsl:text>):&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    os.makedirs(</xsl:text>
    <xsl:value-of select="x:value(@dir)"/>
    <xsl:text>)&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="property">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>properties["</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>"] = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:when test="exists(@location)">
        <xsl:text>os.path.abspath(</xsl:text>
        <xsl:value-of select="x:value(@location)"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
    </xsl:choose>
    <xsl:text></xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="property[@file]">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>read_properties(</xsl:text>
    <xsl:value-of select="x:value(@file)"/>
    <xsl:text>)</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="property[@environment]" priority="10"/>
  
  <xsl:template match="makeurl">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>properties["</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:text>"] = urllib.pathname2url(</xsl:text>
    <xsl:value-of select="x:value(@file)"/>
    <xsl:text>)</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  <xsl:template match="basename | dirname">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>properties["</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:text>"] = </xsl:text>
    <xsl:text>os.path.</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="x:value(@file)"/>
    <xsl:text>)</xsl:text>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  
  <xsl:template match="condition">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>if </xsl:text>
    <xsl:apply-templates select="*"/>
    <xsl:text>:&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    properties["</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:value-of select="@name"/>
    <xsl:text>"] = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:otherwise>"true"</xsl:otherwise>
    </xsl:choose>
    <xsl:text>&#xa;</xsl:text>
    <xsl:if test="@else">
      <xsl:value-of select="$indent"/>
      <xsl:text>else:&#xa;</xsl:text>
      <xsl:value-of select="$indent"/>
      <xsl:text>    properties["</xsl:text>
      <xsl:value-of select="@property"/>
      <xsl:text>"] = </xsl:text>
      <xsl:value-of select="x:value(@else)"/>
      <xsl:text>&#xa;</xsl:text>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="target/available" priority="10">
    <xsl:param name="indent" tunnel="yes"/>
    <xsl:value-of select="$indent"/>
    <xsl:text>if </xsl:text>
    <xsl:choose>
      <xsl:when test="@file">
        <xsl:text>os.path.exists(</xsl:text>
        <xsl:value-of select="x:value(@file)"/>
        <xsl:text>)</xsl:text>        
      </xsl:when>
      <xsl:when test="@classname">
        <xsl:text>class_available(</xsl:text>
        <xsl:value-of select="x:value(@classname)"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
    </xsl:choose>
    <xsl:text>:&#xa;</xsl:text>
    <xsl:value-of select="$indent"/>
    <xsl:text>    properties["</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:value-of select="@name"/>
    <xsl:text>"] = </xsl:text>
    <xsl:choose>
      <xsl:when test="exists(@value)">
        <xsl:value-of select="x:value(@value)"/>
      </xsl:when>
      <xsl:otherwise>"true"</xsl:otherwise>
    </xsl:choose>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>
  
  
  <xsl:template match="or">
    <xsl:text>(</xsl:text>
    <xsl:for-each select="*">
      <xsl:if test="position() ne 1"> or </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
  </xsl:template>
  
  <xsl:template match="and">
    <xsl:text>(</xsl:text>
    <xsl:for-each select="*">
      <xsl:if test="position() ne 1"> and </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
  </xsl:template>
  
  <xsl:template match="not">
    <xsl:if test="empty(isset)">
      <xsl:text>not </xsl:text>
    </xsl:if>
    <xsl:apply-templates select="*"/>
  </xsl:template>
  
  <xsl:template match="equals">
    <xsl:value-of select="x:value(@arg1)"/>
    <xsl:text> == </xsl:text>
    <xsl:value-of select="x:value(@arg2)"/>
  </xsl:template>
  
  <xsl:template match="istrue">
    <xsl:value-of select="x:value(@value)"/>
    <xsl:text> == "true"</xsl:text>
  </xsl:template>
  
  <xsl:template match="isfalse">
    <xsl:text>not </xsl:text>
    <xsl:value-of select="x:value(@value)"/>
    <xsl:text> == "true"</xsl:text>
  </xsl:template>
  
  <xsl:template match="isabsolute">
    <xsl:text>is_absolute(</xsl:text>
    <xsl:value-of select="x:value(@path)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>
  
  <xsl:template match="os">
    <xsl:text>os.name == </xsl:text>
    <xsl:value-of select="x:value(@arch | @family)"/>
  </xsl:template>
  
  <xsl:template match="contains">
    <xsl:value-of select="x:value(@substring)"/>
    <xsl:text> in </xsl:text>
    <xsl:value-of select="x:value(@string)"/>
  </xsl:template>
  
  <xsl:template match="isset">
    <xsl:text>"</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:text>" in properties</xsl:text>
  </xsl:template>
  
  <xsl:template match="not/isset">
    <xsl:text>"</xsl:text>
    <xsl:value-of select="@property"/>
    <xsl:text>" not in properties</xsl:text>
  </xsl:template>
  
  <xsl:template match="condition//available[@file]">
    <xsl:text>os.path.exists(</xsl:text>
    <xsl:value-of select="x:value(@file)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>
  
  <xsl:template match="available[@classname]">
    <xsl:text>class_available(</xsl:text>
    <xsl:value-of select="x:value(@classname)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>
  
  <xsl:function name="x:value">
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="string-length($value) = 0">""</xsl:when>
      <xsl:otherwise>
        <xsl:variable name="v" as="xs:string*">
          <xsl:analyze-string select="$value" regex="\$\{{(.+?)\}}">
            <xsl:matching-substring>
              <xsl:choose>
                <xsl:when test="regex-group(1) = 'file.separator'">
                  <xsl:text>os.sep</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:sequence select="concat('properties[&quot;', regex-group(1), '&quot;]')"/>    
                </xsl:otherwise>
              </xsl:choose>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
              <xsl:if test="string-length(.) gt 0">
                <xsl:sequence select="concat('&quot;', replace(replace(., '&#xA;', '\\n'), '&quot;', '\\&quot;'), '&quot;')"/>
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
  </xsl:function>
  
  <xsl:template match="*" priority="-2">
    <xsl:message>No mapping for <xsl:value-of select="name()"/></xsl:message>
    <xsl:apply-templates select="*"/>
  </xsl:template>
  
  
</xsl:stylesheet>