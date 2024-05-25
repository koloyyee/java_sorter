import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * DirectoryWatcher will register and watch a directory
 * <br>
 * In this case it will be ~/Download
 * To keep it simple it doesn't support recursive
 * We will watch for create and modify. {@link #register(Path)}
 * <hr>
 *   TODO:
 *   <p>1. <code> java Sorter </code>  </p>
 *   <p>Default - no args</p>
 * <p> By default, we will be watching ~/Download </p>
 *
 * <p> we will check the new item is an image with jpg, svg, png <br>
 * then create an "images" directory if not exists on the Desktop for Mac (not sure about Windows and Linux yet).<br>
 * move the new images to the "images" directory.
 * </p>
 * <p>2. e.g.: <code>java Sorter -k CST </code></p>
 * -k Keyword that filename contains
 * <p> With the keyword flag new file contains keyword will be moved to directory with the keyword <br>
 * if the directory is doesn't exist, system will create one.
 * </p>
 * <p>3. e.g.: <code>java Sorter -k CST -t CST</code></p>
 * -k Keyword
 * -t Target directory
 * <p> With the keyword flag new file contains keyword will be moved to <strong>target</strong> directory<br>
 * if the directory is doesn't exist, system will create one.
 * </p>
 */
public class DirectoryWatcher {
	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final Path home;
	private final Path downloads;
	private boolean trace;
	
	public DirectoryWatcher(Path source, Path... dest) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<>();
		register(source);
		this.trace = true;
		
		home = Path.of(System.getProperty("user.home"));
		downloads = home.resolve("Downloads");
	}
	
	;
	
	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}
	
	/**
	 * register the directory we are watching.
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(dir);
			if (prev == null) {
				System.out.printf("registering: %s\n", dir);
			} else {
				System.out.printf("updating: %s -> %s\n", prev, dir);
			}
		}
		keys.put(key, dir);
	}
	
	/**
	 * In recursive situation, use walkFileTree to register all files
	 */
	private void registerAll(final Path dir) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			/**
			 * Invoked for a directory before entries in the directory are visited.
			 *
			 * <p> Unless overridden, this method returns {@link FileVisitResult#CONTINUE
			 * CONTINUE}.
			 *
			 * @param dir
			 * @param attrs
			 */
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	/**
	 * Key method to sorting.
	 * We will look at the source directory,<br>
	 * then will move the file to the target directory or create target directory then move to it.
	 */
	void process() throws IOException {
		for (; ; ) {
			WatchKey key;
			try {
				key = watcher.take(); // polling
			} catch (InterruptedException e) {
				System.err.println(e.getLocalizedMessage());
				return;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized.");
				continue;
			}
			
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				if (kind == OVERFLOW) {
					continue;
				}
				
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path newItem = dir.resolve(name);
				// TODO: check the file type
				String fileType = checkFileType(newItem);
				System.out.format("%s: %s\n", event.kind().name(), newItem);
				if (fileType != null && !fileType.isEmpty()) {
					/**
					 *
					 * <ol>
					 *   File types are
					 *   <li>text/plain</li>
					 *   <li>images/jpeg</li>
					 *   <li>images/png</li>
					 *   <li>application/zip</li>
					 * </ol>
					 * */
					if (fileType.contains("image")) {
						//TODO: move the newItem to a new "image" director, create the directory if it doesn't exist.
					}
					// TODO: move the newItem to a target directory, create the directory if it doesn't exist.
				}
				
				
			}
		}
	}
	
	/**
	 * Check the file type such as jpg, png
	 * return if
	 */
	String checkFileType(Path file) throws IOException {
		if (Files.isRegularFile(file)) {
			return Files.probeContentType(file);
		}
		return "";
	}
	
	/**
	 * Perform the move mechanism
	 */
	void move(Path file, Path to) {
		System.out.println("moving file " + file);
		System.out.println("to " + to);
		Path target = to.resolve(file.getFileName());
		try {
			if (!Files.isDirectory(to)) {
				createDir(to);
			}
			Files.move(file, target, REPLACE_EXISTING, ATOMIC_MOVE);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	void createDir(Path dirname) {
		try {
			Files.createDirectory(dirname);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	void moveToImagesDir(Path newImage) {
		Path desktop = home.resolve("Desktop");
		System.out.println(desktop);
		Path imagesDir = desktop.resolve("images");
		move(newImage, imagesDir);
	}
	
}
