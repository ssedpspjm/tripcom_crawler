import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("\n[Tripcom 항공편 시스템]\n> 원하는 작업을 입력하세요 (search, add, update, delete, exit): ");
            String cmd = sc.nextLine().trim();

            switch (cmd.toLowerCase()) {
                case "search" -> searchFlights(sc);
                case "add" -> insertFlight(sc);
                case "update" -> updateFlight(sc);
                case "delete" -> deleteFlight(sc);
                case "exit" -> {
                    System.out.println("Exiting...");
                    return;
                }
                default -> System.out.println("❌ 잘못된 입력입니다. 다시 시도하세요.");
            }
        }
    }

    static void searchFlights(Scanner sc) {
        System.out.print("검색할 도착지를 입력하세요 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();

        String table = "tripcom_flights_" + dest;
        String sql = "SELECT * FROM " + table + " ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n✈ " + dest + " 항공편 목록:");
            while (rs.next()) {
                System.out.printf("[%d] %s | %s | ₩%,d | %s → %s\n",
                        rs.getInt("id"),
                        rs.getString("airline"),
                        rs.getString("flight_time"),
                        rs.getInt("price"),
                        rs.getString("departure"),
                        rs.getString("arrival"));
            }

        } catch (SQLException e) {
            System.out.println("❌ 해당 목적지 테이블이 존재하지 않거나 조회 실패");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void insertFlight(Scanner sc) {
        System.out.print("추가할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();
        String table = "tripcom_flights_" + dest;

        try (Connection conn = DBUtil.getConnection()) {
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

            String sql = "INSERT INTO " + table + " (id, airline, flight_time, price, flight_number, departure, arrival) " +
                         "VALUES (" + table + "_seq.NEXTVAL, ?, ?, ?, NULL, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, airline);
                ps.setString(2, time);
                ps.setInt(3, price);
                ps.setString(4, dep);
                ps.setString(5, arr);
                ps.executeUpdate();
                System.out.println("✅ 항공편 추가 완료");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void updateFlight(Scanner sc) {
        System.out.print("수정할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();
        String table = "tripcom_flights_" + dest;

        try (Connection conn = DBUtil.getConnection()) {
            System.out.print("수정할 항공편 ID: ");
            int id = Integer.parseInt(sc.nextLine());
            System.out.print("새로운 가격: ");
            int newPrice = Integer.parseInt(sc.nextLine());

            String sql = "UPDATE " + table + " SET price = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newPrice);
                ps.setInt(2, id);
                int updated = ps.executeUpdate();
                System.out.println("🔄 수정 완료 (행 수: " + updated + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void deleteFlight(Scanner sc) {
        System.out.print("삭제할 도착지 테이블 입력 (예: KIX, HKG, SIN): ");
        String dest = sc.nextLine().toUpperCase();
        String table = "tripcom_flights_" + dest;

        try (Connection conn = DBUtil.getConnection()) {
            System.out.print("삭제할 항공편 ID: ");
            int id = Integer.parseInt(sc.nextLine());

            String sql = "DELETE FROM " + table + " WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                System.out.println("🗑️ 삭제 완료 (행 수: " + deleted + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
