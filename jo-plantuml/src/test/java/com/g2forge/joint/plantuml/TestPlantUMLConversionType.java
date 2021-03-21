package com.g2forge.joint.plantuml;

import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.resource.IResource;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.test.HAssert;

import net.sourceforge.plantuml.security.ImageIO;

public class TestPlantUMLConversionType {
	public static void assertImageEquals(IResource expected, Path actual) {
		try {
			byte[] expectedBytes = ((DataBufferByte) ImageIO.read(expected.getResourceAsStream(true)).getData().getDataBuffer()).getData();
			final byte[] actualBytes = ((DataBufferByte) ImageIO.read(Files.newInputStream(actual)).getData().getDataBuffer()).getData();
			HAssert.assertArrayEquals(expectedBytes, actualBytes);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	public void test() throws IOException {
		try (final TempDirectory temp = new TempDirectory()) {
			final Path actual = temp.get().resolve("diagram.png");
			try (final ICloseableSupplier<Path> resourcePath = new Resource(getClass(), "diagram.puml").getPath()) {
				PlantUMLConversionType.create().convert(null, null, resourcePath.get(), actual);
			}
			assertImageEquals(new Resource(getClass(), "diagram.png"), actual);
		}
	}
}
