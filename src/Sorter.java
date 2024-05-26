import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Sorter will register and watch a directory
 * <br>
 * In this case it will be ~/Download
 * To keep it simple it doesn't support recursive
 * We will watch for create and modify. {@link #register(Path)}
 * <hr>
 *   <p>1. <code> java src/Sorter.java </code>  </p>
 *   <p>Default - no args</p>
 * <p> By default, we will be watching ~/Download </p>
 *
 * <p> we will check the new item is an image with jpg, svg, png <br>
 * then create an "images" directory if not exists on the Desktop for Mac (not sure about Windows and Linux yet).<br>
 * move the new images to the "images" directory.
 * </p>
 * <p>2. e.g.: <code>java src/Sorter.java fromDir toDir </code></p>
 * <p> fromDir is the where the system will register the directory,<br>
 * <p> toDir is the destination
 * if the directory is doesn't exist, system will create one.
 * </p>
 * </p>
 * <p>3. e.g.: <code>java src/Sorter.java fromDir toDir -k CST </code></p>
 * -k Keyword that filename contains
 * <p> With the keyword flag new file contains keyword will be moved to directory with the keyword <br>
 * if the directory is doesn't exist, system will create one.
 * </p>
 
 */
public class Sorter {
	private static final Path home = Path.of(System.getProperty("user.home"));
	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final Path downloads;
	private final Path desktop;
	private final Path source;
	private boolean trace;
	
	public Sorter(Path source) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<>();
		this.source = source;
		register(this.source);
		this.trace = true;
		
		
		downloads = home.resolve("Downloads");
		desktop = home.resolve("Desktop");
	}
	
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
	void process(String keyword, Path specificDir) throws IOException {
		System.out.println("We are watching " + this.source);
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
					if (fileType.contains("image") && specificDir == null) {
						//TODO: move the newItem to a new "image" director, create the directory if it doesn't exist.
						moveToImagesDir(newItem);
					} else if (!keyword.isEmpty() && newItem.getFileName().toString().contains(keyword)) {
						// TODO: move the newItem to a target directory, create the directory if it doesn't exist.
						Path keywordDir = specificDir.resolve(keyword);
						move(newItem, keywordDir);
					} else {
						move(newItem, specificDir);
					}
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
	
	/**
	 * The default method with there are no arguments.
	 */
	void moveToImagesDir(Path newImage) {
		Path imagesDir = desktop.resolve("images");
		move(newImage, imagesDir);
	}
	
	public static void usage() {
		System.out.println("""
				java Sorter - default behaviour: put any images from ~/Downloads to an image directory in ~/Desktop\n
				java Sorter [fromDir] [toDir] - 1st argument FROM which directory, 2nd argument TO which directory
				if toDir doesn't exist, it will be created, if fromDir doesn't exist, the program will be terminated.\n
				java Sorter [fromDir] [toDir] -k [TheKeyWord] - 4th is the keyword, only files contains the keyword will be moved to a directory with the keyword under destination directory.
				""");
	}
	
	public static void main(String[] args) throws IOException {
		Path sourceDir = null;
		String keyword = "";
		Path targetDir = null;
		
		if (args.length > 4) {
			System.out.println("wait wait");
			usage();
			return;
		}
		if (args.length == 0) {
			sourceDir = home.resolve("Downloads");
		} else {
			
			if (args[0].contains("~")) {
				int tildaIdx = args[0].indexOf("~");
				// +2 is because if Path see "/" it will override as a "/" root directory.
				sourceDir = home.resolve(args[0].substring(tildaIdx + 2));
			} else {
				sourceDir = Paths.get(args[0]);
			}
			
			if (args[1].contains("~")) {
				int tildaIdx = args[1].indexOf("~");
				targetDir = home.resolve(args[1].substring(tildaIdx + 2));
			} else {
				targetDir = Paths.get(args[1]);
			}
			
			if (!Files.isDirectory(sourceDir)) {
				usage();
				throw new IllegalArgumentException(sourceDir + " is not a directory.");
			}
		}
		if (args.length > 2) {
			if (args[2].equals("-k") && args[3] != null) {
				keyword = args[3];
			} else {
				throw new IllegalArgumentException("keyword cannot be empty.");
			}
		}
		new Sorter(sourceDir).process(keyword, targetDir);
	}
}
