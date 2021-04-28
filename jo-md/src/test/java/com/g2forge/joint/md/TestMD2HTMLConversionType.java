package com.g2forge.joint.md;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.io.Filename;
import com.g2forge.alexandria.java.io.HBinaryIO;
import com.g2forge.alexandria.java.io.HTextIO;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.md.flexmark.FlexmarkConverter;

import lombok.AccessLevel;
import lombok.Getter;

public class TestMD2HTMLConversionType {
	@Rule
	@Getter(AccessLevel.PROTECTED)
	public final TestName name = new TestName();

	@Test
	public void escapeNone() throws IOException {
		testLink("File Name.html", "File Name.md");
	}

	@Test
	public void escapePercent() throws IOException {
		testLink("File Name.html", "File%20Name.md");
	}

	@Test
	public void escapePlus() throws IOException {
		testLink("File Name.html", "File+Name.md");
	}

	@Test
	public void image() throws IOException {
		test("<p><img src=\"image.png\" alt=\"converted\" /></p>", "![converted](image.puml)");
	}

	@Test
	public void nestedDirectory() throws IOException {
		testLink("./Nested/Other.html", "./Nested/Other.md");
	}

	@Test
	public void nestedFilename() throws IOException {
		testLink("Nested/Other.html", "Nested/Other.md");
	}

	@Test
	public void otherDirectory() throws IOException {
		testLink("./Other.html", "./Other.md");
	}

	@Test
	public void otherFilename() throws IOException {
		testLink("Other.html", "Other.md");
	}

	@Test
	public void parent() throws IOException {
		testLink("../Other.html", "../Other.md");
	}

	@Test
	public void root() throws IOException {
		testLink("/Other.html", "/Other.md");
	}

	@Test
	public void selfDirectory() throws IOException {
		testLink("./File.html", "./File.md");
	}

	@Test
	public void selfFilename() throws IOException {
		testLink("File.html", "File.md");
	}

	@Test
	public void sibling() throws IOException {
		testLink("../Directory/Other.html", "../Directory/Other.md");
	}

	@Test
	public void suffixBoth() throws IOException {
		testLink("?a=b#fragment", "?a=b#fragment");
	}

	@Test
	public void suffixFragmentOnly() throws IOException {
		testLink("#fragment", "#fragment");
	}

	@Test
	public void suffixQueryOnly() throws IOException {
		testLink("?a=b", "?a=b");
	}

	protected void test(String expectedHTML, String markdownInput) throws IOException {
		final String inputFilename = "File.md", outputFilename = "File.html";
		final Path directory = Paths.get("A/B");

		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + getClass().getSimpleName() + "_" + getName().getMethodName()), null);
		/*final ICloseableSupplier<Path> inputResource = new Resource(getClass(), inputFilename).getPath()*/) {

			final Path inputRoot = fs.getPath("/input"), inputRelative = directory.resolve(inputFilename);
			final Path outputRoot = fs.getPath("/output");
			Files.createDirectories(inputRoot.resolve(directory));
			Files.createDirectories(outputRoot.resolve(directory));

			// Write out the input markdown
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(markdownInput.getBytes());
				final OutputStream outputStream = Files.newOutputStream(inputRoot.resolve(inputRelative))) {
				HBinaryIO.copy(inputStream, outputStream);
			}

			new FileConversion(inputRoot, inputRelative, outputRoot, new MD2HTMLConversionType(new FlexmarkConverter())).invoke(new IConversionContext() {
				@Override
				public Set<IConversion> getConversions(Path input) {
					return HCollection.asSet(new IConversion() {
						@Override
						public Set<Path> getInputs() {
							return HCollection.asSet(input);
						}

						@Override
						public Set<Path> getOutputs() {
							final Path relative = inputRoot.relativize(input);
							final IMediaType inputMediaType = ExtendedMediaType.getRegistry().computeMediaType(relative), outputMediaType;
							if (inputMediaType == MediaType.Markdown) outputMediaType = MediaType.HTML;
							else if (inputMediaType == ExtendedMediaType.PlantUML) outputMediaType = MediaType.PNG;
							else outputMediaType = inputMediaType;

							final Path output = (outputMediaType == null) ? relative : Filename.replaceLastExtension(relative, outputMediaType.getFileExtensions().getDefaultExtension());
							return HCollection.asSet(outputRoot.resolve(output));
						}

						@Override
						public void invoke(IConversionContext context) {
							throw new UnsupportedOperationException();
						}
					});
				}

				@Override
				public Mode getMode() {
					return Mode.Build;
				}

				@Override
				public void register(AutoCloseable closeable) {
					throw new UnsupportedOperationException();
				}
			});

			try (final InputStream stream = Files.newInputStream(outputRoot.resolve(directory).resolve(outputFilename))) {
				HAssert.assertEquals(expectedHTML.trim(), HTextIO.readAll(stream, true).trim());
			}
		}
	}

	protected void testLink(String expectedHTMLLink, String markdownInputLink) throws IOException {
		test("<p><a href=\"" + expectedHTMLLink + "\">Text</a></p>", "[Text](" + markdownInputLink + ")");
	}
}
