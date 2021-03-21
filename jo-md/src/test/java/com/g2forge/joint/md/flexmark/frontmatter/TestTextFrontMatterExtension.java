package com.g2forge.joint.md.flexmark.frontmatter;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import org.junit.Test;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.dataaccess.ResourceDataSource;
import com.g2forge.alexandria.java.type.ref.ITypeRef;
import com.g2forge.alexandria.test.HAssert;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class TestTextFrontMatterExtension {
	@Test
	public void a() {
		final MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, HCollection.asList(TextFrontMatterExtension.create()));

		final ResourceDataSource source = new ResourceDataSource(new Resource(getClass(), "a.md"));

		final Parser parser = Parser.builder(options).build();
		final Document document;
		try (final Reader reader = source.getReader(ITypeRef.of(Reader.class))) {
			document = parser.parseReader(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		final TextFrontMatterVisitor visitor = new TextFrontMatterVisitor();
		visitor.visit(document);
		HAssert.assertEquals("A: a", visitor.getBlock().getFrontMatterBody().trim());
	}
}
