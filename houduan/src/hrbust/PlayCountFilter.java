package hrbust;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

@WebFilter("/addcount")
public class PlayCountFilter implements Filter {
    private SongDao songDao = new SongDao();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String songid = request.getParameter("songid");
        if (songid != null) {
            try {
                songDao.increasePlatcount(songid);
            } catch (Exception e) {
                // 日志
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}