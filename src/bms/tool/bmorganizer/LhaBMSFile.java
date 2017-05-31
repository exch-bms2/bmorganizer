package bms.tool.bmorganizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import jp.gr.java_conf.dangan.util.lha.LhaHeader;
import jp.gr.java_conf.dangan.util.lha.LhaInputStream;

/**
 * lzh差分ファイル
 * 
 * @author exch
 */
public class LhaBMSFile extends AbstractBMSFile {

	private LhaInputStream lhais;

	public LhaBMSFile(File file) {
		super(file);
	}

	@Override
	public String[] listFiles() throws IOException {
		List<String> files = new ArrayList<String>();
		LhaInputStream lis = new LhaInputStream(new FileInputStream(getFile()));
		LhaHeader head;
		while ((head = lis.getNextEntry()) != null) {
			files.add(head.getPath());
		}
		lis.close();
		return files.toArray(new String[0]);
	}

	@Override
	public InputStream getInputStream(String file) throws IOException {
		lhais = new LhaInputStream(new FileInputStream(getFile()));
		LhaHeader head;
		while ((head = lhais.getNextEntry()) != null) {
			if (head.getPath().equals(file)) {
				return lhais;
			}
		}
		return null;
	}

	public String[] getParentHash() {
		return new String[0];
	}

	@Override
	public void close() throws Exception {
		if (lhais != null) {
			lhais.close();
		}

	}
}