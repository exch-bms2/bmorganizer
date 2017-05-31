package bms.tool.bmorganizer;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.logging.Logger;

import bms.model.BMSDecoder;
import bms.model.BMSModel;

/**
 * 差分ファイル
 * 
 * @author exch
 */
public abstract class AbstractBMSFile {

	/**
	 * 差分ファイル
	 */
	final private File file;
	/**
	 * 許容するWAV定義損失率(0-100)
	 */
	private int missrate = 10;

	/**
	 * 差分ファイル内のBMSModelのキャッシュ
	 */
	private Map<String, BMSModel> models = new HashMap<String, BMSModel>();
	/**
	 * 差分ファイル内のファイル一覧のキャッシュ
	 */
	private String[] filenames;

	public AbstractBMSFile(File file) {
		this.file = file;
	}

	public int getMediaFileMissrate() {
		return missrate;
	}

	public void setMediaFileMissrate(int missrate) {
		this.missrate = missrate;
	}

	/**
	 * 対象のディレクトリに差分ファイルを導入する
	 * 
	 * @param dir
	 *            差分導入対象のディレクトリ
	 * @return 導入ログ
	 */
	public List<BMSImportLog> importDiffFiles(File dir) {
		List<BMSImportLog> sb = new ArrayList<BMSImportLog>();
		// 導入ファイルのリストアップ、分類
		List<String> wavfiles = new ArrayList<String>();
		List<String> bmsfiles = new ArrayList<String>();
		List<String> otherfiles = new ArrayList<String>();
		Map<String, Set<String>> diffmap = new HashMap<String, Set<String>>();
		InputStream is = null;
		try {
			// 導入先のディレクトリの全BMSファイルのmd5を取得
			Set<String> set = new HashSet<String>();
			for (File f : dir.listFiles()) {
				if (BMSImporter.isBMSFile(f.getName())) {
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					is = new DigestInputStream(new FileInputStream(f), digest);

					toByteArray(is);
					set.add(BMSDecoder.convertHexString(digest.digest()));
					is.close();
				}
			}

			for (String s : this.getContentFiles()) {
				if (BMSImporter.isAudioFile(s)) {
					otherfiles.add(s);
					// wavファイルはディレクトリ部分をカットする
					if (s.contains("/")) {
						s = s.substring(s.lastIndexOf('/') + 1);
					}
					if (s.contains("\\")) {
						s = s.substring(s.lastIndexOf('\\') + 1);
					}
					wavfiles.add(s);
				} else if (BMSImporter.isBMSFile(s)) {
					bmsfiles.add(s);
				} else {
					otherfiles.add(s);
				}
			}
			for (File f : dir.listFiles()) {
				String s = f.getName();
				if (BMSImporter.isAudioFile(s)) {
					wavfiles.add(s);
				}
			}

			boolean copyAll = false;
			for (String bmsfile : bmsfiles) {
				BMSModel model = this.getBMSModel(bmsfile);
				if (set.contains(model.getSHA256())) {
					sb.add(new BMSImportLog(this, model.getFullTitle(), bmsfile, dir,
							BMSImportLog.STATUS_SUCCEED, "同じ譜面が既に存在しています", diffmap));
				} else {
					int count = 0;
					// WAC定義に対応するファイルの存在チェック
					for (String wav : model.getWavList()) {
						boolean fileExists = false;
						for (String wavfile : wavfiles) {
							wavfile = wavfile.toLowerCase();
							if (wavfile.lastIndexOf('.') != -1) {
								wavfile = wavfile.substring(0, wavfile.lastIndexOf('.'));
							}
							wav = wav.toLowerCase();
							if (wav.lastIndexOf('.') != -1) {
								wav = wav.substring(0, wav.lastIndexOf('.'));
							}
							if (wav.equals(wavfile)) {
								fileExists = true;
								break;
							}
						}
						if (!fileExists) {
							count++;
						}
					}
					if (count <= model.getWavList().length * missrate / 100) {
						// WAV定義中で存在しないファイルが規定以下の場合、導入処理
						is = this.getInputStream(bmsfile);
						sb.add(new BMSImportLog(this, model.getFullTitle(), bmsfile, dir,
								BMSImportLog.STATUS_SUCCEED, this.copyFile(is, dir, bmsfile, model.getSHA256()),
								diffmap));
						is.close();
						copyAll = true;
						if (set.size() > 0) {
							diffmap.put(model.getSHA256(), set);
						}
					} else {
						// WAV定義中で存在しないファイルが規定以上の場合、導入しない
						if (count == 0) {
							sb.add(new BMSImportLog(this, model.getFullTitle(), bmsfile, dir,
									BMSImportLog.STATUS_FAILED, ") --> 差分導入中にエラーが発生したため、追加されませんでした", diffmap));
						} else if (model.getWavList().length == 0) {
							// BMSファイルにWAV定義がなかった場合(rarファイルの解凍失敗でまれに発生)
							sb.add(new BMSImportLog(this, model.getFullTitle(), bmsfile, dir,
									BMSImportLog.STATUS_FAILED,
									") --> bmsファイルを正常に展開できなかった可能性があるため、追加されませんでした : WAV定義なし", diffmap));
						} else {
							sb.add(new BMSImportLog(this, model.getFullTitle(), bmsfile, dir,
									BMSImportLog.STATUS_FAILED,
									(count * 100 / model.getWavList().length) + "%の存在しないWAV定義を検出したため、追加されませんでした",
									diffmap));
						}
					}
				}
			}
			// BMSファイルの導入処理成功時、それ以外の音源等のファイルも導入する
			if (copyAll) {
				for (String file : otherfiles) {
					is = this.getInputStream(file);
					sb.add(new BMSImportLog(this, "", file, dir, BMSImportLog.STATUS_SUCCEED,
							this.copyFile(is, dir, file, null), diffmap));
					is.close();
				}
			}
		} catch (Exception e) {
			sb.add(new BMSImportLog(this, dir.getPath() + "への差分導入中にエラー発生:" + e.getMessage()));
			Logger.getGlobal().severe("差分導入中の例外:" + e.getMessage());
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb;
	}

	private String copyFile(InputStream is, File dir, String filename, String hash)
			throws IOException, NoSuchAlgorithmException {
		if (filename.contains("/")) {
			filename = filename.substring(filename.lastIndexOf("/") + 1);
		}
		if (filename.contains("\\")) {
			filename = filename.substring(filename.lastIndexOf("\\") + 1);
		}
		File file = new File(dir.getPath() + "/" + filename);
		// コピー先に同一名ファイルがあった場合の処理
		if (file.exists()) {
			// 音源ファイル、BGAファイルは上書きしない
			if (!BMSImporter.isBMSFile(file.getName())) {
				return "コピー先にすでに同じファイルがあります";
			} else {
				// 衝突しないファイル名に変更
				String add = "";
				while (file.exists()) {
					add += "copy_";
					file = new File(dir.getPath() + "/" + add + filename);
				}
			}
		}
		Files.copy(is, file.toPath());
		return "OK";
	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (true) {
			int len = inputStream.read(buffer);
			if (len < 0) {
				break;
			}
			bout.write(buffer, 0, len);
		}
		return bout.toByteArray();
	}

	/**
	 * 差分ファイル内のBMSファイル名に対応したBMSModelを取得する。 <br />
	 * 一度取得したBMSModelはキャッシュされ、次回取得時は高速で取得可能
	 * 
	 * @param bmsfile
	 *            BMSファイル名
	 * @return BMSファイル名に対応したBMSModel
	 * @throws Exception
	 *             BMSファイルのInputStream取得失敗時にスロー
	 */
	public BMSModel getBMSModel(String bmsfile) throws Exception {
		if (models.get(bmsfile) == null) {
			BMSDecoder decoder = new BMSDecoder();
			models.put(bmsfile,
					decoder.decode(toByteArray(this.getInputStream(bmsfile)), bmsfile.toLowerCase().endsWith(".pms"), null));
		}
		return models.get(bmsfile);
	}

	public String[] getContentFiles() {
		if (filenames == null) {
			try {
				filenames = this.listFiles();
			} catch (Exception e) {
				Logger.getGlobal().severe("差分ファイル解析時の例外:" + e.getMessage());
				filenames = new String[0];
			}
		}
		return filenames;
	}

	/**
	 * 差分ファイル内のファイルをリストアップし、ファイル名を返す
	 * 
	 * @return 差分ファイル内のファイル名リスト
	 * @throws Exception
	 */
	protected abstract String[] listFiles() throws Exception;

	/**
	 * ファイル名に対応するInputStreamを返す
	 * 
	 * @param file
	 *            ファイル名
	 * @return ファイル名に対応するInputStream
	 * @throws Exception
	 */
	public abstract InputStream getInputStream(String file) throws Exception;

	/**
	 * 差分ファイル内で定義されいている同梱譜面hashを返す
	 * 
	 * @return 同梱譜面hash
	 * @throws Exception
	 */
	public abstract String[] getParentHash() throws Exception;

	/**
	 * 差分ファイルのclose処理を行う
	 * 
	 * @throws Exception
	 */
	public abstract void close() throws Exception;

	public File getFile() {
		return file;
	}

}