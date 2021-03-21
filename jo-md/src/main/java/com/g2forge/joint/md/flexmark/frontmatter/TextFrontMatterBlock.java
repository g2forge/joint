package com.g2forge.joint.md.flexmark.frontmatter;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import lombok.Getter;

public class TextFrontMatterBlock extends Block {
	@Getter
	protected BasedSequence body;

	public String getFrontMatterAll() {
		return getChars().toString();
	}

	public String getFrontMatterBody() {
		return getBody().toString();
	}

	@NotNull
	@Override
	public BasedSequence[] getSegments() {
		return EMPTY_SEGMENTS;
	}

	@Override
	public void setContent(@NotNull List<BasedSequence> lineSegments) {
		super.setContent(lineSegments);
		this.body = lineSegments.size() <= 2 ? BasedSequence.NULL : lineSegments.get(1).baseSubSequence(lineSegments.get(1).getStartOffset(), lineSegments.get(lineSegments.size() - 2).getEndOffset());
	}
}
