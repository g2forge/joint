package com.g2forge.joint.md.enigma;

import java.lang.reflect.Type;

public interface IDocConvertContext {
	public IExplicitDocElement toExplicit(Object object, Type type);
}
