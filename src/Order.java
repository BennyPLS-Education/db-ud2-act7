import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class Order {

    private static boolean initialized = false;

    private static final String spanish_localization = "SET SESSION lc_time_names = 'ca_ES';";
    private static final String getOrderDetailsProcedure = """
            CREATE PROCEDURE IF NOT EXISTS `getOrderDetails`(IN number INT)
            main: BEGIN
                SELECT CONCAT(
                               LPAD(ROW_NUMBER() OVER (), 4, ' '), ') ',
                               products.productName,
                               ' (', details.quantityOrdered, ' x ',
                               details.priceEach, ' = ',
                               FORMAT(details.quantityOrdered * details.priceEach, 2), ' €)'
                       ) as 'details'
                FROM orders
                         JOIN orderdetails details on orders.orderNumber = details.orderNumber
                         JOIN products on products.productCode = details.productCode
                WHERE orders.orderNumber = number;
            END;
            """;
    private static final String getOrderTotalProcedure = """
                    CREATE PROCEDURE IF NOT EXISTS getOrderTotal(IN number INT)
                    main:
                    BEGIN
                        SELECT CONCAT('TOTAL: ', FORMAT(SUM(details.quantityOrdered * details.priceEach), 2), ' €') as 'total', FORMAT(SUM(details.quantityOrdered * details.priceEach), 2) as 'totalNumb'
                        FROM orders
                                 JOIN orderdetails details on orders.orderNumber = details.orderNumber
                                 JOIN products on products.productCode = details.productCode
                        WHERE orders.orderNumber = number;
                    END;
            """;
    private static final String getOrderInfoProcedure = """
            CREATE PROCEDURE IF NOT EXISTS `getOrderInfo`(IN number INT)
            main:
            BEGIN
                SELECT CONCAT('Commanda n. ', orders.orderNumber, ' del ', DATE_FORMAT(orders.orderDate, '%d de %M de %Y'), ' (', 'Servida en ', DAY(TIMEDIFF(orders.orderDate, orders.shippedDate)), ' dies',
                              ')') as 'info'
                FROM orders
                         JOIN customers on orders.customerNumber = customers.customerNumber
                WHERE orders.orderNumber = number;
            END;
            """;
    private final CallableStatement getOrderDetails;
    private final CallableStatement getOrderTotal;
    private final CallableStatement getOrderInfo;

    private final Connection connection;
    private final int number;

    public Order(Connection connection, int number) throws SQLException {
        this.connection = connection;
        this.number = number;

        getOrderDetails = connection.prepareCall("CALL getOrderDetails(?);");
        getOrderTotal = connection.prepareCall("CALL getOrderTotal(?);");
        getOrderInfo = connection.prepareCall("CALL getOrderInfo(?);");

        if (!initialized) {
            try (var stmt = connection.createStatement()) {
                stmt.execute(spanish_localization);
                stmt.execute(getOrderDetailsProcedure);
                stmt.execute(getOrderTotalProcedure);
                stmt.execute(getOrderInfoProcedure);
            }

            initialized = true;
        }
    }

    public String[] getInfo() throws SQLException {
        getOrderDetails.setInt(1, number);
        getOrderInfo.setInt(1, number);
        getOrderTotal.setInt(1, number);

        ArrayList<String> info = new ArrayList<>();

        var result = getOrderInfo.executeQuery();
        result.next();
        info.add(result.getString("info"));

        result = getOrderDetails.executeQuery();
        while (result.next()) info.add(result.getString("details"));

        result = getOrderTotal.executeQuery();
        result.next();
        info.add(result.getString("total"));

        return info.toArray(new String[0]);
    }

    public double getTotal() throws SQLException {
        var result = getOrderTotal.executeQuery();
        result.next();
        return Double.parseDouble(result.getString("totalNumb").replace(",", ""));
    }
}
