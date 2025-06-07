package hrbust;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = { "/login", "/register" })
public class LoginRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDao userDao = new UserDao();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain;charset=UTF-8");
        String path = req.getServletPath();
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if ("/login".equals(path)) {
            try {
                boolean success = userDao.login(username, password);
                if (success) {
                    resp.getWriter().write("success");
                } else {
                    resp.getWriter().write("fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().write("error");
            }
        } else if ("/register".equals(path)) {
            try {
                boolean success = userDao.register(new User(username, password));
                if (success) {
                    resp.getWriter().write("success");
                } else {
                    resp.getWriter().write("fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().write("error");
            }
        }
    }
}