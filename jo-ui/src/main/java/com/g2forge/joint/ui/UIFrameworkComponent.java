package com.g2forge.joint.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import com.g2forge.alexandria.adt.associative.map.MapBuilder;
import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.function.IConsumer1;
import com.g2forge.alexandria.java.io.dataaccess.ResourceDataSource;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.copy.CopyComponent;
import com.g2forge.joint.core.copy.CopyConversionType;
import com.g2forge.joint.core.copy.CreateDirectoryConversionType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class UIFrameworkComponent implements IComponent, ICloseable {
	@Getter(AccessLevel.PROTECTED)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	protected final ICloseableSupplier<Path> input = new Resource(UIFrameworkComponent.class, "/input").getPath();

	protected final String name;

	protected final Path output;

	@Override
	public void close() {
		getInput().close();
	}

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getInput().get());
	}

	@Override
	public Set<Path> getOutputs() {
		return HCollection.asSet(getOutput());
	}

	@Override
	public void map(IConsumer1<? super IConversion> consumer) {
		final Path input = getInput().get();
		final Path output = getOutput();
		CopyComponent.map(input, output, null, entry -> {
			// Don't copy the root directory
			if (entry.isRoot()) return CopyComponent.Operation.builder().conversionType(CreateDirectoryConversionType.create()).build();

			// Rewrite the pom.xml to be more portable
			if (CopyComponent.Entry.Type.File.equals(entry.getType())) {
				final Path relative = entry.getRelative();
				if (relative.equals(relative.getFileSystem().getPath("pom.xml"))) return CopyComponent.Operation.builder().conversionType(new XSLConversionType(new ResourceDataSource(new Resource(getClass(), "pom-transform.xsl")), new MapBuilder<String, Object>().put("name", getName()).build())).build();
				if (relative.equals(relative.getFileSystem().getPath("src/assets/.gitignore"))) return CopyComponent.Operation.createIgnore();
				if (relative.equals(relative.getFileSystem().getPath("mvnw")) || relative.equals(relative.getFileSystem().getPath("mvnw.cmd"))) return CopyComponent.Operation.builder().conversionType(new CopyConversionType() {
					@Override
					protected void copy(Path input, Path output, final CopyOption[] options) throws IOException {
						try (final BufferedReader reader = Files.newBufferedReader(input);
							final BufferedWriter writer = Files.newBufferedWriter(output)) {
							while (true) {
								final String line = reader.readLine();
								if (line == null) break;
								writer.append(line).append(System.lineSeparator());
							}
						}

						if (Files.isRegularFile(output)) {
							final PosixFileAttributeView attributes = Files.getFileAttributeView(output, PosixFileAttributeView.class);
							if (attributes != null) attributes.setPermissions(HCollection.union(attributes.readAttributes().permissions(), HCollection.asSet(PosixFilePermission.OWNER_EXECUTE)));
						}
					}
				}).build();
			}

			return CopyComponent.Operation.builder().build();
		}, consumer);
	}
}
