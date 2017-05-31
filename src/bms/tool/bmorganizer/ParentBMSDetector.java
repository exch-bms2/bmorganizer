package bms.tool.bmorganizer;

import java.util.logging.Logger;

/**
 * 差分から同梱譜面を推定するためのクラス
 * 
 * @author exch
 */
public class ParentBMSDetector {

	private SongAccessor controller;

	private static final char[] delimiter = { '<', '[', '(', '-', ' ' };

	public ParentBMSDetector(SongAccessor controller) {
		this.controller = controller;
	}

	/**
	 * タイトルから同梱譜面を推定し、SongDataとして返す
	 * 
	 * @param title
	 *            タイトル
	 * @return 推定した同梱譜面パス。推定できなかった場合はnull
	 */
	public Song detect(String title) {
		return this.detect(title, null);
	}

	/**
	 * タイトル、アーティストから同梱譜面を推定し、SongDataとして返す
	 * 
	 * @param title
	 *            タイトル
	 * @param artist
	 *            アーティスト
	 * @return 推定した同梱譜面パス。推定できなかった場合はnull
	 */
	public Song detect(String title, String artist) {
		Logger.getGlobal().info("同梱譜面推定開始 TITLE:" + title + " ARTIST:" + artist);
		while (title.length() > 0) {
			Song[] dtes = controller.getSong(title, null);
			if (dtes.length > 0) {
				// アーティスト名の指定がある場合、さらに絞り込み
				for (Song dte : dtes) {
					if (artist == null || dte.getArtist().contains(artist)) {
						Logger.getGlobal().info("同梱譜面推定完了 TITLE:" + dte.getTtitle());
						return dte;
					}
				}
				return null;
			} else {
				// 検索タイトル名を短くする
				boolean b = true;
				for (char ch : delimiter) {
					if (title.indexOf(ch) != -1) {
						title = title.substring(0, title.lastIndexOf(ch));
						b = false;
						break;
					}
				}
				if (b) {
					break;
				}
			}
		}
		return null;
	}

}
