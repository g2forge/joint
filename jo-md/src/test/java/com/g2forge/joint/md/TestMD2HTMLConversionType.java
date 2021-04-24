package com.g2forge.joint.md;

import java.io.IOException;
import java.io.InputStream;
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

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.Filename;
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
	public void test() throws IOException {
		final String inputFilename = "ConversionType.md", outputFilename = "ConversionType.html";
		final Path directory = Paths.get("A/B");

		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + getClass().getSimpleName() + "_" + getName().getMethodName()), null);
			final ICloseableSupplier<Path> inputResource = new Resource(getClass(), inputFilename).getPath()) {

			final Path inputRoot = fs.getPath("/input"), inputRelative = directory.resolve(inputFilename);
			final Path outputRoot = fs.getPath("/output");
			Files.createDirectories(inputRoot.resolve(directory));
			Files.createDirectories(outputRoot.resolve(directory));
			Files.copy(inputResource.get(), inputRoot.resolve(inputRelative));

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

							final Path output = Filename.replaceLastExtension(relative, outputMediaType.getFileExtensions().getDefaultExtension());
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
				HAssert.assertEquals(new Resource(getClass(), outputFilename), HTextIO.readAll(stream, true));
			}
		}
	}
}
