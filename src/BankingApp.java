import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BankingApp {
    private JFrame frame;
    private JTextField usernameField, amountField, recipientField;
    private JPasswordField passwordField;
    private JTextArea transactionHistoryArea;
    private Connection connection;

    // MAIN METHOD
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankingApp().frame.setVisible(true));
    }

    // CONSTRUCTOR
    public BankingApp() {
        initialize();
        connectToDatabase();
    }

    // GUI INITIALIZATION
    private void initialize() {
        frame = new JFrame("Banking Application");
        frame.setSize(600, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // Panel for input fields
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Account Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        inputPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        inputPanel.add(passwordField, gbc);

        // Amount
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField(15);
        inputPanel.add(amountField, gbc);

        // Recipient Username
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Recipient:"), gbc);
        gbc.gridx = 1;
        recipientField = new JTextField(15);
        inputPanel.add(recipientField, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");
        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");
        JButton transferButton = new JButton("Transfer");

        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(depositButton);
        buttonPanel.add(withdrawButton);
        buttonPanel.add(transferButton);

        // Transaction History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Transaction History"));
        transactionHistoryArea = new JTextArea(10, 40);
        transactionHistoryArea.setEditable(false);
        historyPanel.add(new JScrollPane(transactionHistoryArea), BorderLayout.CENTER);

        // Adding components to frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(historyPanel, BorderLayout.SOUTH);

        // Button Listeners
        registerButton.addActionListener(e -> register());
        loginButton.addActionListener(e -> login());
        depositButton.addActionListener(e -> depositFunds());
        withdrawButton.addActionListener(e -> withdrawFunds());
        transferButton.addActionListener(e -> transferFunds());
    }

    // REGISTER NEW ACCOUNT
    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String balanceText = amountField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || balanceText.isEmpty()) {
            showMessage("All fields are required!");
            return;
        }

        try {
            double balance = Double.parseDouble(balanceText);
            String hashedPassword = hashPassword(password);

            String query = "INSERT INTO accounts (username, password, balance) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setDouble(3, balance);
                stmt.executeUpdate();
            }
            showMessage("Account registered successfully!");
        } catch (NumberFormatException e) {
            showMessage("Invalid balance amount.");
        } catch (SQLException e) {
            showMessage("Username already exists or database error.");
        }
    }

    // LOGIN METHOD
    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Enter username and password.");
            return;
        }

        try {
            String query = "SELECT id, password FROM accounts WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        if (storedPassword.equals(hashPassword(password))) {
                            int accountId = rs.getInt("id");
                            showMessage("Login successful!");
                            displayAccountBalance(accountId);
                            displayTransactionHistory(accountId);
                        } else {
                            showMessage("Incorrect password.");
                        }
                    } else {
                        showMessage("Account not found.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Database error.");
        }
    }

    // DEPOSIT METHOD
    private void depositFunds() {
        updateBalance("Deposit");
    }

    // WITHDRAW METHOD
    private void withdrawFunds() {
        updateBalance("Withdraw");
    }

    // COMMON BALANCE UPDATE METHOD
    private void updateBalance(String transactionType) {
        String username = usernameField.getText().trim();
        String amountText = amountField.getText().trim();

        if (username.isEmpty() || amountText.isEmpty()) {
            showMessage("Enter username and amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String query = "UPDATE accounts SET balance = balance " +
                    (transactionType.equals("Deposit") ? "+" : "-") + " ? WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setDouble(1, amount);
                stmt.setString(2, username);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    showMessage(transactionType + " successful!");
                } else {
                    showMessage("Account not found or insufficient balance.");
                }
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid amount.");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Database error.");
        }
    }

    // TRANSFER METHOD
    private void transferFunds() {
        String sender = usernameField.getText().trim();
        String recipient = recipientField.getText().trim();
        String amountText = amountField.getText().trim();

        if (sender.isEmpty() || recipient.isEmpty() || amountText.isEmpty()) {
            showMessage("All fields are required!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            connection.setAutoCommit(false);

            // Deduct from sender
            String deductQuery = "UPDATE accounts SET balance = balance - ? WHERE username = ? AND balance >= ?";
            try (PreparedStatement deductStmt = connection.prepareStatement(deductQuery)) {
                deductStmt.setDouble(1, amount);
                deductStmt.setString(2, sender);
                deductStmt.setDouble(3, amount);
                int senderUpdated = deductStmt.executeUpdate();
                if (senderUpdated == 0) {
                    showMessage("Insufficient balance.");
                    connection.rollback();
                    return;
                }
            }

            // Add to recipient
            String addQuery = "UPDATE accounts SET balance = balance + ? WHERE username = ?";
            try (PreparedStatement addStmt = connection.prepareStatement(addQuery)) {
                addStmt.setDouble(1, amount);
                addStmt.setString(2, recipient);
                int recipientUpdated = addStmt.executeUpdate();
                if (recipientUpdated == 0) {
                    showMessage("Recipient not found.");
                    connection.rollback();
                    return;
                }
            }

            // Log transactions
            int senderAccountId = getAccountId(sender);
            int recipientAccountId = getAccountId(recipient);

            logTransaction(senderAccountId, "Transfer Out", amount);
            logTransaction(recipientAccountId, "Transfer In", amount);

            connection.commit();
            showMessage("Transfer successful!");
        } catch (NumberFormatException e) {
            showMessage("Invalid amount.");
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // GET ACCOUNT ID
    private int getAccountId(String username) throws SQLException {
        String query = "SELECT id FROM accounts WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Account not found: " + username);
                }
            }
        }
    }

    // LOG TRANSACTIONS
    private void logTransaction(int accountId, String type, double amount) throws SQLException {
        String query = "INSERT INTO transactions (account_id, transaction_type, amount, transaction_date) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        }
    }

    // DISPLAY BALANCE
    private void displayAccountBalance(int accountId) {
        try {
            String query = "SELECT balance FROM accounts WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double balance = rs.getDouble("balance");
                        transactionHistoryArea.append("Current Balance: ₹" + balance + "\n");
                    } else {
                        showMessage("Account not found.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Database error.");
        }
    }

    // DISPLAY TRANSACTION HISTORY
    private void displayTransactionHistory(int accountId) {
        try {
            String query = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    transactionHistoryArea.setText(""); // Clear previous data
                    while (rs.next()) {
                        String transactionType = rs.getString("transaction_type");
                        double amount = rs.getDouble("amount");
                        String date = rs.getString("transaction_date");
                        transactionHistoryArea.append(transactionType + ": ₹" + amount + " on " + date + "\n");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Database error.");
        }
    }

    // CONNECT TO DATABASE
    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC",
                "root",
                "root"
            );
            System.out.println("✅ Connected to database successfully!");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(frame, "MySQL JDBC Driver not found!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database connection failed: " + e.getMessage());
        }
    }

    // PASSWORD HASHING
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // SHOW MESSAGE DIALOG
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }
}
