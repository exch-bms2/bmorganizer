package bms.tool.bmorganizer;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Logger;

import bms.model.*;

/**
 * BMS差分ファイル導入用クラス
 * 
 * @author exch
 */
public class BMSImporter {

	// TODO Diffフォルダ以下に配置した差分の一括導入機能
	// TODO 導入時チェック機能(音源やBGAの有無の割合が一定以下の場合、ユーザーに確認)

	private int mode = BACKUP_AND_REMOVE_FILE_AFTER_SUCCEED;
	/**
	 * 差分導入成功時の差分ファイルの処理:何もしない
	 */
	public static final int DO_NOTHING_AFTER_SUCCEED = 0;
	/**
	 * 差分導入成功時の差分ファイルの処理:Diffフォルダに移動
	 */
	public static final int BACKUP_AND_REMOVE_FILE_AFTER_SUCCEED = 1;
	/**
	 * 差分導入成功時の差分ファイルの処理:削除
	 */
	public static final int REMOVE_FILE_AFTER_SUCCEED = 2;

	private SongAccessor controller;

	private int missrate = 10;

	public BMSImporter(SongAccessor controller) {
		this.controller = controller;
	}

	/**
	 * 差分ファイルを導入する
	 * 
	 * @param files
	 *            差分ファイル群
	 * @param dir
	 *            導入先のファイルパス。nullの場合は推定する
	 * @return 導入ログ
	 */
	public List<BMSImportLog> append(File[] files, String dir) {
		List<BMSImportLog> importLog = new ArrayList<BMSImportLog>();
		for (File f : files) {
			AbstractBMSFile fp = null;
			try {
				// フォルダ処理
				if (f.isDirectory()) {
					fp = new BMSFolder(f);
				}
				// zipファイル処理
				final String filename = f.getName().toLowerCase();
				if (filename.endsWith(".zip")) {
					fp = new ZipBMSFile(f);
				}
				// rarファイル処理
				if (filename.endsWith(".rar")) {
					fp = new RarBMSFile(f);
				}
				// lhaファイル処理
				if (filename.endsWith(".lzh")) {
					fp = new LhaBMSFile(f);
				}
				// bmsファイル処理
				if (isBMSFile(filename)) {
					fp = new BMSFile(f);
				}
				if (fp != null) {
					fp.setMediaFileMissrate(missrate);
					Map<String, BMSModel> models = new HashMap<String, BMSModel>();
					if (dir == null) {
						// 差分ファイル自体の同梱譜面md5定義を検出
						String[] parent = fp.getParentHash();
						Song song = controller.getSong(parent);
						if(song != null) {
							dir = song.getPath();
						}

						if (dir == null) {
							for (String s : fp.getContentFiles()) {
								if (isBMSFile(s)) {
									BMSModel model = fp.getBMSModel(s);
									models.put(s, model);
								}
							}
						}

						if (dir == null) {
							// 差分BMSファイルのタイトルから同梱譜面推測
							ParentBMSDetector pbd = new ParentBMSDetector(controller);
							for (String key : models.keySet()) {
								BMSModel model = models.get(key);
								song = pbd.detect(model.getFullTitle(), model.getFullArtist());
								if (song != null) {
									dir = song.getPath();
									break;
								}
							}
						}
					}
					
					// 差分導入先を測定した場合、差分導入処理
					if (dir != null) {
						dir = dir.replace("\\", "/");
						dir = dir.substring(0, dir.lastIndexOf("/"));
						List<BMSImportLog> logs = fp.importDiffFiles(new File(dir));
						boolean succeed = false;
						for (BMSImportLog log : logs) {
							if (log.getStatus() == BMSImportLog.STATUS_SUCCEED) {
								succeed = true;
								break;
							}
						}
						importLog.addAll(logs);
						fp.close();
						if (succeed) {
							// 差分導入成功したファイルの後処理
							Path dest = null;
							switch (mode) {
							case BACKUP_AND_REMOVE_FILE_AFTER_SUCCEED:
								dest = Paths.get("Diff");
								if(!Files.isDirectory(dest)) {
									Files.createDirectory(dest);
								}
							case REMOVE_FILE_AFTER_SUCCEED:
								MyFileVisitor visitor = new MyFileVisitor();
								visitor.dest = dest;
								Files.walkFileTree(f.toPath(), visitor);
							case DO_NOTHING_AFTER_SUCCEED:
								break;
							}
						}
					} else {
						for (String key : models.keySet()) {
							importLog.add(new BMSImportLog(fp, models.get(key).getFullTitle(), key, null,
									BMSImportLog.STATUS_FAILED, "導入先が不明です",
									new HashMap<String, Set<String>>()));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				importLog.add(new BMSImportLog(fp, "[" + f.getName() + "] : 導入中にエラー発生 - " + e.getMessage()));
				Logger.getGlobal().warning(e.getMessage());
			} finally {
				if (fp != null) {
					try {
						fp.close();
					} catch (Exception e1) {
						Logger.getGlobal()
								.severe("差分ファイル" + fp.getFile().getName() + "のクローズに失敗しました : " + e1.getMessage());
						e1.printStackTrace();
					}
				}
			}

		}
		return importLog;
	}

	public class MyFileVisitor implements FileVisitor<Path> {
		public Path dest;

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// ディレクトリのコピー
			if(dest != null) {
				Files.copy(dir, dest.resolve(dir), StandardCopyOption.COPY_ATTRIBUTES);								
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// ファイルをコピー
			Files.copy(file, dest.resolve(file), StandardCopyOption.COPY_ATTRIBUTES);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	public static boolean isBMSFile(String s) {
		s = s.toLowerCase();
		return s.endsWith(".bms") || s.endsWith(".bme") || s.endsWith(".bml") || s.endsWith(".pms")
				|| s.endsWith(".bmson");
	}

	public static boolean isAudioFile(String s) {
		s = s.toLowerCase();
		return s.endsWith(".wav") || s.endsWith(".ogg") || s.endsWith(".mp3");
	}

	/**
	 * 終了時の処理
	 */
	public void stop() {
		// 差分に対応した同梱譜面md5のマップ保存
	}
}
