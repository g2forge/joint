package com.g2forge.joint.codeowners;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.file.HFile;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.gearbox.git.HGit;
import com.g2forge.gearbox.github.codeowners.GHCodeOwners;
import com.g2forge.habitat.trace.HTrace;
import com.g2forge.joint.core.IConversion;

public class TestGHCodeOwnersComponent {
	@Test
	public void test() throws IOException {
		final String testName = HTrace.getCaller().getName();
		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + testName), null)) {
			final Path root = fs.getPath(testName);
			Files.createDirectories(root.resolve(HGit.GIT_DIRECTORY));
			for (int i = 1; i < 5; i++) {
				final String filename = "file" + i + ".md";
				try (final ICloseableSupplier<Path> resourcePath = new Resource(getClass(), filename).getPath()) {
					HFile.copy(resourcePath.get(), root.resolve(filename));
				}
			}
			final List<IConversion> conversions = new ArrayList<>();
			new GHCodeOwnersComponent(root).map(conversions::add);
			conversions.forEach(conversion -> conversion.invoke(null));

			HAssert.assertEquals(new Resource(getClass(), "CODEOWNERS"), root.resolve(GHCodeOwners.GITHUB_CODEOWNERS));
		}
	}
}
