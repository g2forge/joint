package com.g2forge.joint.md.flexmark;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.dataaccess.IDataSink;
import com.g2forge.alexandria.java.io.dataaccess.IDataSource;
import com.g2forge.alexandria.java.type.ref.ITypeRef;
import com.g2forge.joint.md.IMDConverter;
import com.g2forge.joint.md.flexmark.frontmatter.TextFrontMatterBlock;
import com.g2forge.joint.md.flexmark.frontmatter.TextFrontMatterExtension;
import com.g2forge.joint.md.flexmark.frontmatter.TextFrontMatterVisitor;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.LinkResolverFactory;
import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class FlexmarkConverter implements IMDConverter {
	public interface IDocument {
		public String getFrontMatter();

		public void render(IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs);
	}

	@RequiredArgsConstructor
	@Getter
	protected static class RewritingLinkResolverFactory implements LinkResolverFactory {
		protected class CustomLinkResolver implements LinkResolver {
			public CustomLinkResolver(LinkResolverBasicContext context) {}

			@NotNull
			@Override
			public ResolvedLink resolveLink(@NotNull Node node, @NotNull LinkResolverBasicContext context, @NotNull ResolvedLink link) {
				return link.withUrl(getRewrite().apply(link.getUrl()));
			}
		}

		protected final IFunction1<? super String, ? extends String> rewrite;

		@Override
		public boolean affectsGlobalScope() {
			return false;
		}

		@NotNull
		@Override
		public LinkResolver apply(@NotNull LinkResolverBasicContext context) {
			return new CustomLinkResolver(context);
		}

		@Nullable
		@Override
		public Set<Class<?>> getAfterDependents() {
			return null;
		}

		@Nullable
		@Override
		public Set<Class<?>> getBeforeDependents() {
			return null;
		}
	}

	protected final MutableDataSet options = new MutableDataSet().set(Parser.SPACE_IN_LINK_URLS, true);

	public FlexmarkConverter() {
		options.set(Parser.EXTENSIONS, HCollection.asList(TablesExtension.create(), StrikethroughExtension.create(), TaskListExtension.create(), TextFrontMatterExtension.create(), TocExtension.create()));
	}

	@Override
	public void convert(IDataSource source, IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs) {
		parse(source).render(sink, rewriteURLs);
	}

	public IDocument parse(IDataSource source) {
		final Parser parser = Parser.builder(options).build();
		final Document document;
		try (final Reader reader = source.getReader(ITypeRef.of(Reader.class))) {
			document = parser.parseReader(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new IDocument() {
			@Override
			public String getFrontMatter() {
				final TextFrontMatterVisitor visitor = new TextFrontMatterVisitor();
				visitor.visit(document);
				final TextFrontMatterBlock block = visitor.getBlock();
				if (block == null) return null;
				return block.getBody().toString();
			}

			@Override
			public void render(IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs) {
				final @NotNull Builder rendererBuilder = HtmlRenderer.builder(options);
				if (rewriteURLs != null) rendererBuilder.linkResolverFactory(new RewritingLinkResolverFactory(rewriteURLs));
				final HtmlRenderer renderer = rendererBuilder.build();
				try (final Writer writer = sink.getWriter(ITypeRef.of(Writer.class))) {
					renderer.render(document, writer);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
	}

}
