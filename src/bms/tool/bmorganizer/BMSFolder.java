package bms.tool.bmorganizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 差分フォルダ
 * 
 * @author exch
 */
public class BMSFolder extends AbstractBMSFile {

	public BMSFolder(File file) {
		super(file);
	}

	@Override
	public String[] listFiles() {
		List<String> files = new ArrayList<String>();
		for (File f : getFile().listFiles()) {
			files.add(f.getName());
		}
		return files.toArray(new String[0]);
	}

	@Override
	public InputStream getInputStream(String file)
			throws FileNotFoundException {
		return new FileInputStream(new File(getFile().getPath() + "/"
				+ file));
	}

	public String[] getParentHash() {
		return new String[] { getFile().getName() };
	}

	@Override
	public void close() {
	}
}