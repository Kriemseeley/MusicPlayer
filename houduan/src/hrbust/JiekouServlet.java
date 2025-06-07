package hrbust;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/jiekou")
public class JiekouServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    SongDao dao = new SongDao();

    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keywords = request.getParameter("kw");
        String[] kws = keywords.split("\\s+");
        System.out.println(keywords);
        System.out.println(kws.length);

        String hostandport = getHostPost(request);

        try {
            List<Song> songlist = dao.query(kws);

            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (int i = 0; i < songlist.size(); i++) {
                Song song = songlist.get(i);
                song.getPath();
                sb.append(song.toJson());
                if (i < songlist.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");

            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getHostPost(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort();
    }
}
