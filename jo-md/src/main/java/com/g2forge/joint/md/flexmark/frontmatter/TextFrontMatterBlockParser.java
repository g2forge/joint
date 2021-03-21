package com.g2forge.joint.md.flexmark.frontmatter;

import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.block.AbstractBlockParser;
import com.vladsch.flexmark.parser.block.AbstractBlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.CustomBlockParserFactory;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.parser.core.DocumentBlockParser;
import com.vladsch.flexmark.util.ast.BlockContent;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import lombok.Getter;

public class TextFrontMatterBlockParser extends AbstractBlockParser {
	private static class BlockFactory extends AbstractBlockParserFactory {
		private BlockFactory(DataHolder options) {
			super(options);
		}

		@Override
		public BlockStart tryStart(ParserState parserState, MatchedBlockParser matchedBlockParser) {
			final CharSequence line = parserState.getLine();
			final BlockParser parser = matchedBlockParser.getBlockParser();
			// Only parse front matter at the start of the document
			if ((parser instanceof DocumentBlockParser) && (parser.getBlock().getFirstChild() == null) && REGEX_BEGIN.matcher(line).matches()) return BlockStart.of(new TextFrontMatterBlockParser()).atIndex(parserState.getNextNonSpaceIndex());
			return BlockStart.none();
		}
	}

	public static class Factory implements CustomBlockParserFactory {
		@Override
		public boolean affectsGlobalScope() {
			return false;
		}

		@NotNull
		@Override
		public BlockParserFactory apply(@NotNull DataHolder options) {
			return new BlockFactory(options);
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

	protected static final Pattern REGEX_BEGIN = Pattern.compile("^-{3}(\\s.*)?");

	protected static final Pattern REGEX_END = Pattern.compile("^(-{3}|\\.{3})(\\s.*)?");

	protected boolean inside;

	@Getter
	protected final TextFrontMatterBlock block;

	protected final BlockContent content;

	public TextFrontMatterBlockParser() {
		inside = true;
		block = new TextFrontMatterBlock();
		content = new BlockContent();
	}

	@Override
	public void addLine(ParserState state, BasedSequence line) {
		content.add(line, state.getIndent());
	}

	@Override
	public void closeBlock(ParserState state) {
		block.setContent(content.getLines());
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public void parseInlines(InlineParser inlineParser) {}

	@Override
	public BlockContinue tryContinue(ParserState state) {
		final BasedSequence line = state.getLine();

		if (inside) {
			if (REGEX_END.matcher(line).matches()) {
				addLine(state, line);
				return BlockContinue.finished();
			}
			return BlockContinue.atIndex(state.getIndex());
		} else if (REGEX_BEGIN.matcher(line).matches()) {
			inside = true;
			return BlockContinue.atIndex(state.getIndex());
		}

		return BlockContinue.none();
	}
}
