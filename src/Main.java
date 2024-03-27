import java.sql.*;
import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        MySQL database = new MySQL();
        try (Connection connection = database.connect()) {

        	String dni = null;
        	while (dni == null) {
        	    dni = loginLoop(connection);
        	}

            while (actionLoop(connection, dni)) {
            	continue;
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    private static String loginLoop(Connection connection) throws SQLException {
        System.out.println("Enter DNI:");
        String dni = scanner.nextLine();

        System.out.println("Enter password:");
        String password = scanner.nextLine();

        String loginSQL = "SELECT * FROM bankapp WHERE dni = ? AND password = ?";

        try (PreparedStatement statement = connection.prepareStatement(loginSQL)) {
            statement.setString(1, dni);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println("Login successful.");
                    return dni;
                } else {
                    System.out.println("Login failed. DNI or password incorrect.");
                    return null;
                }
            }
        }
    }

    private static boolean actionLoop(Connection connection, String ownDNI) {
        System.out.println("Do you want to check your balance (c), send money (s) or disconnect (d)?");
        char choice = scanner.next().charAt(0);
        scanner.nextLine();

        switch (choice) {
            case 'c':
                float balance = moneyGot(connection, ownDNI);
                System.out.println("Your balance is: " + balance);
                return true;
            case 's':
                sendMoneyPrompt(connection, ownDNI);
                return true;
            case 'd':
                return false;
            default:
                System.out.println("Invalid choice.");
                return true;
        }
    }

    private static void sendMoneyPrompt(Connection connection, String ownDNI) {
        System.out.println("Who do you want to send money to? Enter their DNI:");
        String recipient = scanner.nextLine();

        if (!doesUserExist(connection, recipient)) {
            System.out.println("User does not exist.");
            return;
        }

        System.out.println("How much money do you want to send?");
        float amount = scanner.nextFloat();
        scanner.nextLine();

        if (amount <= 0) {
            System.out.println("Amount must be greater than 0.");
            return;
        }

        if (ownDNI.equals(recipient)) {
            System.out.println("You cannot send money to yourself.");
            return;
        }

        if (!hasEnoughMoney(connection, ownDNI, amount)) {
            System.out.println("You do not have enough money.");
            return;
        }

        try {
            sendMoney(connection, ownDNI, recipient, amount);
        } catch (SQLException e) {
            System.out.println("Error sending money: " + e.getMessage());
        }
    }

    private static boolean doesUserExist(Connection connection, String dni) {
        String query = "SELECT * FROM bankapp WHERE dni = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, dni);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.out.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    private static boolean hasEnoughMoney(Connection connection, String ownDNI, float amount) {
        String query = "SELECT savings FROM bankapp WHERE dni = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, ownDNI);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    float savings = resultSet.getFloat("savings");
                    return savings >= amount;
                } else {
                    System.out.println("User with DNI " + ownDNI + " not found.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking user savings: " + e.getMessage());
            return false;
        }
    }
    
    private static float moneyGot(Connection connection, String ownDNI) {
        String query = "SELECT savings FROM bankapp WHERE dni = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, ownDNI);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getFloat("savings");
                } else {
                    System.out.println("User with DNI " + ownDNI + " not found.");
                    return 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking user savings: " + e.getMessage());
            return 0;
        }
    }

    private static void sendMoney(Connection connection, String senderDNI, String recipientDNI, float amount) throws SQLException {
        String updateSQL = "UPDATE bankapp SET savings = savings - ? WHERE dni = ?";
        try (PreparedStatement senderStatement = connection.prepareStatement(updateSQL)) {
            senderStatement.setFloat(1, amount);
            senderStatement.setString(2, senderDNI);
            senderStatement.executeUpdate();
        }
        try (PreparedStatement recipientStatement = connection.prepareStatement(updateSQL)) {
            recipientStatement.setFloat(1, -amount);
            recipientStatement.setString(2, recipientDNI);
            recipientStatement.executeUpdate();
        }
        System.out.println("Money sent successfully.");
    }

}
