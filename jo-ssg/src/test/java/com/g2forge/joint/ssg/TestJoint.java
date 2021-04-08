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
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.concurrent.HConcurrent;
import com.g2forge.alexandria.java.core.helpers.HBinary;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.helpers.HCollector;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.alexandria.java.io.file.CopyWalker;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.java.io.file.compare.CompareWalker;
import com.g2forge.alexandria.java.io.file.compare.IFileCompareGroup;
import com.g2forge.alexandria.java.io.file.compare.IFileCompareGroupFunction;
import com.g2forge.alexandria.java.io.file.compare.SHA1HashFileCompareGroupFunction;
import com.g2forge.alexandria.java.io.file.compare.TextHashFileCompareGroupFunction;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.gearbox.image.ImageCluster;
import com.g2forge.gearbox.image.PHashImageComparator;
import com.g2forge.gearbox.image.PHashImageComparator.CharacterizedImage;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.ssg.operation.Operation;

import lombok.AccessLevel;
import lombok.Getter;
import net.sourceforge.plantuml.security.ImageIO;

public class TestJoint {
	protected static final int COMPARATOR_GROUPDISTANCE = 50;

	protected static final PHashImageComparator COMPARATOR = new PHashImageComparator(16);

	protected static final IFunction1<Path, IFileCompareGroupFunction<?>> HASHFUNCTIONFUNCTION = path -> {
		final IMediaType mediaType = ExtendedMediaType.getRegistry().computeMediaType(path);

		// If the media type is a PNG (probably from PlantUML) then just hash the image data, not the metadata
		if (MediaType.PNG.equals(mediaType)) {
			return new IFileCompareGroupFunction<CharacterizedImage>() {
				@Override
				public Map<IFileCompareGroup, Set<Path>> group(Map<Path, ? extends CharacterizedImage> hashes) {
					final Map<CharacterizedImage, Path> reverse = hashes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, HCollector.mergeFail(), IdentityHashMap::new));
					final List<ImageCluster<CharacterizedImage>> clusters = ImageCluster.cluster(COMPARATOR, hashes.values(), COMPARATOR_GROUPDISTANCE);
					final Map<IFileCompareGroup, Set<Path>> retVal = new HashMap<>();
					for (ImageCluster<CharacterizedImage> cluster : clusters) {
						retVal.put(new IFileCompareGroup() {
							@Override
							public String describe(Collection<Path> roots, Path relative) {
								try {
									final byte[] expectedBytes = ((DataBufferByte) ImageIO.read(Files.newInputStream(HCollection.getAny(roots).resolve(relative))).getData().getDataBuffer()).getData();
									return HBinary.toHex(HIO.sha1(expectedBytes, ByteArrayInputStream::new));
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							}
						}, cluster.getImages().stream().map(reverse::get).collect(Collectors.toSet()));
					}
					return retVal;
				}

				@Override
				public CharacterizedImage hash(Path path) {
					return COMPARATOR.characterize(path);
				}
			};
		}

		// Fall back to the default hash functions
		if ((mediaType != null) && mediaType.isText()) return TextHashFileCompareGroupFunction.create();
		return SHA1HashFileCompareGroupFunction.create();
	};

	@Rule
	@Getter(AccessLevel.PROTECTED)
	public final TestName name = new TestName();

	protected void compareBuild(final Path input, final Path expected, final Path actual) throws Exception {
		HAssert.assertEquals(Integer.valueOf(0), createJoint(input, actual, Operation.Build).call());
		CompareWalker.builder().root(actual).groupFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(expected);
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
				CompareWalker.builder().root(output).groupFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(source);

				// Test that new files get copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).append("Text").close();
				HConcurrent.wait(1000);
				CompareWalker.builder().root(output).groupFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(input);

				// Test that modified files get re-copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.APPEND, StandardOpenOption.WRITE).append("Other").close();
				HConcurrent.wait(1000);
				CompareWalker.builder().root(output).groupFunctionFunction(HASHFUNCTIONFUNCTION).build().walkFileTree(input);
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
