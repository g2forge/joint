package com.g2forge.joint.core;

import java.nio.file.Path;
import java.util.Set;

public interface IConversionContext {
	public enum Mode {
		Build,
		ServeBuild,
		ServeRebuild;
	}

	public Mode getMode();

	public Set<IConversion> getConversions(Path input);

	public void register(AutoCloseable closeable);
}
