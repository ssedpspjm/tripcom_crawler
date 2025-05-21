import java.sql.*;

public class DBUtil {

    // 데이터베이스 연결을 위한 정적 메서드
    public static Connection getConnection() throws Exception {
        // 데이터베이스 연결 URL (Oracle DB 사용)
        String url = "jdbc:oracle:thin:@localhost:39161:XE";

        // 데이터베이스 사용자 이름
        String user = "CrawlingPJ";

        // 데이터베이스 사용자 비밀번호
        String password = "CrawlingPJ";

        // Oracle JDBC 드라이버 로딩
        Class.forName("oracle.jdbc.driver.OracleDriver");

        // DriverManager를 통해 데이터베이스 연결을 생성하고 반환
        return DriverManager.getConnection(url, user, password);
    }
}
