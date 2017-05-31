package bms.tool.bmorganizer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * zip差分ファイル
 * 
 * @author exch
 */
public class ZipBMSFile extends AbstractBMSFile {

	private ZipFile zip;

	public ZipBMSFile(File file) throws IOException {
		super(file);
		zip = new ZipFile(file, Charset.forName("Shift_JIS"));
	}

	@Override
	public String[] listFiles() {
		List<String> files = new ArrayList<String>();
		Enumeration<? extends ZipEntry> e = zip.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			if (!entry.isDirectory()) {
				files.add(entry.getName());
			}
		}
		return files.toArray(new String[0]);
	}

	@Override
	public InputStream getInputStream(String file) throws IOException {
		Enumeration<? extends ZipEntry> e = zip.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			if (!entry.isDirectory() && entry.getName().equals(file)) {
				return zip.getInputStream(entry);
			}
		}
		return null;
	}

	public String[] getParentHash() {
		Enumeration<? extends ZipEntry> e = zip.entries();
		String hash = "dummy";
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			if (entry.getName().contains("/")) {
				hash = entry.getName();
				hash = hash.substring(0, hash.indexOf("/"));
				Logger.getGlobal()
						.info(zip.getName() + "を展開中 - " + " parent md5 : "
								+ hash);
				break;
			}
		}
		return new String[] { hash };
	}

	@Override
	public void close() throws IOException {
		zip.close();
	}
}