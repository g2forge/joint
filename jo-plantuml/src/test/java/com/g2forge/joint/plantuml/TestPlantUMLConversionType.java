package com.g2forge.joint.plantuml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.error.HError;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.gearbox.image.PHashImageComparator;
import com.g2forge.gearbox.image.PHashImageComparator.CharacterizedImage;

public class TestPlantUMLConversionType {
	protected static final PHashImageComparator comparator = new PHashImageComparator(16);

	public static void assertImageEquals(Path expected, Path actual) {
		final CharacterizedImage e = comparator.characterize(expected);
		final CharacterizedImage a = comparator.characterize(actual);
		HAssert.assertThat(comparator.distance(e, a), Matchers.lessThan(50));
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
				try (final ICloseableSupplier<Path> closeable = new Resource(getClass(), "diagram" + i + ".png").getPath()) {
					assertImageEquals(closeable.get(), actual);
					return;
				} catch (Throwable throwable) {
					throwables.add(throwable);
				}
			}

			throw HError.withSuppressed(new AssertionError("None of the candidate images matched!"), throwables);
		}
	}
}
