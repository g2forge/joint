package com.g2forge.joint.plantuml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.g2forge.alexandria.java.core.helpers.HStream;
import com.g2forge.alexandria.java.core.marker.ISingleton;
import com.g2forge.alexandria.java.io.Filename;
import com.g2forge.alexandria.java.io.HTextIO;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.core.copy.IFileConversionType;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

@Slf4j
public class PlantUMLConversionType implements IFileConversionType, ISingleton {
	protected static final PlantUMLConversionType INSTANCE = new PlantUMLConversionType();

	public static PlantUMLConversionType create() {
		return INSTANCE;
	}

	protected PlantUMLConversionType() {}

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		final Path inputRelative = conversion.getInputRelative();
		final Filename filename = new Filename(inputRelative);
		if (filename.getComponents().size() > 2) return Filename.modifyFilename(inputRelative, filename.getFullName());
		else return Filename.modifyFilename(inputRelative, filename.getFirstName() + FileFormat.PNG.getFileSuffix());
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		final String extension = "." + new Filename(output).getLastExtension();
		final FileFormat format = HStream.findOne(Stream.of(FileFormat.values()).filter(f -> f.getFileSuffix().toLowerCase().equals(extension)));

		if (!Files.isRegularFile(output) || (Files.getLastModifiedTime(output).compareTo(Files.getLastModifiedTime(input)) <= 0)) {
			log.debug("Running PlantUML to convert {} to {}", input, output);
			try (final InputStream inputStream = Files.newInputStream(input)) {
				final SourceStringReader reader = new SourceStringReader(HTextIO.readAll(inputStream, false));
				try (final OutputStream outputStream = Files.newOutputStream(output)) {
					reader.outputImage(outputStream, new FileFormatOption(format));
				}
			}
			log.info("Converted {} to {}", input, output);
		} else log.info("Skipping conversion of {} to {} based on timestamps", input, output);
	}
}