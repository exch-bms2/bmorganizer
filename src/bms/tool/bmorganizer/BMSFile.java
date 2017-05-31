package bms.tool.bmorganizer;

import java.io.*;

/**
 * 差分BMSファイル
 * 
 * @author exch
 */
public class BMSFile extends AbstractBMSFile {

	public BMSFile(File file) {
		super(file);
	}

	@Override
	public String[] listFiles() {
		return new String[] { getFile().getName() };
	}

	@Override
	public InputStream getInputStream(String file)
			throws FileNotFoundException {
		return new FileInputStream(getFile());
	}

	public String[] getParentHash() {
		return new String[0];
	}

	@Override
	public void close() {
	}

}