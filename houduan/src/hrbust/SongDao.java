package hrbust;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SongDao {

	private static String url = "jdbc:mysql://14.103.112.126:3306/musicdb?serverTimezone=Asia/Shanghai"
			+ "&useUnicode=true&characterEncoding=UTF-8";
	private static String driverName = "com.mysql.cj.jdbc.Driver";
	private static String username = "navicat_user";
	private static String password = "hyrink112250";

	public List<Song> query(String[] kws) throws Exception {
		String sql = "select id, songid, songname, songer, songduration, platcount, songpath from song ";
		String where1 = createWhere("songname", kws);
		String where2 = createWhere("songer", kws);
		String where = where1 + " or " + where2;
		String select = sql + " where " + where;

		System.out.println(select);

		Connection conn = openConnection();
		PreparedStatement pst = conn.prepareStatement(select);
		ResultSet rs = pst.executeQuery();

		List<Song> songlist = new ArrayList();
		while (rs.next()) {
			Song song = new Song();
			song.setId(rs.getString("songid"));
			song.setName(rs.getString("songname"));
			song.setSinger(rs.getString("songer"));
			song.setDuration(rs.getInt("songduration"));
			song.setPlayCount(rs.getInt("platcount"));
			song.setPath(rs.getString("songpath"));
			songlist.add(song);
		}

		rs.close();
		pst.close();
		conn.close();

		return songlist;
	}

	private String createWhere(String fieldname, String[] kws) {
		StringBuilder sb = new StringBuilder();
		sb.append(fieldname + " like " + "'%" + kws[0] + "%' ");
		if (kws.length > 1) {
			for (int i = 1; i < kws.length; i++) {
				sb.append(" or " + fieldname + " like " + "'%" + kws[i] + "%' ");
			}
		}
		return sb.toString();
	}

	public void increasePlatcount(String songid) throws Exception {
		String sql = "UPDATE song SET platcount = platcount + 1 WHERE songid = ?";
		Connection conn = openConnection();
		PreparedStatement pst = conn.prepareStatement(sql);
		pst.setString(1, songid);
		pst.executeUpdate();
		pst.close();
		conn.close();
	}

	public void insert(Song song) throws Exception {
		String insertsql = "insert into song(songid, songname, songer, songduration, platcount, songpath) values(?,?,?,?,?,?)";
		Connection conn = openConnection();
		PreparedStatement pst = conn.prepareStatement(insertsql);
		pst.setString(1, song.getId());
		pst.setString(2, song.getName());
		pst.setString(3, song.getSinger());
		pst.setInt(4, song.getDuration());
		pst.setInt(5, song.getPlayCount());
		pst.setString(6, song.getPath());
		pst.executeUpdate();

		pst.close();
		conn.close();
	}

	// 查询：模糊匹配并按播放次数倒序
	public List<Song> queryByKeywordOrderByPlatcount(String keyword) throws Exception {
		String sql = "SELECT songid, songname, songer, songduration, platcount, songpath " +
				"FROM song WHERE songname LIKE ? OR songer LIKE ? ORDER BY platcount DESC";
		Connection conn = openConnection();
		PreparedStatement pst = conn.prepareStatement(sql);
		String like = "%" + keyword + "%";
		pst.setString(1, like);
		pst.setString(2, like);
		ResultSet rs = pst.executeQuery();

		List<Song> songlist = new ArrayList<>();
		while (rs.next()) {
			Song song = new Song();
			song.getName();
			song.getSinger();
			song.getPath();
			songlist.add(song);
		}
		rs.close();
		pst.close();
		conn.close();
		return songlist;
	}

	private Connection openConnection() throws Exception {
		Class.forName(driverName);
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	public List<Song> searchSongs(String[] keywords) {
		List<Song> songs = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM song WHERE 1=1");
		for (String keyword : keywords) {
			sql.append(" AND (songname LIKE ? OR songer LIKE ?)");
		}
		sql.append(" ORDER BY platcount DESC");

		try (Connection conn = openConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
			int paramIndex = 1;
			for (String keyword : keywords) {
				pstmt.setString(paramIndex++, "%" + keyword + "%");
				pstmt.setString(paramIndex++, "%" + keyword + "%");
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Song song = new Song();
				song.getName();
				song.getSinger();
				song.getPath();
				songs.add(song);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return songs;
	}

}
