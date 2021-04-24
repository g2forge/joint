package com.g2forge.joint.ssg;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.core.marker.ICommand;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.ssg.operation.Operation;

public abstract class AJointCommand implements Callable<Integer> {
	@Override
	public Integer call() throws Exception {
		final ICloseable closeable = start();
		wait(closeable);
		return ICommand.SUCCESS;
	}

	protected abstract Configuration configure();

	protected abstract Operation getOperation();

	protected ICloseable start() {
		return getOperation().create().invoke(configure());
	}

	protected void wait(final ICloseable closeable) {
		if (closeable != null) {
			System.out.println("Press the \"Enter\" key to stop serving...");
			try {
				System.in.read();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
			closeable.close();
		}
	}
}
