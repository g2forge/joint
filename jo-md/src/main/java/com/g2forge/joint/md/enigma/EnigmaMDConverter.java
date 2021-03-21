package com.g2forge.joint.md.enigma;

import java.io.IOException;
import java.io.Writer;

import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.alexandria.java.io.dataaccess.IDataSink;
import com.g2forge.alexandria.java.io.dataaccess.IDataSource;
import com.g2forge.alexandria.java.type.ref.ITypeRef;
import com.g2forge.enigma.document.convert.WikitextParser;
import com.g2forge.enigma.document.model.IDocElement;
import com.g2forge.enigma.web.html.convert.HTMLRenderer;
import com.g2forge.joint.md.IMDConverter;

import lombok.AccessLevel;
import lombok.Getter;

public class EnigmaMDConverter implements IMDConverter {
	@Getter(lazy = true, value = AccessLevel.PROTECTED)
	private final WikitextParser markdown = WikitextParser.getMarkdown();

	@Getter(lazy = true, value = AccessLevel.PROTECTED)
	private final DocConverter converter = new DocConverter();

	@Getter(lazy = true, value = AccessLevel.PROTECTED)
	private final HTMLRenderer renderer = new HTMLRenderer();

	@Override
	public void convert(IDataSource source, IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs) {
		if (rewriteURLs != null) throw new IllegalArgumentException("Does not support URL rewriting yet!");
		final IDocElement parsed = getMarkdown().parse(source);
		final Object converted = getConverter().toWeb(parsed);
		final String rendered = getRenderer().render(converted);
		try (final Writer writer = sink.getWriter(ITypeRef.of(Writer.class))) {
			writer.append(rendered);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
