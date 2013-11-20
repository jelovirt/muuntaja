<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita="http://dita-ot.sourceforge.net" 
                xmlns:x="x"
                exclude-result-prefixes="xs x dita"
                version="2.0">

  <xsl:import href="ant2scala.xsl"/>
  
  <xsl:output method="xml" indent="yes"/>
  
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
    <xsl:copy-of select="$preprocessed"/>
  </xsl:template>
  
</xsl:stylesheet>