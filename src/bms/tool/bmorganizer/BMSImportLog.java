package bms.tool.bmorganizer;

import java.io.File;
import java.util.*;

/**
 * 差分導入時のログ
 * 
 * @author exch
 */
public class BMSImportLog {

	/**
	 * 導入ステータス
	 */
	private int status;
	
	public static final int STATUS_FAILED = 0;
	public static final int STATUS_SUCCEED = 1;
	public static final int STATUS_RETRY = 2;
	/**
	 * メッセージ
	 */
	private final String message;
	/**
	 * 差分hashと同梱譜面hashのマップ。導入成功時に記録される
	 */
	private final Map<String, Set<String>> diffmap;
	/**
	 * 差分ファイル
	 */
	private final AbstractBMSFile diff;
	/**
	 * 導入BMSのタイトル
	 */
	private final String title;
	/**
	 * 導入BMSファイル名
	 */
	private final String bmsfile;
	/**
	 * 差分導入先
	 */
	private File dir;

	public BMSImportLog(AbstractBMSFile diff, String message) {
		this(diff, null, null, null, STATUS_FAILED, message,
				new HashMap<String, Set<String>>());
	}

	public BMSImportLog(AbstractBMSFile diff, String title, String bmsfile,
			File dir, int succeed, String message,
			Map<String, Set<String>> diffmap) {
		this.diff = diff;
		this.title = title;
		this.bmsfile = bmsfile;
		setImportDirectory(dir);
		setStatus(succeed);
		this.message = message;
		this.diffmap = diffmap;
	}

	/**
	 * 差分導入成功したかどうかを返す
	 * 
	 * @return 差分導入成功であればtrue
	 */
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	/**
	 * 差分導入成功時に記録される差分hashと同梱譜面hashのマップを返す
	 * 
	 * @return 差分hashと同梱譜面hashのマップ
	 */
	public Map<String, Set<String>> getDiffMap() {
		return diffmap;
	}

	/**
	 * 差分ファイルを返す
	 * 
	 * @return 差分ファイル
	 */
	public AbstractBMSFile getDiffFile() {
		return diff;
	}

	/**
	 * 導入先のディレクトリを返す
	 * 
	 * @return 導入先のディレクトリ
	 */
	public File getImportDirectory() {
		return dir;
	}
	
	public void setImportDirectory(File dir) {
		this.dir = dir;;
	}
	
	public String getText() {
		return message;
	}
	
	/**
	 * ログに対応したメッセージを作成する
	 * 
	 * @return ログに対応したメッセージ
	 */
	public String getMessage() {
		if (getStatus() == STATUS_FAILED) {
			if (diff == null) {
				return message;
			}
			if (dir == null) {
				return "[" + diff.getFile().getName() + "] : " + message;
			}
			return "[" + diff.getFile().getName() + "]" + title + "(" + bmsfile
					+ ") --> " + message;
		}
		return "[" + diff.getFile().getName() + "]" + title + "(" + bmsfile
				+ ") --> " + message;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getBmsfile() {
		return bmsfile;
	}

}
