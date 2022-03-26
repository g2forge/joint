package com.g2forge.joint.md;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.Filename;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.joint.core.ExtendedMediaType;

import lombok.AccessLevel;
import lombok.Getter;

public class TestMD2HTMLConversionType {
	@Getter(lazy = true)
	private static final IFunction1<Path, Path> conversion = path -> {
		final IMediaType inputMediaType = ExtendedMediaType.getRegistry().computeMediaType(path), outputMediaType;
		if (inputMediaType == MediaType.Markdown) outputMediaType = MediaType.HTML;
		else if (inputMediaType == ExtendedMediaType.PlantUML) outputMediaType = MediaType.PNG;
		else outputMediaType = inputMediaType;
		return (outputMediaType == null) ? path : Filename.replaceLastExtension(path, outputMediaType.getFileExtensions().getDefaultExtension());
	};

	@Rule
	@Getter(AccessLevel.PROTECTED)
	public final TestName name = new TestName();

	@Test
	public void conversionDocument() throws IOException {
		testLink("link.html", "link.md");
	}

	@Test
	public void conversionImage() throws IOException {
		testLink("image.png", "image.puml");
	}

	@Test
	public void conversionNoExtension() throws IOException {
		testLink("child", "child");
	}

	@Test
	public void conversionNone() throws IOException {
		testLink("link.html", "link.html");
	}

	@Test
	public void escapeAbsolute() throws IOException {
		try {
			testLink("/../BadLink.html", "/../BadLink.md");
		} catch (Throwable throwable) {
			while (true) {
				final Throwable cause = throwable.getCause();
				if (cause == null) break;
				throwable = cause;
			}
			HAssert.assertInstanceOf(IllegalArgumentException.class, throwable);
			return;
		}
		HAssert.fail("Exception should have been thrown");
	}

	@Test
	public void escapeNone() throws IOException {
		testLink("File Name.html", "File Name.md");
	}

	@Test
	public void escapePercent() throws IOException {
		testLink("File Name.html", "File%20Name.md");
	}

	@Test
	public void escapePlus() throws IOException {
		testLink("File Name.html", "File+Name.md");
	}

	@Test
	public void escapeRelative() throws IOException {
		try {
			testLink("../../../../BadLink.md", "../../../../BadLink.md");
		} catch (Throwable throwable) {
			while (true) {
				final Throwable cause = throwable.getCause();
				if (cause == null) break;
				throwable = cause;
			}
			HAssert.assertInstanceOf(IllegalArgumentException.class, throwable);
			return;
		}
		HAssert.fail("Exception should have been thrown");
	}

	@Test
	public void relativeDirFile() throws IOException {
		testLink("directory/File.html", "directory/File.md");
	}

	@Test
	public void relativeFile() throws IOException {
		testLink("File.html", "File.md");
	}

	@Test
	public void relativeParentDirFile() throws IOException {
		testLink("../Directory/File.html", "../Directory/File.md");
	}

	@Test
	public void relativeParentFile() throws IOException {
		testLink("../Other.html", "../Other.md");
	}

	@Test
	public void relativeSelfDirFile() throws IOException {
		testLink("Directory/File.html", "./Directory/File.md");
	}

	@Test
	public void relativeSelfFile() throws IOException {
		testLink("File.html", "./File.md");
	}

	@Test
	public void rootFile() throws IOException {
		testLink("/File.html", "/File.md");
	}

	@Test
	public void suffixBoth() throws IOException {
		testLink("?a=b#fragment", "?a=b#fragment");
	}

	@Test
	public void suffixFragmentOnly() throws IOException {
		testLink("#fragment", "#fragment");
	}

	@Test
	public void suffixQueryOnly() throws IOException {
		testLink("?a=b", "?a=b");
	}

	protected void test(final String expectedLink, final IFunction1<Path, Path> conversion, final String inputRelative, final String outputRelative, final String inputLink) throws IOException {
		try (final FileSystem fs = FileSystems.newFileSystem(URI.create("memory:" + getClass().getSimpleName() + "_" + getName().getMethodName()), null)) {
			final Path inputRoot = fs.getPath("/input");
			final Path outputRoot = fs.getPath("/output");
			final Path input = inputRoot.resolve(inputRelative);
			final Path output = outputRoot.resolve(outputRelative);
			Files.createDirectories(input.getParent());
			Files.createDirectories(output.getParent());

			final MD2HTMLConversionType.Translator translator = new MD2HTMLConversionType.Translator(path -> {
				Path toTranslate = path;
				if (path.startsWith(inputRoot)) toTranslate = outputRoot.resolve(inputRoot.relativize(path));
				return conversion.apply(toTranslate);
			}, inputRoot, input, outputRoot, output);
			final String actualLink = translator.apply(inputLink);
			HAssert.assertEquals(expectedLink, actualLink);
		}
	}

	protected void testLink(final String expected, final String input) throws IOException {
		final String document = "d0/d1/document.html";
		test(expected, getConversion(), document, document, input);
	}
}
