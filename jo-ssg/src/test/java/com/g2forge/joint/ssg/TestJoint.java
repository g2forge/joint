package com.g2forge.joint.ssg;

import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.concurrent.HConcurrent;
import com.g2forge.alexandria.java.core.helpers.HBinary;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.alexandria.java.io.file.CompareWalker;
import com.g2forge.alexandria.java.io.file.CopyWalker;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.ssg.operation.Operation;

import lombok.AccessLevel;
import lombok.Getter;
import net.sourceforge.plantuml.security.ImageIO;

public class TestJoint {
	protected static final IFunction1<Path, IFunction1<Path, String>> HASHFUNCTIONFUNCTION = path -> {
		final IMediaType mediaType = ExtendedMediaType.getRegistry().computeMediaType(path);

		// If the media type is a PNG (probably from PlantUML) then just hash the image data, not the metadata
		if (MediaType.PNG.equals(mediaType)) {
			return actual -> {
				try {
					final byte[] expectedBytes = ((DataBufferByte) ImageIO.read(Files.newInputStream(actual)).getData().getDataBuffer()).getData();
					return HBinary.toHex(HIO.sha1(expectedBytes, ByteArrayInputStream::new));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};
		}

		// Fall back to the default hash functions
		if ((mediaType != null) && mediaType.isText()) return CompareWalker.TextHashFunction.create();
		return CompareWalker.BinaryHashFunction.create();
	};

	@Rule
	@Getter(AccessLevel.PROTECTED)
	public final TestName name = new TestName();

	protected void compareBuild(final Path input, final Path expected, final Path actual) throws Exception {
		HAssert.assertEquals(Integer.valueOf(0), createJoint(input, actual, Operation.Build).call());
		CompareWalker.builder().root(actual).hashFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(expected);
	}

	@Test
	public void copy() throws Exception {
		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + getName().getMethodName()), null);
			final ICloseableSupplier<Path> inputCloseable = new Resource(getClass(), "copy").getPath()) {
			final Path input = inputCloseable.get();
			compareBuild(input, input, fs.getPath("/output"));
		}
	}

	protected Joint createJoint(final Path input, final Path output, final Operation operation) {
		return Joint.builder().input(input).output(output).operation(operation).components(EnumSet.of(Joint.Component.StaticContent)).build();
	}

	@Test
	public void md() throws Exception {
		test();
	}

	@Test
	public void puml() throws Exception {
		test();
	}

	@Test
	public void rewrite() throws Exception {
		test();
	}

	@Test
	public void serve() throws Exception {
		try (final TempDirectory temp = new TempDirectory();
			final ICloseableSupplier<Path> sourcePath = new Resource(getClass(), "copy").getPath();) {
			final Path input = temp.get().resolve("input"), output = temp.get().resolve("output");
			Files.createDirectories(output);
			final Path source = sourcePath.get();
			CopyWalker.builder().target(input).build().walkFileTree(source);

			try (final ICloseable closeable = createJoint(input, output, Operation.Serve).start()) {
				CompareWalker.builder().root(output).hashFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(source);

				// Test that new files get copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).append("Text").close();
				HConcurrent.wait(500);
				CompareWalker.builder().root(output).hashFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(input);

				// Test that modified files get re-copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.APPEND, StandardOpenOption.WRITE).append("Other").close();
				HConcurrent.wait(500);
				CompareWalker.builder().root(output).hashFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(input);
			}
		}
	}

	protected void test() throws Exception, IOException {
		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + getName().getMethodName()), null)) {
			final String name = getName().getMethodName();
			try (final ICloseableSupplier<Path> inputPath = new Resource(getClass(), name + "/input").getPath();
				final ICloseableSupplier<Path> outputPath = new Resource(getClass(), name + "/output").getPath()) {
				compareBuild(inputPath.get(), outputPath.get(), fs.getPath("/output"));
			}
		}
	}
}
