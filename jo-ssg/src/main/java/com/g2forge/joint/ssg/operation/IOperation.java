package com.g2forge.joint.ssg.operation;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.joint.core.Configuration;

public interface IOperation {
	public ICloseable invoke(Configuration configuration);
}
