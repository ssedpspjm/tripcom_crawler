import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in); // 입력을 위한 Scanner 생성성

        while (true) { // exit 입력시 까지 반복복
            System.out.print("\n[Tripcom 항공편 시스템]\n> 원하는 작업을 입력하세요 (search / add / update / delete / exit): ");
            String cmd = sc.nextLine().trim(); // 명령어 앞뒤 공백 제거

            // 입력받은 명령어에 따라 해당 메서드를 호출하는 switch-case 문
            switch (cmd.toLowerCase()) { // 소문자 변환환
                case "search" -> searchFlights(sc); // 항공편 조회
                case "add" -> insertFlight(sc);     // 항공편 추가
                case "update" -> updateFlight(sc);  // 항공편 수정
                case "delete" -> deleteFlight(sc);  // 항공편 삭제
                case "exit" -> {                    // 프로그램 종료
                    System.out.println("Exiting...");
                    return;                          // main 메서드 종료
                }
                default -> System.out.println(" 잘못된 입력입니다. 다시 시도하세요."); // 잘못된 명령어 처리
            }
        }
    }

    static void searchFlights(Scanner sc) { // 항공편 조회 메서드
        System.out.print("검색할 도착지를 입력하세요 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase(); // 입력받은 내용 대문자 변환환

        String table = "tripcom_flights_" + dest; // 데이터베이스 테이블 이름 설정
        String sql = "SELECT * FROM " + table + " ORDER BY id"; // SQL 쿼리 생성

        try (Connection conn = DBUtil.getConnection(); // DB 연결
             Statement stmt = conn.createStatement();  // SQL 실행할 statement 객체 생성
             ResultSet rs = stmt.executeQuery(sql)) {  // SQL 쿼리 실행 후 결과 저장

            System.out.println("\n✈ " + dest + " 항공편 목록:");
            while (rs.next()) { // 결과 행 반복 조회
                System.out.printf("[%d] %s | %s | ₩%,d | %s → %s\n",
                        rs.getInt("id"),                 // 항공편 고유번호
                        rs.getString("airline"),          // 항공사 이름
                        rs.getString("flight_time"),      // 항공편 운항 시간
                        rs.getInt("price"),              // 항공편 가격
                        rs.getString("departure"),        // 출발지
                        rs.getString("arrival"));         // 도착지
            }

        } catch (SQLException e) { // SQL 관련 예외 처리
            System.out.println(" 해당 목적지 테이블이 존재하지 않거나 조회 실패");
        } catch (Exception e) {    // 기타 예외 처리
            e.printStackTrace();   // 오류 상세 출력
        }
    }

    static void insertFlight(Scanner sc) { // 항공편 추가 메서드
        System.out.print("추가할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase(); // 입력받아 대문자 변환
        String table = "tripcom_flights_" + dest;  // 테이블 이름 생성

        try (Connection conn = DBUtil.getConnection()) { // DB 연결
            // 사용자로부터 항공편 정보 입력 받기
            System.out.print("항공사: ");
            String airline = sc.nextLine();
            System.out.print("운항시간 (예: 07:30 → 09:20): ");
            String time = sc.nextLine();
            System.out.print("가격: ");
            int price = Integer.parseInt(sc.nextLine());
            System.out.print("출발지: ");
            String dep = sc.nextLine();
            System.out.print("도착지: ");
            String arr = sc.nextLine();

            // 데이터 삽입을 위한 SQL 쿼리 생성
            String sql = "INSERT INTO " + table + " (id, airline, flight_time, price, flight_number, departure, arrival) " +
                         "VALUES (" + table + "_seq.NEXTVAL, ?, ?, ?, NULL, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) { // PreparedStatement 생성
                ps.setString(1, airline); // 항공사 이름 설정
                ps.setString(2, time);    // 운항 시간 설정
                ps.setInt(3, price);      // 가격 설정
                ps.setString(4, dep);     // 출발지 설정
                ps.setString(5, arr);     // 도착지 설정
                ps.executeUpdate();       // SQL 실행
                System.out.println(" 항공편 추가 완료");
            }

        } catch (Exception e) { // 예외 처리
            e.printStackTrace();
        }
    }

    static void updateFlight(Scanner sc) { // 항공편 수정 메서드
        System.out.print("수정할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();
        String table = "tripcom_flights_" + dest;

        try (Connection conn = DBUtil.getConnection()) { // DB 연결
            System.out.print("수정할 항공편 ID: ");
            int id = Integer.parseInt(sc.nextLine());
            System.out.print("새로운 가격: ");
            int newPrice = Integer.parseInt(sc.nextLine());

            String sql = "UPDATE " + table + " SET price = ? WHERE id = ?"; // 가격 수정 SQL
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newPrice); // 새 가격 설정
                ps.setInt(2, id);       // 항공편 ID 설정
                int updated = ps.executeUpdate(); // 실행 결과 반환
                System.out.println("🔄 수정 완료 (행 수: " + updated + ")");
            }

        } catch (Exception e) { // 예외 처리
            e.printStackTrace();
        }
    }

    static void deleteFlight(Scanner sc) { // 항공편 삭제 메서드
        System.out.print("삭제할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();
        String table = "tripcom_flights_" + dest;

        try (Connection conn = DBUtil.getConnection()) { // DB 연결
            System.out.print("삭제할 항공편 ID: ");
            int id = Integer.parseInt(sc.nextLine());

            String sql = "DELETE FROM " + table + " WHERE id = ?"; // 항공편 삭제 SQL
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id); // 삭제할 항공편 ID 설정
                int deleted = ps.executeUpdate(); // 실행 결과 반환
                System.out.println("🗑️ 삭제 완료 (행 수: " + deleted + ")");
            }

        } catch (Exception e) { // 예외 처리
            e.printStackTrace();
        }
    }
}
