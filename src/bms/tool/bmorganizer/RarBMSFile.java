package bms.tool.bmorganizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

/**
 * rar差分ファイル
 * 
 * @author exch
 */
public class RarBMSFile extends AbstractBMSFile {

	// TODO bug:rarファイルの一部が部分的に欠落する

	private Archive archive;

	public RarBMSFile(File file) throws RarException, IOException {
		super(file);
		archive = new Archive(file);
	}

	@Override
	public String[] listFiles() {
		List<String> files = new ArrayList<String>();
		FileHeader[] fh = new FileHeader[archive.getFileHeaders().size()];
		archive.getFileHeaders().toArray(fh);
		for (FileHeader head : fh) {
			if (!head.isDirectory()) {
				// 処理するファイル名を取得
				files.add(head.getFileNameString());
			}
		}
		return files.toArray(new String[0]);
	}

	@Override
	public InputStream getInputStream(String file) throws RarException,
			IOException {
		for (FileHeader head : archive.getFileHeaders()) {
			if (!head.isDirectory()
					&& head.getFileNameString().equals(file)) {
				// 処理するファイル名を取得
				Logger.getGlobal().info(
						"rarファイルを展開中 - file : " + head.getFileNameString()
								+ " size : " + head.getDataSize());
				return archive.getInputStream(head);
			}
		}
		return null;
	}

	public String[] getParentHash() {
		String hash = "dummy";
		for (FileHeader head : archive.getFileHeaders()) {
			if (head.isDirectory()) {
				hash = head.getFileNameString();
				Logger.getGlobal().info(
						"rarファイルを展開中 - " + " parent md5 : " + hash);
				break;
			}
		}
		return new String[] { hash };
	}

	@Override
	public void close() throws Exception {
		archive.close();
	}
}