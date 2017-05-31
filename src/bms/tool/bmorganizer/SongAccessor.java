package bms.tool.bmorganizer;

public interface SongAccessor {
	
	public Song getSong(String[] hashes);

	public Song[] getSong(String title, String artist);

}
