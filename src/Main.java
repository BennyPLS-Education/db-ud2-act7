import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static final String url = "jdbc:mysql://localhost:3306/classicmodels";
    private static final String user = "root";
    private static final String password = "CalaClara21.";

    private static final Scanner scanner = new Scanner(System.in);
    private static final String orders = "SELECT orderNumber FROM orders JOIN classicmodels.customers c on c.customerNumber = orders.customerNumber WHERE c.customerNumber = ?";
    private static final String customerInfo = "SELECT CONCAT('COMMANDES del client ' , customerNumber, ': \\'',customerName, '\\' (', country, ')') as 'result'" +
                                               "FROM customers\n" +
                                               "WHERE customerNumber = ?;";

    public static void main(String[] args) throws SQLException {
        var conn = DriverManager.getConnection(url, user, password);

        System.out.print("De quin client (número) vols veure les comandes? ");
        int client = scanner.nextInt();
        System.out.println("-------------------------------------------------------------------- ");

        try (var stmt = conn.prepareStatement(customerInfo)) {
            stmt.setInt(1, client);
            var result = stmt.executeQuery();
            result.next();

            System.out.println(result.getString("result"));
        }

        double clientTotal = 0;
        try (var stmt = conn.prepareStatement(orders)) {
            stmt.setInt(1, client);

            var result = stmt.executeQuery();

            while (result.next()) {
                var ord = new Order(conn, result.getInt("orderNumber"));
                var orders = ord.getInfo();

                for (int i = 0; i < orders.length; i++) {

                    if (i == 0) {
                        System.out.print("\t");
                    } else if (i == orders.length - 1) {
                        System.out.print("\t\t\t");
                    } else {
                        System.out.print("\t");
                    }

                    System.out.println(orders[i]);
                }

                clientTotal += ord.getTotal();
                System.out.println();
            }
        }
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        String formattedNumber = formatter.format(clientTotal);
        System.out.println("\tTOTAL del client: " + formattedNumber + "€");
        System.out.println("-------------------------------------------------------------------- ");

        conn.close();
    }
}