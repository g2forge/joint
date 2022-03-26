package com.g2forge.joint.ssg;

import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.command.invocation.CommandInvocation;
import com.g2forge.alexandria.command.invocation.CommandInvocation.CommandInvocationBuilder;
import com.g2forge.alexandria.command.invocation.runner.IdentityCommandRunner;
import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.helpers.HBinary;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.helpers.HCollector;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.alexandria.java.io.file.CopyWalker;
import com.g2forge.alexandria.java.io.file.CopyWalker.ExtendedCopyOption;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.java.io.file.compare.CompareWalker;
import com.g2forge.alexandria.java.io.file.compare.IFileCompareGroup;
import com.g2forge.alexandria.java.io.file.compare.IFileCompareGroupFunction;
import com.g2forge.alexandria.java.io.file.compare.SHA1HashFileCompareGroupFunction;
import com.g2forge.alexandria.java.io.file.compare.TextHashFileCompareGroupFunction;
import com.g2forge.alexandria.java.platform.HPlatform;
import com.g2forge.alexandria.java.platform.PathSpec;
import com.g2forge.alexandria.java.retry.Retry;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.gearbox.command.converter.IMethodArgument;
import com.g2forge.gearbox.command.converter.dumb.ArgumentRenderer;
import com.g2forge.gearbox.command.converter.dumb.DumbCommandConverter;
import com.g2forge.gearbox.command.converter.dumb.IArgumentRenderer;
import com.g2forge.gearbox.command.converter.dumb.Named;
import com.g2forge.gearbox.command.converter.dumb.ToStringArgumentRenderer;
import com.g2forge.gearbox.command.converter.dumb.Working;
import com.g2forge.gearbox.command.process.ProcessBuilderRunner;
import com.g2forge.gearbox.command.process.redirect.IRedirect;
import com.g2forge.gearbox.command.proxy.CommandProxyFactory;
import com.g2forge.gearbox.command.proxy.ICommandProxyFactory;
import com.g2forge.gearbox.command.proxy.process.ModifyProcessInvocationException;
import com.g2forge.gearbox.image.ImageCluster;
import com.g2forge.gearbox.image.PHashImageComparator;
import com.g2forge.gearbox.image.PHashImageComparator.CharacterizedImage;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.ssg.operation.Operation;

import lombok.AccessLevel;
import lombok.Getter;
import net.sourceforge.plantuml.security.ImageIO;

public class TestJoint {
	public interface IJoint {
		public class ComponentsArgumentRenderer implements IArgumentRenderer<Set<Joint.Component>> {
			@Override
			public List<String> render(IMethodArgument<Set<Joint.Component>> argument) {
				return HCollection.asList("--components", argument.get().stream().map(Object::toString).collect(Collectors.joining(",")));
			}
		}

		public default Stream<String> joint(@Working Path pwd, Path input, Path output, @Named(value = "--operation", joined = false) @ArgumentRenderer(ToStringArgumentRenderer.class) Operation operation, @ArgumentRenderer(ComponentsArgumentRenderer.class) Set<Joint.Component> components) {
			throw new ModifyProcessInvocationException(processInvocation -> {
				final CommandInvocation<IRedirect, IRedirect> inputCommand = processInvocation.getCommandInvocation();
				final CommandInvocationBuilder<IRedirect, IRedirect> commandBuilder = inputCommand.toBuilder();
				commandBuilder.clearArguments();

				final PathSpec pathSpec = HPlatform.getPlatform().getPathSpec();

				// Add the java executable
				commandBuilder.argument(System.getProperty("java.home") + pathSpec.getFileSeparator() + "bin" + pathSpec.getFileSeparator() + "java");
				// Uncomment to enable debugging of the child process
				//commandBuilder.argument("-Xdebug");
				//commandBuilder.argument("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9000");
				// Add the classpath
				commandBuilder.argument("-cp").argument(System.getProperty("java.class.path"));

				// Add the joint class
				commandBuilder.argument(Joint.class.getName());
				// Add the joint arguments
				commandBuilder.arguments(inputCommand.getArguments().subList(1, inputCommand.getArguments().size()));

				return processInvocation.toBuilder().commandInvocation(commandBuilder.build()).build();
			});
		}
	}

	protected static final int COMPARATOR_GROUPDISTANCE = 50;

	protected static final PHashImageComparator COMPARATOR = new PHashImageComparator(16);

	protected static final IFunction1<Path, IFileCompareGroupFunction<?>> GROUPFUNCTIONFUNCTION = path -> {
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
		CompareWalker.builder().root(actual).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(expected);
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

	/**
	 * Ensure everything works with relative paths.
	 */
	@Test
	public void relative() throws Exception {
		try (final TempDirectory temp = new TempDirectory();
			final ICloseableSupplier<Path> sourcePath = new Resource(getClass(), "rewrite").getPath();) {
			final Path input = temp.get().resolve("input"), output = temp.get().resolve("output");
			Files.createDirectories(input);
			Files.createDirectories(output);
			CopyWalker.builder().options(p -> new CopyOption[] { ExtendedCopyOption.SKIP_EXISTING }).target(input).build().walkFileTree(sourcePath.get().resolve("input"));

			final ICommandProxyFactory commandProxyFactory = new CommandProxyFactory(DumbCommandConverter.create(), new ProcessBuilderRunner(IdentityCommandRunner.create()));
			final IJoint joint = commandProxyFactory.apply(IJoint.class);
			joint.joint(input, Paths.get("."), input.relativize(output), Operation.Build, HCollection.asSet(Joint.Component.StaticContent)).forEach(System.out::println);

			CompareWalker.builder().root(output).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(sourcePath.get().resolve("output"));
		}
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
			CompareWalker.builder().root(input).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(source);

			try (final ICloseable closeable = createJoint(input, output, Operation.Serve).start()) {
				new Retry(Duration.ofMillis(1000), Duration.ofMillis(50), () -> CompareWalker.builder().root(output).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(source)).run();

				// Test that new files get copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).append("Text").close();
				new Retry(Duration.ofMillis(500), Duration.ofMillis(50), () -> CompareWalker.builder().root(output).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(input)).run();

				// Test that modified files get re-copied correctly
				Files.newBufferedWriter(input.resolve("dir1/text.txt"), StandardOpenOption.APPEND, StandardOpenOption.WRITE).append("Other").close();
				new Retry(Duration.ofMillis(500), Duration.ofMillis(50), () -> CompareWalker.builder().root(output).groupFunctionFunction(GROUPFUNCTIONFUNCTION).build().walkFileTree(input)).run();
			} catch (Throwable t) {
				throw t;
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
