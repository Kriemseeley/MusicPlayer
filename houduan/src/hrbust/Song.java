package hrbust;

public class Song {

	private String id;
	private String name;
	private String singer;
	private int duration;
	private int playCount;
	private String path;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSinger() {
		return singer;
	}

	public void setSinger(String singer) {
		this.singer = singer;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getPlayCount() {
		return playCount;
	}

	public void setPlayCount(int playCount) {
		this.playCount = playCount;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String toJson() {
		String json = "{";
		json = json + "\"songname\":\"" + getName() + "\",";
		json = json + "\"songer\":\"" + getSinger() + "\",";
		json = json + "\"songpath\":\"" + getPath() + "\"";
		json = json + "}";
		return json;
	}

}
