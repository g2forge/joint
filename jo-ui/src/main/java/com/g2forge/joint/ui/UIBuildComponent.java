package com.g2forge.joint.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.enums.HEnum;
import com.g2forge.alexandria.java.core.helpers.HBinary;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.function.IConsumer1;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.alexandria.java.io.HTextIO;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.gearbox.command.converter.IMethodArgument;
import com.g2forge.gearbox.command.converter.dumb.ArgumentRenderer;
import com.g2forge.gearbox.command.converter.dumb.Command;
import com.g2forge.gearbox.command.converter.dumb.Constant;
import com.g2forge.gearbox.command.converter.dumb.DumbCommandConverter;
import com.g2forge.gearbox.command.converter.dumb.EnvPath;
import com.g2forge.gearbox.command.converter.dumb.HDumbCommandConverter;
import com.g2forge.gearbox.command.converter.dumb.IArgumentRenderer;
import com.g2forge.gearbox.command.converter.dumb.Named;
import com.g2forge.gearbox.command.converter.dumb.Working;
import com.g2forge.gearbox.command.process.ProcessBuilderRunner;
import com.g2forge.gearbox.command.proxy.CommandProxyFactory;
import com.g2forge.gearbox.command.proxy.ICommandProxyFactory;
import com.g2forge.gearbox.command.proxy.method.ICommandInterface;
import com.g2forge.gearbox.command.proxy.result.StreamConsumer;
import com.g2forge.gearbox.maven.IMaven;
import com.g2forge.joint.core.CloseableConversion;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.CopyConversionType;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.core.copy.IFileConversionType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@Slf4j
public class UIBuildComponent implements IComponent {
	@EqualsAndHashCode
	protected class AngularBuildConversion implements IConversion {
		@Override
		public Set<Path> getInputs() {
			return HCollection.asSet(getWorking());
		}

		@Override
		public Set<Path> getOutputs() {
			return getOutput() == null ? HCollection.emptySet() : HCollection.asSet(getOutput());
		}

		@Override
		public void invoke(IConversionContext context) {
			final Path node = getWorking().resolve("node");
			if (isInitialize()) {
				final Path init = node.resolve("init");
				boolean initialize = !Files.isRegularFile(init);
				if (!initialize) {
					final String actual = HBinary.toHex(HIO.sha1(getWorking().resolve("package.json"), Files::newInputStream)).trim().toUpperCase(), expected;
					try (final InputStream stream = Files.newInputStream(init)) {
						expected = HCollection.getFirst(HTextIO.readAll(stream)).trim().toUpperCase();
					} catch (IOException exception) {
						throw new RuntimeIOException(exception);
					}
					initialize = !actual.equals(expected);
				}
				if (initialize) getFactory().apply(IMaven.class).maven(getWorking(), Paths.get("./mvnw"), true, "initialize", HCollection.asList("ui-build")).forEach(log::info);
			}
			final Path npm = node.resolve("npm");
			final IAngular angular = getFactory().apply(IAngular.class);
			switch (context.getMode()) {
				default:
					if (getOutput() == null) throw new NullPointerException();
					final String baseHref = getBaseHref();
					if ((baseHref != null) && (!baseHref.startsWith("/") || !baseHref.endsWith("/"))) log.warn("Angular Base HREF should probably start and end with a \"/\", but is \"{}\"!", baseHref);
					angular.build(getWorking(), node, npm, getOutput(), baseHref).forEach(log::info);

					// Note that we move the maps if maps are DISABLED, hence the ! in the condition
					if (!maps) angular.maps(getWorking(), node, npm, getOutput()).forEach(log::info);

					break;
				case ServeBuild:
					context.register(new StreamConsumer(angular.serve(getWorking(), node, npm), log::info).open().asCloseableWithWait());
					break;
				case ServeRebuild:
					break;
			}
		}
	}

	public static interface IAngular extends ICommandInterface {
		public class NonNullArgumentRenderer implements IArgumentRenderer<String> {
			@Override
			public List<String> render(IMethodArgument<String> argument) {
				final String value = argument.get();
				if (value == null) return HCollection.emptyList();
				return HDumbCommandConverter.computeString(argument, value);
			}
		}

		@Command({})
		public Stream<String> build(@Working Path working, @EnvPath Path node, @Constant({ "run", "build", "--", "--prod" }) Path npm, @Named(value = "--output-path", joined = false) Path output, @Named(value = "--base-href", joined = false) @ArgumentRenderer(NonNullArgumentRenderer.class) String baseHref);

		@Command({})
		public Stream<String> maps(@Working Path working, @EnvPath Path node, @Constant({ "run", "maps" }) Path npm, Path output);

		@Command({})
		public Stream<String> serve(@Working Path working, @EnvPath Path node, @Constant({ "run", "serve" }) Path npm);
	}

	public static class NonServeFileConversion extends FileConversion {
		protected NonServeFileConversion(Path inputRoot, Path inputRelative, Path outputRoot, IFileConversionType type) {
			super(inputRoot, inputRelative, outputRoot, type);
		}

		@Override
		public void invoke(IConversionContext context) {
			switch (context.getMode()) {
				case ServeBuild:
				case ServeRebuild:
					// Don't bother building the 404 handlers when we're in serve mode, since the server doesn't use them *anyway*
					return;
				default:
			}
			super.invoke(context);
		}
	}

	public enum NotFoundHandler {
		CopyIndex {
			@Override
			protected void map(UIBuildComponent component, IConsumer1<? super IConversion> consumer) {
				final Path output = component.getOutput();
				if (output != null) consumer.accept(new NonServeFileConversion(output, Paths.get("index.html"), output, new CopyConversionType() {
					@Override
					public Path computeOutputRelative(FileConversion conversion) {
						return Paths.get("404.html");
					}
				}));
			}
		},
		IISURLRewrite {
			@Override
			protected void map(UIBuildComponent component, IConsumer1<? super IConversion> consumer) {
				final Class<NotFoundHandler> enumClass = HEnum.getEnumClass(this);
				final ICloseableSupplier<Path> input = new Resource(enumClass, name() + enumClass.getSimpleName() + ".xml").getPath();
				try {
					final Path output = component.getOutput().resolve("web.config");
					consumer.accept(new CloseableConversion(new NonServeFileConversion(input.get(), null, output, STConversionType.builder().startDelimiter('$').endDelimiter('$').property("base-href", component.getBaseHref()).build()), input));
				} catch (Throwable throwable) {
					input.close();
					throw throwable;
				}
			}
		};

		protected abstract void map(UIBuildComponent component, IConsumer1<? super IConversion> consumer);
	}

	@Builder.Default
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	protected final ICommandProxyFactory factory = new CommandProxyFactory(DumbCommandConverter.create(), new ProcessBuilderRunner());

	protected final Path working;

	protected final Path output;

	protected final boolean initialize;

	protected final boolean maps;

	@Builder.Default
	protected final String baseHref = null;

	@Builder.Default
	protected final Set<NotFoundHandler> notFoundHandlers = EnumSet.noneOf(NotFoundHandler.class);

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getWorking());
	}

	@Override
	public Set<Path> getOutputs() {
		return getOutput() == null ? HCollection.emptySet() : HCollection.asSet(getOutput());
	}

	@Override
	public void map(IConsumer1<? super IConversion> consumer) {
		consumer.accept(new AngularBuildConversion());
		for (NotFoundHandler handler : getNotFoundHandlers()) {
			handler.map(this, consumer);
		}
	}
}
