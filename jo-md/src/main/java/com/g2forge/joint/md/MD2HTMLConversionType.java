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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@Slf4j
public class MD2HTMLConversionType implements IFileConversionType {
	@RequiredArgsConstructor
	@Getter(AccessLevel.PROTECTED)
	protected static class Translator implements IFunction1<String, String> {
		protected static final Pattern PATTERN_URI = Pattern.compile("^([A-Za-z][-+.A-Za-z0-9]*):(.*)$");

		protected static final IEscaper PATH_ESCAPER = SequenceEscaper.builder().escape(" ", "+", true).escape("+", "%2B", true).build();

		/** A function from normalized input paths to the converted result, or {@code null} if the input path was not converted. */
		protected final IFunction1<Path, Path> conversion;

		/** The root directory containing all the input files. */
		protected final Path inputRoot;

		/** The input file the link we're translating comes from. */
		protected final Path input;

		/** The root directory to put output files in. */
		protected final Path outputRoot;

		/** The output file which is being generated from the input file. */
		protected final Path output;

		@Override
		public String apply(String string) {
			// Don't translate URIs with schemes
			if (PATTERN_URI.matcher(string).matches()) return string;

			final URI uri;
			final String inputPath;
			{ // Don't rewrite "absolute" URIs, those are URIs with a scheme and all that, we're only here for the paths...
				final String escaped = PATH_ESCAPER.escape(string);
				try {
					uri = new URI(escaped);
					if (uri.isAbsolute() || uri.isOpaque() || (uri.getAuthority() != null)) return string;
				} catch (URISyntaxException e) {
					throw new RuntimeException(String.format("Failed to translate \"%1$s\" (escaped as \"%2$s\")", string, escaped), e);
				}
				inputPath = PATH_ESCAPER.unescape(uri.getPath());
			}

			// Figure out the link target input path and normalize it
			final boolean isAbsolute = inputPath.startsWith("/");
			final Path inputParent = getInput().getParent();
			final Path targetInput = isAbsolute ? getInputRoot().resolve(inputPath.substring(1)) : inputParent.resolve(inputPath);
			{
				final Path actual = targetInput.toAbsolutePath().normalize();
				final Path expectedBase = getInputRoot().toAbsolutePath().normalize();
				if (!actual.startsWith(expectedBase)) { throw new IllegalArgumentException(String.format("URI \"%1$s\" relative to \"%2$s\" escapes input root \"%3$S\", which is both incorrect and a potential security issue", string, inputParent, getInputRoot())); }
			}

			// Determine if that input was converted, and if not, don't translate the link
			final Path targetOutput = conversion.apply(targetInput.normalize());
			if (targetOutput == null) return string;
			// Get the path to that output relative to the parent of the target of this conversion
			final Path targetRelative = (isAbsolute ? getOutputRoot() : getOutput().getParent()).relativize(targetOutput);
			final String targetPath = (isAbsolute ? "/" : "") + HCollection.toCollection(targetRelative).stream().map(Object::toString).collect(Collectors.joining("/"));

			final StringBuilder target = new StringBuilder();
			final boolean hasQuery = uri.getQuery() != null, hasFragment = uri.getFragment() != null;
			if (!".".equals(targetPath) || (!hasQuery && !hasFragment)) target.append(targetPath);
			if (hasQuery) target.append('?').append(uri.getQuery());
			if (hasFragment) target.append('#').append(uri.getFragment());

			return target.toString();
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
		final Translator translator = new Translator(i -> {// Figure out the conversion that uses that input
			final Set<IConversion> conversions = context.getConversions(i.normalize());
			if ((conversions == null) || conversions.isEmpty()) return null;
			// Get the output of that conversion
			return HCollection.getOne(HCollection.getOne(conversions).getOutputs());
		}, conversion.getInputRoot(), input, conversion.getOutputRoot(), output);

		try {
			getConverter().convert(new PathDataSource(input, StandardOpenOption.READ), new PathDataSink(output, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), translator);
		} catch (Throwable throwable) {
			throw new RuntimeException(String.format("Exception while converting %1$s to %2$s", input, output), throwable);
		}
		log.info("Converted {} to {}", input, output);
	}
}
