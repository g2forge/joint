package com.g2forge.joint.ui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.stringtemplate.v4.ST;

import com.g2forge.alexandria.java.io.HTextIO;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.core.copy.IFileConversionType;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@Slf4j
public class STConversionType implements IFileConversionType {
	@Builder.Default
	protected final char startDelimiter = '<';

	@Builder.Default
	protected final char endDelimiter = '>';

	@Singular
	protected final Map<String, Object> properties;

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		return conversion.getInputRelative();
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		log.debug("Processing StringTemplate");

		final String template;
		try (final InputStream inputStream = Files.newInputStream(input)) {
			template = HTextIO.readAll(inputStream, true);
		}

		final ST st = new ST(template, getStartDelimiter(), getEndDelimiter());
		for (Map.Entry<String, Object> property : getProperties().entrySet()) {
			st.add(property.getKey(), property.getValue());
		}

		try (final BufferedWriter writer = Files.newBufferedWriter(output)) {
			writer.write(st.render());
		}

		log.debug("Converted {} to {}", input, output);
	}
}