package com.g2forge.joint.core.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.g2forge.alexandria.java.core.marker.ISingleton;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.joint.core.IConversionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateDirectoryConversionType implements IFileConversionType, ISingleton {
	protected static final CreateDirectoryConversionType INSTANCE = new CreateDirectoryConversionType();

	public static CreateDirectoryConversionType create() {
		return INSTANCE;
	}

	protected CreateDirectoryConversionType() {}

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		return conversion.getInputRelative();
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		try {
			Files.createDirectories(output);
		} catch (IOException e) {
			throw new RuntimeIOException(String.format("Failed to create %1$s", output), e);
		}
		log.debug("Created {}", output);
	}
}
