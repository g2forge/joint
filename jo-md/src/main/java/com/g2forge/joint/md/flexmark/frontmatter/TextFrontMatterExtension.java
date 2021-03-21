package com.g2forge.joint.md.flexmark.frontmatter;

import com.g2forge.alexandria.java.core.marker.ISingleton;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;

public class TextFrontMatterExtension implements Parser.ParserExtension, Formatter.FormatterExtension, ISingleton {
	protected static final TextFrontMatterExtension INSTANCE = new TextFrontMatterExtension();

	public static TextFrontMatterExtension create() {
		return INSTANCE;
	}

	protected TextFrontMatterExtension() {}

	@Override
	public void extend(Formatter.Builder formatterBuilder) {
		formatterBuilder.nodeFormatterFactory(new TextFrontMatterNodeFormatter.Factory());
	}

	@Override
	public void extend(Parser.Builder parserBuilder) {
		parserBuilder.customBlockParserFactory(new TextFrontMatterBlockParser.Factory());
	}

	@Override
	public void parserOptions(MutableDataHolder options) {}

	@Override
	public void rendererOptions(MutableDataHolder options) {}
}