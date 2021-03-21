package com.g2forge.joint.ssg.staticcontent;

import java.util.HashMap;
import java.util.Map;

import com.g2forge.alexandria.media.IMediaRegistry;
import com.g2forge.alexandria.media.IMediaType;
import com.g2forge.alexandria.media.MediaType;
import com.g2forge.joint.core.ExtendedMediaType;
import com.g2forge.joint.core.copy.IFileConversionType;
import com.g2forge.joint.md.MD2HTMLConversionType;
import com.g2forge.joint.md.flexmark.FlexmarkConverter;
import com.g2forge.joint.plantuml.PlantUMLConversionType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class SystemConfiguration {
	public static Map<IMediaType, IFileConversionType> computeDefaultTypes() {
		final HashMap<IMediaType, IFileConversionType> retVal = new HashMap<>();
		retVal.put(MediaType.Markdown, new MD2HTMLConversionType(new FlexmarkConverter()));
		retVal.put(ExtendedMediaType.PlantUML, PlantUMLConversionType.create());
		return retVal;
	}

	@Builder.Default
	protected final IMediaRegistry registry = ExtendedMediaType.getRegistry();

	@Builder.Default
	protected final Map<IMediaType, IFileConversionType> types = computeDefaultTypes();
}