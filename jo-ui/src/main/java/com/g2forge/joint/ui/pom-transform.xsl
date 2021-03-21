<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://maven.apache.org/POM/4.0.0">

	<xsl:strip-space elements="*" />
	<xsl:output omit-xml-declaration="no" indent="yes" />
	<xsl:param name="name" />

	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="/project/artifactId">
		<artifactId>
			<xsl:value-of select="$name" />
		</artifactId>
		<groupId>com.g2forge.joint.ui</groupId>
		<version>0.0.1-SNAPSHOT</version>
	</xsl:template>
	<xsl:template match="/project/parent">
		<parent>
			<groupId>com.g2forge.alexandria</groupId>
			<artifactId>ax-root</artifactId>
			<version>0.0.15</version>
		</parent>
	</xsl:template>
	<xsl:template match="/project/dependencies"></xsl:template>
</xsl:stylesheet>