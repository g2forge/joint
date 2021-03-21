package com.g2forge.joint.core;

import com.g2forge.alexandria.media.CompositeMediaRegistry;
import com.g2forge.alexandria.media.IFileExtensions;
import com.g2forge.alexandria.media.IMediaRegistry;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaRegistry;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.media.SimpleFileExtensions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExtendedMediaType implements IMediaType {
	PlantUML(true, new SimpleFileExtensions("puml"));

	@Getter(lazy = true)
	private static final IMediaRegistry registry = computeRegistry();

	protected static IMediaRegistry computeRegistry() {
		return new CompositeMediaRegistry(MediaType.getRegistry(), new MediaRegistry(ExtendedMediaType.values()));
	}

	protected final boolean text;

	protected final IFileExtensions fileExtensions;
}
