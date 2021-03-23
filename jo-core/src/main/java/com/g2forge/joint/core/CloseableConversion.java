package com.g2forge.joint.core;

import java.nio.file.Path;
import java.util.Set;

import com.g2forge.alexandria.java.close.ICloseable;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class CloseableConversion implements IConversion {
	/** The conversion to nest inside this one. */
	protected final IConversion conversion;

	/** The closeable to call when this conversion is no longer relevant. */
	protected final ICloseable closeable;

	@Override
	public Set<Path> getInputs() {
		return getConversion().getInputs();
	}

	@Override
	public Set<Path> getOutputs() {
		return getConversion().getOutputs();
	}

	@Override
	public void invoke(IConversionContext context) {
		final ICloseable closeable = getCloseable();
		if (closeable != null) context.register(closeable);

		getConversion().invoke(context);
	}
}
