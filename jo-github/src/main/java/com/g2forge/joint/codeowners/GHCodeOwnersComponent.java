package com.g2forge.joint.codeowners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IConsumer1;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.alexandria.java.io.file.AMultithrowFileVisitor;
import com.g2forge.alexandria.media.IMediaRegistry;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.gearbox.git.HGit;
import com.g2forge.gearbox.github.codeowners.GHCodeOwners;
import com.g2forge.gearbox.github.codeowners.IGHCOLine;
import com.g2forge.gearbox.github.codeowners.convert.GHCORenderer;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class GHCodeOwnersComponent implements IComponent {
	@EqualsAndHashCode
	protected class GHCodeOwnerConversion implements IConversion {
		private final Map<Path, IGHCOParser> parsers;

		protected GHCodeOwnerConversion(Map<Path, IGHCOParser> parsers) {
			this.parsers = parsers;
		}

		@Override
		public Set<Path> getInputs() {
			return parsers.keySet().stream().map(relative -> getContent().resolve(relative)).collect(Collectors.toSet());
		}

		@Override
		public Set<Path> getOutputs() {
			return GHCodeOwnersComponent.this.getOutputs();
		}

		@Override
		public void invoke(IConversionContext context) {
			final GHCodeOwners.GHCodeOwnersBuilder builder = GHCodeOwners.builder();

			for (Map.Entry<Path, IGHCOParser> entry : parsers.entrySet()) {
				final Path relative = entry.getKey();
				final IGHCOParser parser = entry.getValue();
				final IGHCOLine line = parser.parse(content, relative);
				if (line != null) builder.line(line);
			}

			final GHCodeOwners codeowners = builder.build();
			try {
				Files.createDirectories(getOutput().getParent());
				try (final BufferedWriter writer = Files.newBufferedWriter(getOutput())) {
					writer.append(new GHCORenderer().render(codeowners));
				}
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Data
	@Builder(toBuilder = true)
	@RequiredArgsConstructor
	@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
	public static class Configuration {
		public static Map<IMediaType, IGHCOParser> computeDefaultTypes() {
			final HashMap<IMediaType, IGHCOParser> retVal = new HashMap<>();
			retVal.put(MediaType.Markdown, new MDFrontMatterGHCOParser());
			return retVal;
		}

		@Builder.Default
		protected final IMediaRegistry registry = ExtendedMediaType.getRegistry();

		@Builder.Default
		protected final Map<IMediaType, IGHCOParser> types = computeDefaultTypes();
	}

	public interface IGHCOParser {
		public IGHCOLine parse(Path root, Path relative);
	}

	protected final Path content;

	@Builder.Default
	protected final Configuration configuration = Configuration.builder().build();

	public GHCodeOwnersComponent(Path content) {
		this(content, Configuration.builder().build());
	}

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getContent());
	}

	protected Path getOutput() {
		return getRoot().resolve(GHCodeOwners.GITHUB_CODEOWNERS);
	}

	@Override
	public Set<Path> getOutputs() {
		return HCollection.asSet(getOutput());
	}

	protected Path getRoot() {
		Path current = getContent();
		while (!Files.isDirectory(HGit.getGitFile(current))) {
			current = current.getParent();
			if (current == null) throw new IllegalArgumentException(String.format("\"%1$s\" does not appear to be inside a git repository", getContent()));
		}
		return current;
	}

	@Override
	public void map(IConsumer1<? super IConversion> consumer) {
		final Map<Path, IGHCOParser> parsers = new LinkedHashMap<>();
		try {
			final Path content = getContent();
			Files.walkFileTree(content, new AMultithrowFileVisitor() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
					final FileVisitResult retVal = super.visitFile(path, attributes);
					final Configuration configuration = getConfiguration();
					final IGHCOParser parser = configuration.getTypes().get(configuration.getRegistry().computeMediaType(path));
					if (parser != null) parsers.put(content.relativize(path), parser);
					return retVal;
				}
			});
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		consumer.accept(new GHCodeOwnerConversion(parsers));
	}
}
