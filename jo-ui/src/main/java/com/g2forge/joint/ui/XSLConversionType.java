package com.g2forge.joint.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.g2forge.alexandria.java.io.dataaccess.IDataSource;
import com.g2forge.alexandria.java.type.ref.ITypeRef;
import com.g2forge.joint.core.IConversionContext;
import com.g2forge.joint.core.copy.FileConversion;
import com.g2forge.joint.core.copy.IFileConversionType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class XSLConversionType implements IFileConversionType {
	@Getter(lazy = true, value = AccessLevel.PROTECTED)
	private static final DocumentBuilderFactory documentBuilderFactory = computeDocumentBuilderFactory();

	@Getter(lazy = true, value = AccessLevel.PROTECTED)
	private static final TransformerFactory transformFactory = computeTransformerFactory();

	protected static DocumentBuilderFactory computeDocumentBuilderFactory() {
		return DocumentBuilderFactory.newInstance();
	}

	protected static TransformerFactory computeTransformerFactory() {
		return TransformerFactory.newInstance();
	}

	protected final IDataSource xsl;

	@Singular
	protected final Map<String, Object> parameters;

	@Override
	public Path computeOutputRelative(FileConversion conversion) {
		return conversion.getInputRelative();
	}

	@Override
	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException {
		try {

			final Document document = createDocumentBuilder().parse(Files.newInputStream(input));
			final Transformer transformer = createTransformer();
			if (getParameters() != null) for (Map.Entry<String, Object> entry : getParameters().entrySet()) {
				transformer.setParameter(entry.getKey(), entry.getValue());
			}

			final DOMSource source = new DOMSource(document);
			final StreamResult result = new StreamResult(output.toFile());
			transformer.transform(source, result);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	protected DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
		return getDocumentBuilderFactory().newDocumentBuilder();
	}

	protected Transformer createTransformer() throws TransformerConfigurationException {
		return getTransformFactory().newTransformer(new StreamSource(getXsl().getStream(ITypeRef.of(InputStream.class))));
	}
}
