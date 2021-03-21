package com.g2forge.joint.md;

import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.dataaccess.IDataSink;
import com.g2forge.alexandria.java.io.dataaccess.IDataSource;

public interface IMDConverter {
	public void convert(IDataSource source, IDataSink sink, IFunction1<? super String, ? extends String> rewriteURLs);
}
