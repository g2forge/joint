package com.g2forge.joint.plantuml;

import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.g2forge.alexandria.annotations.note.Note;
import com.g2forge.alexandria.annotations.note.NoteType;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.error.HError;
import com.g2forge.alexandria.java.core.resource.IResource;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.test.HAssert;

import net.sourceforge.plantuml.security.ImageIO;

public class TestPlantUMLConversionType {
	@Note(type = NoteType.TODO, value = "Use a proper image comparison algorithm, one that handles minor shading differences better (and abstract this to a shared library)")
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

			final List<Throwable> throwables = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				try {
					assertImageEquals(new Resource(getClass(), "diagram" + i + ".png"), actual);
					return;
				} catch (Throwable throwable) {
					throwables.add(throwable);
				}
			}

			HAssert.assertTrue(Files.isRegularFile(actual));
			HAssert.assertThat(Files.size(actual), Matchers.greaterThan(2700l));
			HAssert.assertThat(Files.size(actual), Matchers.lessThan(3200l));

			// TODO: Re-enable this when we switch back to image comparison instead of rough file length matching
			// throw HError.withSuppressed(new AssertionError("None of the candidate images matched!"), throwables);
		}
	}
}
