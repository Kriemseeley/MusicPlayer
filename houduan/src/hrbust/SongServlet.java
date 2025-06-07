package hrbust;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation class SongServlet
 */
public class SongServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SongDao dao = new SongDao();

	protected void service(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub

		arg0.setCharacterEncoding("UTF-8");

		String name = arg0.getParameter("songname");
		String songer = arg0.getParameter("songer");
		// String songid = arg0.getParameter("songid");
		Part part = arg0.getPart("songfile");
		String fielname = part.getSubmittedFileName();

		System.out.println(name);
		System.out.println(songer);
		System.out.println(fielname);

		String songid = generateId();
		InputStream is = part.getInputStream();
		String uploadDir = arg0.getServletContext().getRealPath("");
		uploadDir = uploadDir + "/song";

		// String songurl = "http://10.61.23.235:2024/song/";
		String songurl = "/song/";
		int dotindex = fielname.indexOf(".");
		String newfielname = songid + fielname.substring(dotindex);
		songurl = songurl + newfielname;
		String uploadFile = uploadDir + "/" + newfielname;
		FileOutputStream fos = new FileOutputStream(uploadFile);
		byte[] bs = new byte[1024];
		int readlen = -1;
		while ((readlen = is.read(bs)) > 0) {
			fos.write(bs, 0, readlen);
		}
		fos.close();
		is.close();

		Song song = new Song();
		song.setId(songid);
		song.setName(name);
		song.setSinger(songer);
		song.setPath(songurl);
		song.setDuration(0); // 默认时长设为0
		song.setPlayCount(0); // 默认播放次数设为0

		try {
			dao.insert(song);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		arg1.getWriter().println("success ����");
	}

	private String generateId() {
		return System.currentTimeMillis() + "";
	}

	private String parseURLRoot(HttpServletRequest arg0) {
		String root = arg0.getScheme() + "://" + arg0.getServerName() + ":" + arg0.getServerPort();
		return root;

	}

}
