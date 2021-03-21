package com.g2forge.joint.md.mylyncommonmark;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.mylyn.wikitext.commonmark.CommonMarkLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;

import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.alexandria.java.io.dataaccess.IDataSink;
import com.g2forge.alexandria.java.io.dataaccess.IDataSource;
import com.g2forge.alexandria.java.type.ref.ITypeRef;
import com.g2forge.joint.md.IMDConverter;

public class MylynCommonmarkConverter implements IMDConverter {
	@Override
	public void convert(IDataSource source, IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs) {
		final MarkupParser parser = new MarkupParser(new CommonMarkLanguage());
		try (final Reader reader = source.getReader(ITypeRef.of(Reader.class)); final Writer writer = sink.getWriter(ITypeRef.of(Writer.class))) {
			final HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer, true) {
				@Override
				protected String makeUrlAbsolute(String url) {
					if (rewriteURLs != null) return rewriteURLs.apply(url);
					return super.makeUrlAbsolute(url);
				}
			};
			builder.setEmitAsDocument(false);
			parser.setBuilder(builder);
			parser.parse(reader);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
