package com.g2forge.joint.md;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.Filename;
import com.g2forge.alexandria.java.io.dataaccess.PathDataSink;
import com.g2forge.alexandria.java.io.dataaccess.PathDataSource;
import com.g2forge.alexandria.java.text.escape.IEscaper;
import com.g2forge.alexandria.java.text.escape.SequenceEscaper;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.core.copy.IFileConversionType;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@Slf4j
public class MD2HTMLConversionType implements IFileConversionType {
	@RequiredArgsConstructor
	protected static class Translator implements IFunction1<String, String> {
		protected static final Pattern PATTERN_URI = Pattern.compile("^([A-Za-z][-+.A-Za-z0-9]*):(.*)$");

		protected static final IEscaper PATH_ESCAPER = SequenceEscaper.builder().escape(" ", "+", true).escape("+", "%2B", true).build();

		protected final IConversionContext context;

		protected final FileConversion conversion;

		protected final Path input;

		protected final Path output;

		@Override
		public String apply(String string) {
			// Don't translate URIs with schemes
			if (PATTERN_URI.matcher(string).matches()) return string;

			// Don't rewrite "absolute" URIs, those are URIs with a scheme and all that, we're only here for the paths...
			{
				final String escaped = PATH_ESCAPER.escape(string);
				final URI uri;
				try {
					uri = new URI(escaped);
					if (uri.isAbsolute() || uri.isOpaque() || (uri.getAuthority() != null)) return string;
				} catch (URISyntaxException e) {
					throw new RuntimeException(String.format("Failed to translate \"%1$s\" (escaped as \"%2$s\")", string, escaped), e);
				}
			}

			// Figure out the link target input path and normalize it
			final String inputPath = PATH_ESCAPER.unescape(string);
			final boolean isAbsolute = inputPath.startsWith("/");
			final Path inputParent = input.getParent();
			final Path targetNormalized = isAbsolute ? conversion.getInputRoot().resolve(inputPath.substring(1)) : inputParent.resolve(inputPath).normalize();
			if (!isAbsolute && !targetNormalized.startsWith(conversion.getInputRoot())) { throw new IllegalArgumentException(String.format("URI \"%1$s\" relative to \"%2$s\" escapes input root \"%3$S\", which is both incorrect and a potential security issue", string, inputParent, conversion.getInputRoot())); }

			// Figure out the conversion that uses that input
			final Set<IConversion> conversions = context.getConversions(targetNormalized);
			if (conversions.isEmpty()) return string;
			// Get the output of that conversion
			final Path targetOutput = HCollection.getOne(HCollection.getOne(conversions).getOutputs());
			// Get the path to that output relative to the parent of the target of this conversion
			final Path targetRelative = (isAbsolute ? conversion.getOutputRoot() : output.getParent()).relativize(conversion.getOutputRoot().resolve(targetOutput));
			final String targetString = (isAbsolute ? "/" : "") + HCollection.toCollection(targetRelative).stream().map(Object::toString).collect(Collectors.joining("/"));

			return targetString;
		}
	}

	protected final IMDConverter converter;

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		final Path inputRelative = conversion.getInputRelative();
		if (new Filename(inputRelative).getFullName().equals("README")) return Filename.modifyFilename(inputRelative, "index." + MediaType.HTML.getFileExtensions().getDefaultExtension());
		else return Filename.replaceLastExtension(inputRelative, MediaType.HTML.getFileExtensions().getDefaultExtension());
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		// Construct the URL translator
		final Translator translator = new Translator(context, conversion, input, output);

		try {
			getConverter().convert(new PathDataSource(input, StandardOpenOption.READ), new PathDataSink(output, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), translator);
		} catch (Throwable throwable) {
			throw new RuntimeException(String.format("Exception while converting %1$s to %2$s", input, output), throwable);
		}
		log.info("Converted {} to {}", input, output);
	}
}