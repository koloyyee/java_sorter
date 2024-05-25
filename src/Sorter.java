import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sorter{
	public static void main(String[] args) throws IOException {
		if(args.length == 0  ||  args.length > 4) {
			System.out.println("wait wait");
		}
		Path home = Paths.get(System.getProperty("user.home"));
		Path downloads = home.resolve("Downloads");
		int dirArgs=0;
		Path dir = Paths.get(args[dirArgs]);
		new DirectoryWatcher(downloads).process();
	}
	
}