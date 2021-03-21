package com.g2forge.joint.ssg.staticcontent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.g2forge.alexandria.annotations.note.Note;
import com.g2forge.alexandria.annotations.note.NoteType;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IConsumer1;
import com.g2forge.alexandria.java.io.Filename;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.copy.CopyComponent;
import com.g2forge.joint.core.copy.CopyComponent.Operation.OperationBuilder;
import com.g2forge.joint.core.copy.CopyConversionType;
import com.g2forge.joint.core.copy.CreateDirectoryConversionType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class StaticContentComponent implements IComponent {
	@Data
	@Builder(toBuilder = true)
	@RequiredArgsConstructor
	protected static class Context {
		protected final Context parent;

		protected final Path path;

		protected final List<PathMatcher> includes;

		protected final List<PathMatcher> excludes;

		public boolean isIncluded(Path path) {
			final Path relative = getPath().relativize(path);
			if ((getExcludes() != null) && getExcludes().stream().filter(m -> m.matches(relative)).findAny().isPresent()) return false;
			if ((getIncludes() == null) || getIncludes().isEmpty()) return true;
			return getIncludes().stream().filter(m -> m.matches(relative)).findAny().isPresent();
		}
	}

	protected static final String JOINT_JSON = "joint.json";

	protected final Path input;

	protected final Path output;

	protected final Path working;

	@Builder.Default
	@EqualsAndHashCode.Include
	protected final SystemConfiguration configuration = SystemConfiguration.builder().build();

	public StaticContentComponent(Path input, Path output, Path working) {
		this(input, output, working, SystemConfiguration.builder().build());
	}

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getInput());
	}

	@Override
	public Set<Path> getOutputs() {
		return HCollection.asSet(getOutput());
	}

	@Note(type = NoteType.TODO, value = "Unit tests for directory config include/exclude would be nice")
	@Override
	public void map(IConsumer1<? super IConversion> consumer) {
		CopyComponent.<Context>map(getInput(), getOutput(), getWorking().toAbsolutePath(), entry -> {
			if ((entry.getContext() != null) && !entry.getContext().isIncluded(entry.getPath())) return CopyComponent.Operation.createIgnore();

			final OperationBuilder<Context> retVal = CopyComponent.Operation.builder();
			switch (entry.getType()) {
				case Directory:
					// Copy is the default operation

					// Create a context if there's a config file or this is the root directory, otherwise the parent context is fine
					final Path configPath = entry.getPath().resolve(JOINT_JSON);
					if (Files.isRegularFile(configPath)) {
						final ObjectMapper mapper = new ObjectMapper();
						final ObjectNode joint;
						try {
							joint = mapper.readValue(configPath.toFile(), ObjectNode.class);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
						final JsonNode json = joint.get("StaticContent");
						final DirectoryConfiguration configuration = mapper.convertValue(json, DirectoryConfiguration.class);

						final Context.ContextBuilder context = Context.builder().parent(entry.getContext()).path(entry.getPath());
						final FileSystem filesystem = entry.getPath().getFileSystem();
						if (configuration.getInclude() != null) context.includes(configuration.getInclude().stream().map(filesystem::getPathMatcher).collect(Collectors.toList()));
						if (configuration.getExclude() != null) context.excludes(configuration.getExclude().stream().map(filesystem::getPathMatcher).collect(Collectors.toList()));
						retVal.context(context.build());
					} else if (entry.isRoot()) retVal.context(new Context(null, entry.getPath(), null, null));
					else retVal.context(entry.getContext());

					// Create the root output directory
					if (entry.isRoot()) retVal.conversionType(CreateDirectoryConversionType.create());

					break;
				default:
					final IMediaType mediaType = getConfiguration().getRegistry().computeMediaType(new Filename(entry.getRelative()));
					retVal.conversionType(getConfiguration().getTypes().getOrDefault(mediaType, CopyConversionType.create()));
					break;
			}
			return retVal.build();
		}, consumer);
	}
}
