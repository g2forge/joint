package com.g2forge.joint.core.copy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.io.HPath;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class FileConversion implements IConversion {
	protected final Path inputRoot;

	protected final Path inputRelative;

	protected final Path outputRoot;

	protected final IFileConversionType type;

	protected Path getInput() {
		final Path inputRelative = getInputRelative();
		if (inputRelative == null) return getInputRoot();
		return getInputRoot().resolve(inputRelative);
	}

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getInput());
	}

	protected Path getOutput() {
		final Path outputRelative = getType().computeOutputRelative(this);
		if (outputRelative == null) return getOutputRoot();
		return HPath.resolveFS(getOutputRoot(), outputRelative);
	}

	@Override
	public Set<Path> getOutputs() {
		return HCollection.asSet(getOutput());
	}

	@Override
	public void invoke(IConversionContext context) {
		final Path input = getInput();
		if (!Files.exists(input)) return;

		final Path output;
		try {
			output = getOutput();
		} catch (Throwable throwable) {
			throw new RuntimeException(String.format("Failed to determine output file for %1$s", input), throwable);
		}

		try {
			getType().convert(context, this, input, output);
		} catch (Throwable throwable) {
			throw new RuntimeException(String.format("Failed to convert %1$s to %2$s", input, output), throwable);
		}
	}
}