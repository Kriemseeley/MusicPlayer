package hrbust;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {
    private static String url = "jdbc:mysql://14.103.112.126:3306/musicdb?serverTimezone=Asia/Shanghai"
            + "&useUnicode=true&characterEncoding=UTF-8";
    private static String driverName = "com.mysql.cj.jdbc.Driver";
    private static String username = "navicat_user";
    private static String password = "hyrink112250";

    private Connection openConnection() throws Exception {
        Class.forName(driverName);
        return DriverManager.getConnection(url, username, password);
    }

    // 注册用户
    public boolean register(User user) throws Exception {
        String sql = "INSERT INTO user (username, password) VALUES (?, ?)";
        try (Connection conn = openConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, user.getUsername());
            pst.setString(2, user.getPassword());
            int rows = pst.executeUpdate();
            return rows > 0;
        }
    }

    // 登录校验
    public boolean login(String username, String password) throws Exception {
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        try (Connection conn = openConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }
}