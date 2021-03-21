package com.g2forge.joint.md.enigma;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import com.g2forge.alexandria.java.core.enums.EnumException;
import com.g2forge.alexandria.java.core.error.NotYetImplementedError;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.type.function.TypeSwitch1;
import com.g2forge.enigma.document.model.Block;
import com.g2forge.enigma.document.model.IBlock;
import com.g2forge.enigma.document.model.IDocElement;
import com.g2forge.enigma.document.model.ISpan;
import com.g2forge.enigma.document.model.Section;
import com.g2forge.enigma.document.model.Text;
import com.g2forge.enigma.web.html.model.html.Div;
import com.g2forge.enigma.web.html.model.html.Header;
import com.g2forge.enigma.web.html.model.html.Paragraph;

public class DocConverter {
	protected static class DocConvertContext implements IDocConvertContext {
		protected static final IFunction1<Object, IExplicitDocElement> converter = new TypeSwitch1.FunctionBuilder<Object, IExplicitDocElement>().with(builder -> {
			builder.add(IExplicitDocElement.class, IFunction1.identity());
			builder.add(Text.class, s -> c -> s.getText());
			builder.add(Block.class, s -> c -> {
				final List<Object> contents = s.getContents().stream().map(content -> c.toExplicit(content, IBlock.class).convert(c)).collect(Collectors.toList());
				switch (s.getType()) {
					case Document:
						if (contents.size() == 1) return HCollection.getOne(contents);
						return new Div(contents);
					case Paragraph:
						return new Paragraph(contents);
					default:
						throw new EnumException(Block.Type.class, s.getType());
				}
			});
			builder.add(Section.class, s -> c -> {
				final Object title = c.toExplicit(s.getTitle(), ISpan.class).convert(c);
				final Object body = c.toExplicit(s.getBody(), IBlock.class).convert(c);
				return new Div(new Header(titleToID(title), 1, title), body);
			});

			builder.fallback(s -> {
				throw new NotYetImplementedError(String.format("Converter does not yet support elements of type %1$s", s.getClass().getSimpleName()));
			});
		}).build();

		protected static String titleToID(final Object title) {
			return title.toString().toLowerCase();
		}

		@Override
		public IExplicitDocElement toExplicit(Object object, Type type) {
			return converter.apply(object);
		}
	}

	public Object toWeb(IDocElement element) {
		final DocConvertContext context = new DocConvertContext();
		return context.toExplicit(element, IDocElement.class).convert(context);
	}
}
