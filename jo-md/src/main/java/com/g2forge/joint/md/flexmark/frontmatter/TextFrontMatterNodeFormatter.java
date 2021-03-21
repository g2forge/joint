package com.g2forge.joint.md.flexmark.frontmatter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.vladsch.flexmark.formatter.FormattingPhase;
import com.vladsch.flexmark.formatter.MarkdownWriter;
import com.vladsch.flexmark.formatter.NodeFormatter;
import com.vladsch.flexmark.formatter.NodeFormatterContext;
import com.vladsch.flexmark.formatter.NodeFormatterFactory;
import com.vladsch.flexmark.formatter.NodeFormattingHandler;
import com.vladsch.flexmark.formatter.PhasedNodeFormatter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;

public class TextFrontMatterNodeFormatter implements PhasedNodeFormatter {
	public static class Factory implements NodeFormatterFactory {
		@NotNull
		@Override
		public NodeFormatter create(@NotNull DataHolder options) {
			return new TextFrontMatterNodeFormatter(options);
		}
	}

	public TextFrontMatterNodeFormatter(DataHolder options) {}

	@Nullable
	@Override
	public Set<FormattingPhase> getFormattingPhases() {
		return HCollection.asSet(FormattingPhase.DOCUMENT_FIRST);
	}

	@Nullable
	@Override
	public Set<Class<?>> getNodeClasses() {
		return null;
	}

	@Nullable
	@Override
	public Set<NodeFormattingHandler<?>> getNodeFormattingHandlers() {
		return new HashSet<>(Collections.singletonList(new NodeFormattingHandler<>(TextFrontMatterBlock.class, TextFrontMatterNodeFormatter.this::render)));
	}

	private void render(TextFrontMatterBlock node, NodeFormatterContext context, MarkdownWriter markdown) {}

	@Override
	public void renderDocument(@NotNull NodeFormatterContext context, @NotNull MarkdownWriter markdown, @NotNull Document document, @NotNull FormattingPhase phase) {
		if (phase == FormattingPhase.DOCUMENT_FIRST) {
			final Node node = document.getFirstChild();
			if (node instanceof TextFrontMatterBlock) markdown.openPreFormatted(false).append(node.getChars()).blankLine().closePreFormatted();
		}
	}
}
