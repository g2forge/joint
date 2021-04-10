package com.g2forge.joint.core.copy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.Stack;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.function.IConsumer1;
import com.g2forge.alexandria.java.function.IFunction1;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.alexandria.java.io.file.AMultithrowFileVisitor;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class CopyComponent implements IComponent {
	@Data
	@Builder(toBuilder = true)
	@RequiredArgsConstructor
	public static class Entry<Context> {
		public enum Type {
			File,
			Directory;
		}

		/** The path to the input entry on disk. */
		protected final Path path;

		/** The path to the entry, relative to the input root directory. */
		protected final Path relative;

		/** The file system type. */
		protected final Type type;

		/** The hierarchical context this entry was discovered in. */
		protected final Context context;

		/** {@code true} if this is the root entry. */
		protected final boolean root;
	}

	public interface IEntryHandler<Context> extends IFunction1<Entry<Context>, Operation<Context>> {
		@Override
		public Operation<Context> apply(Entry<Context> entry);
	}

	@Data
	@Builder(toBuilder = true)
	@RequiredArgsConstructor
	public static class Operation<Context> {
		public static <Context> Operation<Context> createIgnore() {
			return new Operation<>(false, null, null);
		}

		/** Should the map operation descend to children of the input entry? Defaults to {@code true}. */
		@Builder.Default
		protected final boolean recurse = true;

		/** The conversion type for the entry. Defaults to copy. */
		@Builder.Default
		protected final IFileConversionType conversionType = CopyConversionType.create();

		/** The hierarchical context started by this entry, to be passed to any entries found under this one. */
		@Builder.Default
		protected final Context context = null;
	}

	public static <Context> void map(final Path input, final Path output, Path working, IFunction1<? super Entry<Context>, ? extends Operation<Context>> handler, IConsumer1<? super IConversion> consumer) {
		try {
			Files.walkFileTree(input, new AMultithrowFileVisitor() {
				protected final Stack<Context> contextStack = new Stack<>();

				protected FileVisitResult handle(Entry.EntryBuilder<Context> entryBuilder, Path path) {
					if ((working != null) && path.startsWith(working)) return FileVisitResult.SKIP_SUBTREE;

					if (!contextStack.isEmpty()) entryBuilder.context(contextStack.peek());
					final Path relative = input.relativize(path);
					final Entry<Context> entry = entryBuilder.path(path).relative(relative).root(input.equals(path)).build();
					final Operation<Context> operation = handler.apply(entry);
					if (operation.getConversionType() != null) consumer.accept(new FileConversion(input, relative, output, operation.getConversionType()));
					if (operation.isRecurse() && Entry.Type.Directory.equals(entry.getType())) contextStack.push(operation.getContext());
					return operation.isRecurse() ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path path, IOException exception) throws IOException {
					contextStack.pop();
					return super.postVisitDirectory(path, exception);
				}

				@Override
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
					super.preVisitDirectory(path, attributes);
					return handle(Entry.<Context>builder().type(Entry.Type.Directory), path);
				}

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
					super.visitFile(path, attributes);
					return handle(Entry.<Context>builder().type(Entry.Type.File), path);
				}
			});
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	/** Input directory; the source of the copy. */
	protected final Path input;

	/** Output directory; the target of the copy. */
	protected final Path output;

	/** A working directory. If this directory is nested under the input, it will be skipped. */
	protected final Path working;

	/** A handler which specifies the exact operation for each entry found in the {@link #input}. */
	@Builder.Default
	protected final IFunction1<? super Entry<Object>, ? extends Operation<Object>> handler = entry -> {
		if (entry.isRoot()) return Operation.builder().conversionType(CreateDirectoryConversionType.create()).build();
		return Operation.builder().build();
	};

	@Override
	public Set<Path> getInputs() {
		return HCollection.asSet(getInput());
	}

	@Override
	public Set<Path> getOutputs() {
		return HCollection.asSet(getOutput());
	}

	@Override
	public void map(IConsumer1<? super IConversion> consumer) {
		map(getInput(), getOutput(), getWorking().toAbsolutePath(), getHandler(), consumer);
	}
}
