import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sorter {
	
	private static final Path home = Paths.get(System.getProperty("user.home"));
	
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
		new DirectoryWatcher(sourceDir).process(keyword, targetDir);
	}
	
}