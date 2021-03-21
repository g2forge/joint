package com.g2forge.joint.md.flexmark.frontmatter;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

import lombok.Getter;

public class TextFrontMatterVisitor implements ITextFrontMatterVisitor {
	protected final NodeVisitor nodeVisitor;

	@Getter
	protected TextFrontMatterBlock block;

	public TextFrontMatterVisitor() {
		nodeVisitor = new NodeVisitor(new VisitHandler<?>[] { new VisitHandler<>(TextFrontMatterBlock.class, this::visit) });
	}

	public void visit(Node node) {
		nodeVisitor.visit(node);
	}

	@Override
	public void visit(TextFrontMatterBlock block) {
		if (this.block != null) throw new IllegalStateException();
		this.block = block;
	}
}