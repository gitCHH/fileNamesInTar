package checkZip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

public class DoCheck {

	private final static String[] illegalExtension = { "README", ".LOG",
			".PFX", ".TXT", ".DOC", ".DOCX", ".XLS", ".XLSX", ".PPT", ".PPTX",
			".ZIP", ".GZ", ".RAR", ".TAR", ".7Z", ".PDF" };

	public static void main(String[] args) throws IOException {
		if (args.length <= 0) {
			System.out
					.println("First argument is  tar filename. [Optional, plain text file] Second argument is whitelist file.[Optional, default JVM's] Third argument is charset of whitelist file and resultfile.");
			return;
		}
		String tarFileName = args[0];
		if (!checkArgs(tarFileName)) {
			System.exit(0);
		}
		String whitelistFile = null;
		if (args.length >= 2) {
			whitelistFile = args[1];
			if (!checkArgs(args[1])) {
				System.exit(0);
			}
		}
		String charset = null;
		if (args.length >= 3) {
			charset = args[2];
			Set<String> charsets = Charset.availableCharsets().keySet();
			if (!charsets.contains(charset)) {
				System.out.println("Charset " + charset
						+ " is not available. Available charsets:");
				for (String s : charsets) {
					System.out.print(s + " ");
				}
				System.exit(0);
			}
		}
		if (tarFileName.toUpperCase().endsWith("TAR")) {
			HashSet<String> filesInTar = readTar(tarFileName);
			HashSet<String> whitelist = getWhiteList(whitelistFile, charset);
			HashSet<String> filesInTar2=new HashSet<String>(filesInTar);
			HashSet<String> result = checkTar(whitelist, filesInTar);
			generateRS(filesInTar2, whitelist, result, tarFileName, charset);
			System.exit(0);
		} else {
			System.out.print("The file to be checked must be a tar file.");
			System.exit(0);
		}
	}

	/*
	 * get filenames in the tar file.
	 */
	private static HashSet<String> readTar(String fileName) {
		TarInputStream tis = null;
		HashSet<String> set = null;
		try {
			tis = new TarInputStream(new FileInputStream(fileName));
			int fileCount = 0;
			set = new HashSet<String>();
			TarEntry te = tis.getNextEntry();
			while (te != null) {
				if (!te.isDirectory()) {
					String name = te.getName();
					for (String s : illegalExtension) {
						if (name.toUpperCase().contains(s)) {
							set.add(name);
							// System.out.println(name);
							break;
						}
					}
					fileCount++;
				}
				te = tis.getNextEntry();
			}
			System.out.println("INFO: The package contains " + fileCount
					+ " files with illegal extension.");
			/*
			 * int index = 1; System.out.format("    %-10s%-80s%-20s %n%n",
			 * "NO.", "FileName", "Bytes"); for (TarEntry e : set) {
			 * System.out.format("    %-10s%-80s%-20s %n%n", index, e.getName(),
			 * e.getSize()); index++; }
			 */
			return set;
		} catch (FileNotFoundException e) {
			System.out.println("INFO: File not exist.");
		} catch (IOException e) {
			System.out
					.println("WARNING: IOException occured in TarInputStream.");
		} finally {
			if (tis != null) {
				try {
					tis.close();
				} catch (IOException e) {
					System.out
							.println("WARNING: IOException occured when close file.");
				}
			}
		}
		return set;
	}

	/*
	 * get whitelist without absolute file path.
	 */
	private static HashSet<String> getWhiteList(String fileName, String encoding) {
		if (fileName == null) {
			return null;
		}
		HashSet<String> rs = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			FileInputStream fis = new FileInputStream(fileName);
			if (encoding != null) {
				isr = new InputStreamReader(fis, encoding);
				System.out.println("Reading '" + fileName + "' in " + encoding);
			} else {
				isr = new InputStreamReader(fis);
			}
			br = new BufferedReader(isr);
			String line = null;
			rs = new HashSet<String>();
			while ((line = br.readLine()) != null) {
				rs.add(line);
			}
		} catch (FileNotFoundException e) {
			System.out.println("WARNING: '" + fileName + "' is not exist.");
		} catch (IOException e) {
			System.out
					.println("WARNING: IOException occured while reading whitelist file.");
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				System.out
						.println("WARNING: IOException occured when close BufferedReader.");
			}
		}
		return rs;
	}

	/*
	 * remove
	 */
	private static HashSet<String> checkTar(HashSet<String> whitelist,
			HashSet<String> entries) {
		HashSet<String> rs = null;
		if (whitelist == null) {
			rs = entries;
		} else {
			Iterator<String> it = entries.iterator();
			while (it.hasNext()) {
				String filename = it.next();
				for (String s : whitelist) {
					if (filename.contains(s.trim())) {
						it.remove();
						//System.out.print("remove.");
						break;
					}
				}
			}
			rs = entries;
		}
		return rs;
	}

	private static void generateRS(HashSet<String> filesInTar,
			HashSet<String> whitelist, HashSet<String> result, String fileName,
			String charSet) throws IOException {
		Calendar cal = Calendar.getInstance();
		String outFileName = fileName + "-" + cal.get(cal.YEAR)
				+ (cal.get(cal.MONTH) + 1) + cal.get(cal.DAY_OF_MONTH)
				+ cal.get(cal.HOUR_OF_DAY) + cal.get(cal.MINUTE) + ".txt";
		System.out.println("Result file to generate: " + outFileName);
		PrintWriter writer = null;
		try {
			if (charSet != null) {
				writer = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(outFileName),
						Charset.forName(charSet)));
			} else {
				writer = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(outFileName)));
			}
			int index = 1;
			if (result != null && result.size() >= 1) {
				writer.println("***Possible illegal files:");
				writer.println();
				for (String s : result) {
					writer.format("%-10s%-80s %n", index, s);
					index++;
				}
			} else {
				writer.println(fileName + " contains no illegal file.");
			}
			writer.println();
			index = 1;
			if (whitelist != null && whitelist.size() >= 1) {
				writer.println("***Whitelist:");
				writer.println();
				for (String s : whitelist) {
					writer.format("%-10s%-80s %n", index, s);
					index++;
				}
			} else {
				writer.println("No whitelist.");
			}
			writer.println();
			index = 1;
			if (filesInTar != null && filesInTar.size() >= 1) {
				writer.println("***Files with illegal extension in tar:");
				writer.println();
				for (String s : filesInTar) {
					writer.format("%-10s%-80s %n", index, s);
					index++;
				}
			} else {
				writer.println("No file with illegal extension in " + fileName);
			}

		} catch (FileNotFoundException e) {
			System.out
					.println("WARNING: IOException occured when create result file.");
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private static boolean checkArgs(String fileName) {
		File f = new File(fileName);
		if (!f.exists()) {
			System.out.println("INFO: '" + fileName + "' is not exist.");
			return false;
		}
		if (!f.canRead()) {
			System.out.println("INFO: '" + fileName + "' can't be read.");
			return false;
		}
		if (f.isDirectory()) {
			System.out
					.println("INFO: '" + fileName + "' can't be a directory.");
			return false;
		}
		return true;
	}
}
