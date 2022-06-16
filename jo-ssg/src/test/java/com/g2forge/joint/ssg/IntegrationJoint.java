package com.g2forge.joint.ssg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.error.UnreachableCodeError;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.file.CopyWalker;
import com.g2forge.alexandria.java.io.file.TempDirectory;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.gearbox.command.process.IProcess;
import com.g2forge.gearbox.command.proxy.ICommandProxyFactory;
import com.g2forge.gearbox.git.HGit;
import com.g2forge.gearbox.github.codeowners.GHCodeOwners;
import com.g2forge.gearbox.maven.IMaven;
import com.g2forge.gearbox.maven.MavenCoordinates;
import com.g2forge.joint.ui.UIBuildComponent.IAngular;
import com.g2forge.joint.ui.UIFrameworkComponent;

public class IntegrationJoint {
	@Test
	public void test() throws Exception, IOException {
		final String name = "integration";

		try (final TempDirectory temp = new TempDirectory();
			final ICloseableSupplier<Path> sourcePath = new Resource(getClass(), name + "/input").getPath();) {
			final Path input = temp.get().resolve("input"), output = temp.get().resolve("output");
			Files.createDirectories(output);
			final Path source = sourcePath.get();
			CopyWalker.builder().target(input).build().walkFileTree(source);
			Files.createDirectories(input.resolve(HGit.GIT_DIRECTORY));

			HAssert.assertEquals(Integer.valueOf(0), Joint.builder().input(input).output(output).components(EnumSet.allOf(Joint.Component.class)).commandProxyFactory(new ICommandProxyFactory() {
				@Override
				public <_T> _T apply(Class<_T> type) {
					if (IMaven.class.equals(type)) {
						final IMaven maven = new IMaven() {
							@Override
							public IProcess dependencyCopy(Path path, boolean batch, MavenCoordinates artifact, Path outputDirectory) {
								HAssert.fail();
								throw new UnreachableCodeError();
							}

							@Override
							public Stream<String> effectivePOM(Path path, boolean batch, Path output) {
								HAssert.fail();
								throw new UnreachableCodeError();
							}

							@Override
							public Stream<String> maven(Path path, Path maven, boolean batch, String goal, List<String> profiles) {
								return Stream.of("Maven output");
							}
						};
						@SuppressWarnings("unchecked")
						final _T retVal = (_T) maven;
						return retVal;
					}
					if (IAngular.class.equals(type)) {
						final IAngular angular = new IAngular() {
							@Override
							public Stream<String> build(Path working, Path node, Path npm, Path output, String baseHref) {
								return Stream.of("Angular output");
							}

							@Override
							public Stream<String> maps(Path working, Path node, Path npm, Path output) {
								return Stream.of("Maps output");
							}

							@Override
							public Stream<String> serve(Path working, Path node, Path npm, Integer port) {
								HAssert.fail();
								throw new UnreachableCodeError();
							}
						};
						@SuppressWarnings("unchecked")
						final _T retVal = (_T) angular;
						return retVal;
					}
					HAssert.fail();
					throw new UnreachableCodeError();
				}
			}).build().call());

			HAssert.assertEquals(new Resource(getClass(), name + "/output/CODEOWNERS"), input.resolve(GHCodeOwners.GITHUB_CODEOWNERS));
			HAssert.assertEquals(new Resource(getClass(), name + "/output/index.html"), input.resolve(Joint.JOINT).resolve("src/assets/wiki/index.html"));
			HAssert.assertEquals(new Resource(UIFrameworkComponent.class, "/input/angular.json"), input.resolve(Joint.JOINT).resolve("angular.json"));
		}
	}
}
