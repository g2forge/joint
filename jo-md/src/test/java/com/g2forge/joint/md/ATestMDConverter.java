package com.g2forge.joint.md;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.io.dataaccess.ByteArrayDataSink;
import com.g2forge.alexandria.java.io.dataaccess.ResourceDataSource;
import com.g2forge.alexandria.test.HAssert;
import com.g2forge.habitat.trace.HTrace;

import lombok.AccessLevel;
import lombok.Getter;

public abstract class ATestMDConverter {
	@Rule
	@Getter(AccessLevel.PROTECTED)
	public final TestName name = new TestName();

	@Test
	public void angular() {
		test();
	}

	@Test
	public void doc0() {
		test();
	}

	@Test
	public void doc1() {
		test();
	}

	protected abstract IMDConverter getConverter();

	protected void test() {
		final String name = getName().getMethodName();
		final ByteArrayDataSink actual = new ByteArrayDataSink();
		getConverter().convert(new ResourceDataSource(new Resource(HTrace.getMethod(1).getDeclaringClass(), name + ".md")), actual, null);
		HAssert.assertEquals(new Resource(getClass(), name + ".html"), actual.getStream().toString());
	}
}
