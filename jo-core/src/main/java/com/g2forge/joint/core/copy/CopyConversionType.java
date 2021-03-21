package com.g2forge.joint.core.copy;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import com.g2forge.alexandria.java.core.marker.ISingleton;
import com.g2forge.joint.core.IConversionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CopyConversionType implements IFileConversionType, ISingleton {
	protected static final CopyConversionType INSTANCE = new CopyConversionType();

	public static CopyConversionType create() {
		return INSTANCE;
	}

	protected CopyConversionType() {}

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		return conversion.getInputRelative();
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		final CopyOption[] options = new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING };
		// Handle files
		if (!Files.isDirectory(input)) copy(input, output, options);
		else {
			// Handle directories which don't already exist at the output
			if (!Files.exists(output)) copy(input, output, options);
			else {
				// Handle directories which already exist at the output differently based on whether they have children
				if (Files.list(output).findFirst().isPresent()) {
					final BasicFileAttributes inputAttributes = Files.readAttributes(input, BasicFileAttributes.class);
					final BasicFileAttributeView outputAttributes = Files.getFileAttributeView(output, BasicFileAttributeView.class);
					outputAttributes.setTimes(inputAttributes.lastModifiedTime(), inputAttributes.lastAccessTime(), inputAttributes.creationTime());
					log.debug("Copied attributes from {} to {}", input, output);
				} else copy(input, output, options);
			}
		}
	}

	protected void copy(Path input, Path output, final CopyOption[] options) throws IOException {
		Files.copy(input, output, options);
		log.debug("Copied {} to {}", input, output);
	}
}
