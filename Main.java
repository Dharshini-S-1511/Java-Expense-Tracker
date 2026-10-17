import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * EXPENSE TRACKER - Single File Java Swing Application
 *
 * Modules:
 * 1. User Authentication
 * 2. Income & Expense Management
 * 3. Category Management
 * 4. Budget Management
 * 5. Reports & Analytics - Daily / Weekly / Monthly
 *
 * Run:
 *   javac Main.java
 *   java Main
 */
public class Main {

    // ==================== DATA CLASSES ====================

    static class Transaction implements Serializable {
        private static final long serialVersionUID = 2L;
        String type;
        double amount;
        String category;
        LocalDate date;
        String description;

        Transaction(String type, double amount, String category, LocalDate date, String description) {
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.date = date;
            this.description = description;
        }
    }

    static class UserData implements Serializable {
        private static final long serialVersionUID = 2L;
        String username;
        String password;
        ArrayList<Transaction> transactions = new ArrayList<>();
        ArrayList<String> categories = new ArrayList<>(Arrays.asList(
                "Food", "Travel", "Shopping", "Bills", "Health"
        ));
        double monthlyBudget = 10000.0;

        UserData(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    static class AppData implements Serializable {
        private static final long serialVersionUID = 2L;
        HashMap<String, UserData> users = new HashMap<>();
    }

    // ==================== APPLICATION STATE ====================

    static final String DATA_FILE = "expense_tracker.dat";
    static AppData appData = new AppData();
    static UserData currentUser;

    static JFrame dashboardFrame;
    static JLabel dashboardIncome;
    static JLabel dashboardExpense;
    static JLabel dashboardBalance;
    static JLabel dashboardBudget;
    static JLabel dashboardBudgetUsed;
    static JLabel dashboardBudgetStatus;
    static JProgressBar dashboardProgress;
    static DashboardChartPanel dashboardChart;

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== THEME ====================

    static final Color NAV = new Color(24, 58, 92);
    static final Color NAV_DARK = new Color(18, 44, 70);
    static final Color GREEN = new Color(46, 125, 50);
    static final Color GREEN_LIGHT = new Color(232, 245, 233);
    static final Color BLUE = new Color(25, 118, 210);
    static final Color BLUE_LIGHT = new Color(227, 242, 253);
    static final Color ORANGE = new Color(239, 125, 34);
    static final Color ORANGE_LIGHT = new Color(255, 243, 224);
    static final Color RED = new Color(198, 40, 40);
    static final Color RED_LIGHT = new Color(255, 235, 238);
    static final Color PURPLE = new Color(123, 31, 162);
    static final Color BG = new Color(245, 247, 249);
    static final Color TEXT = new Color(38, 50, 56);
    static final Color MUTED = new Color(105, 115, 120);

    // ==================== MAIN ====================

    public static void main(String[] args) {
        loadData();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            showLogin();
        });
    }

    // ==================== STORAGE ====================

    static void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (obj instanceof AppData) appData = (AppData) obj;
        } catch (Exception e) {
            appData = new AppData();
        }
    }

    static void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(appData);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to save data.\n" + e.getMessage(),
                    "Storage Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== CALCULATIONS ====================

    static double totalIncome() {
        if (currentUser == null) return 0;
        double total = 0;
        for (Transaction t : currentUser.transactions)
            if ("Income".equals(t.type)) total += t.amount;
        return total;
    }

    static double totalExpense() {
        if (currentUser == null) return 0;
        double total = 0;
        for (Transaction t : currentUser.transactions)
            if ("Expense".equals(t.type)) total += t.amount;
        return total;
    }

    static double balance() {
        return totalIncome() - totalExpense();
    }

    static String money(double value) {
        return String.format(Locale.US, "₹ %.2f", value);
    }

    static String compactMoney(double value) {
        return String.format(Locale.US, "₹%.0f", value);
    }

    static String budgetStatus() {
        double spent = totalExpense();
        if (spent > currentUser.monthlyBudget) return "Budget Exceeded";
        if (spent >= currentUser.monthlyBudget * 0.8) return "Warning: 80% Used";
        return "Within Budget";
    }

    // ==================== COMMON GUI ====================

    static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", style, size));
        l.setForeground(color);
        return l;
    }

    static JLabel centerLabel(String text, int size, int style, Color color) {
        JLabel l = label(text, size, style, color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    static JPanel whiteCard() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 228, 232)),
                new EmptyBorder(14, 14, 14, 14)
        ));
        return p;
    }

    static void styleButton(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(9, 14, 9, 14));
    }

    static void stylePrimary(JButton b) { styleButton(b, GREEN, Color.WHITE); }
    static void styleBlue(JButton b) { styleButton(b, BLUE, Color.WHITE); }
    static void styleDanger(JButton b) { styleButton(b, RED, Color.WHITE); }
    static void styleNeutral(JButton b) { styleButton(b, new Color(90, 105, 115), Color.WHITE); }

    static JLabel formLabel(String text) {
        return label(text, 11, Font.BOLD, new Color(75, 85, 90));
    }

    static JPanel moduleRoot(String title, String subtitle) {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAV);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, 21, Font.BOLD, Color.WHITE));
        text.add(Box.createVerticalStrut(5));
        text.add(label(subtitle, 12, Font.PLAIN, new Color(215, 228, 236)));
        header.add(text, BorderLayout.WEST);

        JLabel mark = centerLabel("₹", 32, Font.BOLD, new Color(129, 199, 132));
        header.add(mark, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        return root;
    }

    static JDialog createModuleDialog(JFrame parent, String title, int width, int height) {
        JDialog d = new JDialog(parent, title, true);
        d.setSize(width, height);
        d.setMinimumSize(new Dimension(Math.min(width, 650), Math.min(height, 500)));
        d.setLocationRelativeTo(parent);
        return d;
    }

    static void keepDashboardNormal(JFrame parent) {
        if (parent != null) {
            parent.setState(JFrame.NORMAL);
            parent.setVisible(true);
            parent.toFront();
            parent.requestFocus();
        }
    }

    static void showSuccess(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showWarning(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Please Check", JOptionPane.WARNING_MESSAGE);
    }

    static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ==================== LOGIN ====================

    static void showLogin() {
        JFrame frame = new JFrame("Expense Tracker - Login");
        frame.setSize(1000, 620);
        frame.setMinimumSize(new Dimension(900, 560));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        GradientPanel bg = new GradientPanel(new Color(18, 52, 86), GREEN);
        bg.setLayout(new GridBagLayout());

        JPanel card = new JPanel(new GridLayout(1, 2));
        card.setPreferredSize(new Dimension(860, 500));
        card.setBackground(Color.WHITE);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(NAV);
        left.setBorder(new EmptyBorder(42, 40, 42, 40));

        JLabel logo = centerLabel("₹", 56, Font.BOLD, new Color(129, 199, 132));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel e = label("EXPENSE", 34, Font.BOLD, Color.WHITE);
        JLabel t = label("TRACKER", 34, Font.BOLD, new Color(129, 199, 132));
        e.setAlignmentX(Component.LEFT_ALIGNMENT); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel slogan = label("SMARTER MONEY. BETTER CONTROL.", 13, Font.BOLD, new Color(215, 228, 236));
        slogan.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea desc = new JTextArea(
                "Track income and expenses, manage your budget,\n" +
                "organize categories and understand your\n" +
                "spending habits from one simple dashboard."
        );
        desc.setOpaque(false); desc.setEditable(false); desc.setForeground(new Color(225,235,242));
        desc.setFont(new Font("Arial", Font.PLAIN, 14));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel features = new JPanel(new GridLayout(4,1,0,9));
        features.setOpaque(false); features.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] fs = {"✓  Income & Expense Tracking", "✓  Smart Budget Monitoring", "✓  Custom Categories", "✓  Reports & Spending Analytics"};
        for (String s : fs) features.add(label(s, 13, Font.PLAIN, Color.WHITE));

        left.add(logo); left.add(Box.createVerticalStrut(3)); left.add(e); left.add(t);
        left.add(Box.createVerticalStrut(8)); left.add(slogan); left.add(Box.createVerticalStrut(25));
        left.add(desc); left.add(Box.createVerticalStrut(25)); left.add(features); left.add(Box.createVerticalGlue());
        left.add(label("Personal Finance Management System", 11, Font.PLAIN, new Color(190,210,220)));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(Color.WHITE);
        right.setBorder(new EmptyBorder(55, 55, 45, 55));

        JLabel welcome = label("Welcome Back!", 29, Font.BOLD, TEXT);
        JLabel loginText = label("Sign in to continue to your dashboard", 13, Font.PLAIN, MUTED);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT); loginText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        styleTextField(username); styleTextField(password);
        username.setAlignmentX(Component.LEFT_ALIGNMENT); password.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox showPass = new JCheckBox("Show password");
        showPass.setFont(new Font("Arial", Font.PLAIN, 11)); showPass.setBackground(Color.WHITE); showPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPass.addActionListener(x -> password.setEchoChar(showPass.isSelected() ? (char)0 : '•'));

        JButton login = new JButton("LOGIN  →"); styleButton(login, GREEN, Color.WHITE); login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); login.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton register = new JButton("Create New Account"); styleButton(register, Color.WHITE, GREEN); register.setBorder(BorderFactory.createLineBorder(GREEN)); register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); register.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(welcome); right.add(Box.createVerticalStrut(7)); right.add(loginText); right.add(Box.createVerticalStrut(30));
        right.add(formLabel("USERNAME")); right.add(Box.createVerticalStrut(7)); right.add(username);
        right.add(Box.createVerticalStrut(17)); right.add(formLabel("PASSWORD")); right.add(Box.createVerticalStrut(7)); right.add(password);
        right.add(Box.createVerticalStrut(6)); right.add(showPass); right.add(Box.createVerticalStrut(20));
        right.add(login); right.add(Box.createVerticalStrut(13)); right.add(register); right.add(Box.createVerticalGlue());
        right.add(label("New here? Create an account to get started.", 11, Font.PLAIN, MUTED));

        card.add(left); card.add(right); bg.add(card); frame.add(bg);

        Runnable doLogin = () -> {
            String u = username.getText().trim();
            String p = new String(password.getPassword());
            if (u.isEmpty() || p.isEmpty()) { showWarning(frame, "Please enter username and password."); return; }
            UserData user = appData.users.get(u);
            if (user != null && user.password.equals(p)) {
                currentUser = user;
                frame.dispose();
                showDashboard();
            } else showError(frame, "Invalid username or password.");
        };
        login.addActionListener(x -> doLogin.run());
        password.addActionListener(x -> doLogin.run());
        register.addActionListener(x -> showRegister(frame));

        frame.setVisible(true);
    }

    static void styleTextField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 15));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205,215,220)),
                new EmptyBorder(5,12,5,12)
        ));
    }

    static class GradientPanel extends JPanel {
        Color start, end;
        GradientPanel(Color start, Color end) { this.start = start; this.end = end; }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0,0,start,getWidth(),getHeight(),end));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.dispose();
        }
    }

    // ==================== REGISTER ====================

    static void showRegister(JFrame loginFrame) {
        JDialog dialog = new JDialog(loginFrame, "Create Account", true);
        dialog.setSize(500, 430); dialog.setLocationRelativeTo(loginFrame);

        JPanel root = moduleRoot("CREATE ACCOUNT", "Create your personal Expense Tracker account.");
        JPanel card = whiteCard(); card.setLayout(new GridLayout(5,2,12,14));
        JTextField user = new JTextField(); JPasswordField pass = new JPasswordField(); JPasswordField confirm = new JPasswordField();
        styleTextField(user); styleTextField(pass); styleTextField(confirm);
        JButton create = new JButton("CREATE ACCOUNT"); stylePrimary(create);
        JButton cancel = new JButton("CANCEL"); styleNeutral(cancel);
        card.add(formLabel("USERNAME")); card.add(user);
        card.add(formLabel("PASSWORD")); card.add(pass);
        card.add(formLabel("CONFIRM PASSWORD")); card.add(confirm);
        card.add(new JLabel()); card.add(create);
        card.add(new JLabel()); card.add(cancel);
        root.add(card, BorderLayout.CENTER); dialog.add(root);

        create.addActionListener(x -> {
            String u = user.getText().trim(); String p = new String(pass.getPassword()); String c = new String(confirm.getPassword());
            if (u.isEmpty() || p.isEmpty()) { showWarning(dialog, "Username and password are required."); return; }
            if (!p.equals(c)) { showWarning(dialog, "Passwords do not match."); return; }
            if (appData.users.containsKey(u)) { showWarning(dialog, "Username already exists."); return; }
            appData.users.put(u, new UserData(u,p)); saveData();
            showSuccess(dialog, "Account created successfully. You can now login."); dialog.dispose();
        });
        cancel.addActionListener(x -> dialog.dispose());
        dialog.setVisible(true);
    }

    // ==================== DASHBOARD ====================

    static void showDashboard() {
        dashboardFrame = new JFrame("Expense Tracker - Dashboard");
        dashboardFrame.setSize(1200, 800);
        dashboardFrame.setMinimumSize(new Dimension(1050, 720));
        dashboardFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dashboardFrame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(createDashboardHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 24, 10, 24));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel summary = createDashboardSummary();
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setPreferredSize(new Dimension(1100, 92));
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        body.add(summary);
        body.add(Box.createVerticalStrut(12));

        JPanel middle = new JPanel(new GridLayout(1, 2, 14, 0));
        middle.setOpaque(false);
        middle.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.setPreferredSize(new Dimension(1100, 235));
        middle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 235));
        middle.add(createSpendingCard());
        middle.add(createBudgetCard());
        body.add(middle);
        body.add(Box.createVerticalStrut(12));

        JPanel modules = new JPanel(new GridLayout(2, 2, 14, 12));
        modules.setOpaque(false);
        modules.setAlignmentX(Component.LEFT_ALIGNMENT);
        modules.setPreferredSize(new Dimension(1100, 190));
        modules.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        modules.add(createModuleCard("01", "INCOME & EXPENSE", "Record and manage transactions", GREEN_LIGHT, GREEN,
                () -> showTransactionWindow(dashboardFrame)));
        modules.add(createModuleCard("02", "CATEGORIES", "Manage spending categories", BLUE_LIGHT, BLUE,
                () -> showCategoryWindow(dashboardFrame)));
        modules.add(createModuleCard("03", "BUDGET", "Set and monitor monthly budget", ORANGE_LIGHT, ORANGE,
                () -> showBudgetWindow(dashboardFrame)));
        modules.add(createModuleCard("04", "REPORTS & ANALYTICS", "View daily, weekly and monthly reports", new Color(243,235,249), PURPLE,
                () -> showReportWindow(dashboardFrame)));
        body.add(modules);

        root.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(2, 24, 10, 24));
        footer.add(label("Personal Finance Management System  •  Data is stored locally", 11, Font.PLAIN, MUTED), BorderLayout.WEST);
        JButton logout = new JButton("LOGOUT");
        styleButton(logout, RED_LIGHT, RED);
        logout.setPreferredSize(new Dimension(100, 34));
        footer.add(logout, BorderLayout.EAST);
        logout.addActionListener(x -> logout());
        root.add(footer, BorderLayout.SOUTH);

        dashboardFrame.add(root);
        dashboardFrame.setVisible(true);
        refreshDashboard();
    }

    static JPanel createDashboardHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAV);
        header.setBorder(new EmptyBorder(16, 26, 16, 26));
        header.setPreferredSize(new Dimension(100, 96));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("₹  EXPENSE TRACKER", 28, Font.BOLD, Color.WHITE));
        left.add(Box.createVerticalStrut(3));
        left.add(label("Personal Finance Management Dashboard", 12, Font.PLAIN, new Color(205,220,230)));
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(2, 18, 0, 0));
        JLabel welcome = centerLabel("Welcome, " + currentUser.username, 18, Font.BOLD, Color.WHITE);
        welcome.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel date = centerLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 11, Font.PLAIN, new Color(205,220,230));
        date.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(welcome);
        right.add(Box.createVerticalStrut(5));
        right.add(date);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    static JPanel createDashboardSummary() {
        JPanel p = new JPanel(new GridLayout(1, 4, 12, 0));
        p.setOpaque(false);
        dashboardIncome = centerLabel("", 19, Font.BOLD, GREEN);
        dashboardExpense = centerLabel("", 19, Font.BOLD, RED);
        dashboardBalance = centerLabel("", 19, Font.BOLD, BLUE);
        dashboardBudget = centerLabel("", 19, Font.BOLD, ORANGE);
        p.add(summaryCard("TOTAL INCOME", dashboardIncome, GREEN_LIGHT));
        p.add(summaryCard("TOTAL EXPENSE", dashboardExpense, RED_LIGHT));
        p.add(summaryCard("BALANCE", dashboardBalance, BLUE_LIGHT));
        p.add(summaryCard("MONTHLY BUDGET", dashboardBudget, ORANGE_LIGHT));
        return p;
    }

    static JPanel summaryCard(String title, JLabel value, Color bg) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 225, 228)),
                new EmptyBorder(10, 10, 10, 10)));
        p.add(centerLabel(title, 10, Font.BOLD, MUTED), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    static JPanel createSpendingCard() {
        JPanel p = whiteCard();
        p.setLayout(new BorderLayout(8, 4));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("SPENDING OVERVIEW", 16, Font.BOLD, TEXT), BorderLayout.WEST);
        top.add(label("All recorded expenses", 11, Font.PLAIN, MUTED), BorderLayout.EAST);
        dashboardChart = new DashboardChartPanel();
        p.add(top, BorderLayout.NORTH);
        p.add(dashboardChart, BorderLayout.CENTER);
        return p;
    }

    static JPanel createBudgetCard() {
        JPanel p = whiteCard();
        p.setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("BUDGET STATUS", 16, Font.BOLD, TEXT), BorderLayout.WEST);
        dashboardBudgetStatus = label("", 11, Font.BOLD, GREEN);
        top.add(dashboardBudgetStatus, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        dashboardBudgetUsed = centerLabel("", 21, Font.BOLD, TEXT);
        dashboardBudgetUsed.setAlignmentX(Component.CENTER_ALIGNMENT);
        dashboardProgress = new JProgressBar(0, 100);
        dashboardProgress.setStringPainted(true);
        dashboardProgress.setPreferredSize(new Dimension(350, 25));
        dashboardProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        dashboardProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel note = centerLabel("Expense used from the monthly budget", 11, Font.PLAIN, MUTED);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(GREEN_LIGHT);
        info.setBorder(new EmptyBorder(9, 12, 9, 12));
        info.add(label("i", 14, Font.BOLD, GREEN), BorderLayout.WEST);
        info.add(label("  Stay within your planned monthly spending limit", 11, Font.PLAIN, GREEN), BorderLayout.CENTER);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(dashboardBudgetUsed);
        center.add(Box.createVerticalStrut(12));
        center.add(dashboardProgress);
        center.add(Box.createVerticalStrut(8));
        center.add(note);
        center.add(Box.createVerticalStrut(10));
        center.add(info);
        center.add(Box.createVerticalGlue());
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    static JButton createModuleCard(String number, String title, String subtitle, Color bg, Color accent, Runnable action) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(14, 4));
        b.setBackground(bg);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 222, 215)),
                new EmptyBorder(10, 14, 10, 14)));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel badge = new JPanel(new GridBagLayout());
        badge.setBackground(accent);
        badge.setPreferredSize(new Dimension(58, 58));
        badge.setBorder(BorderFactory.createLineBorder(accent.darker()));
        JLabel n = centerLabel(number, 21, Font.BOLD, Color.WHITE);
        badge.add(n);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, 15, Font.BOLD, TEXT));
        text.add(Box.createVerticalStrut(5));
        text.add(label(subtitle, 11, Font.PLAIN, MUTED));

        JLabel arrow = centerLabel("→", 27, Font.BOLD, accent);
        arrow.setPreferredSize(new Dimension(42, 55));
        b.add(badge, BorderLayout.WEST);
        b.add(text, BorderLayout.CENTER);
        b.add(arrow, BorderLayout.EAST);
        b.addActionListener(e -> action.run());
        return b;
    }

    static void refreshDashboard() {
        if (dashboardFrame == null || currentUser == null) return;
        dashboardIncome.setText(money(totalIncome()));
        dashboardExpense.setText(money(totalExpense()));
        dashboardBalance.setText(money(balance()));
        dashboardBudget.setText(money(currentUser.monthlyBudget));

        double spent = totalExpense();
        double budget = currentUser.monthlyBudget;
        int pct = budget <= 0 ? 0 : (int)Math.round((spent / budget) * 100.0);
        dashboardBudgetUsed.setText(money(spent) + " / " + money(budget));
        dashboardProgress.setValue(Math.min(100, Math.max(0,pct)));
        dashboardProgress.setString(pct + "% used");
        if (spent > budget) {
            dashboardBudgetStatus.setText("Budget Exceeded"); dashboardBudgetStatus.setForeground(RED); dashboardProgress.setForeground(RED);
        } else if (spent >= budget * .8) {
            dashboardBudgetStatus.setText("Warning: 80% Used"); dashboardBudgetStatus.setForeground(ORANGE); dashboardProgress.setForeground(ORANGE);
        } else {
            dashboardBudgetStatus.setText("Within Budget"); dashboardBudgetStatus.setForeground(GREEN); dashboardProgress.setForeground(GREEN);
        }
        dashboardChart.repaint();
        dashboardFrame.revalidate(); dashboardFrame.repaint();
    }

    static void logout() {
        if (dashboardFrame != null) dashboardFrame.dispose();
        currentUser = null;
        showLogin();
    }

    // ==================== MODULE 1: TRANSACTIONS ====================

    static void showTransactionWindow(JFrame parent) {
        JDialog dialog = createModuleDialog(parent,"Income & Expense Management",1000,650);
        JPanel root = moduleRoot("INCOME & EXPENSE MANAGEMENT","Record, view and delete your income and expense transactions.");

        JPanel form = whiteCard(); form.setLayout(new GridLayout(2,6,10,10));
        JComboBox<String> type = new JComboBox<>(new String[]{"Expense","Income"});
        JTextField amount = new JTextField(); JComboBox<String> category = new JComboBox<>(currentUser.categories.toArray(new String[0]));
        JTextField date = new JTextField(LocalDate.now().format(DATE_FORMAT)); JTextField desc = new JTextField();
        JButton add = new JButton("ADD TRANSACTION"); JButton clear = new JButton("CLEAR"); stylePrimary(add); styleNeutral(clear);
        form.add(formLabel("TYPE")); form.add(formLabel("AMOUNT")); form.add(formLabel("CATEGORY")); form.add(formLabel("DATE (DD-MM-YYYY)")); form.add(formLabel("DESCRIPTION")); form.add(formLabel("ACTION"));
        form.add(type); form.add(amount); form.add(category); form.add(date); form.add(desc);
        JPanel acts = new JPanel(new GridLayout(1,2,5,0)); acts.setOpaque(false); acts.add(add); acts.add(clear); form.add(acts);

        String[] cols={"Type","Amount","Category","Date","Description"};
        DefaultTableModel model = new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;} };
        JTable table = new JTable(model); table.setRowHeight(29); table.setFont(new Font("Arial",Font.PLAIN,12));
        table.getTableHeader().setFont(new Font("Arial",Font.BOLD,12)); table.getTableHeader().setBackground(NAV); table.getTableHeader().setForeground(Color.WHITE);
        refreshTransactionTable(model);

        JButton delete = new JButton("DELETE SELECTED"); styleDanger(delete);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false); bottom.add(delete);
        JPanel center = new JPanel(new BorderLayout(10,10)); center.setOpaque(false); center.add(form,BorderLayout.NORTH); center.add(new JScrollPane(table),BorderLayout.CENTER); center.add(bottom,BorderLayout.SOUTH);
        root.add(center,BorderLayout.CENTER); dialog.add(root);

        add.addActionListener(e -> {
            try {
                double value = Double.parseDouble(amount.getText().trim()); if(value<=0) throw new NumberFormatException();
                LocalDate d = LocalDate.parse(date.getText().trim(),DATE_FORMAT);
                currentUser.transactions.add(new Transaction((String)type.getSelectedItem(),value,(String)category.getSelectedItem(),d,desc.getText().trim()));
                saveData(); refreshTransactionTable(model); refreshDashboard();
                amount.setText(""); desc.setText(""); showSuccess(dialog,"Transaction added successfully.");
            } catch(NumberFormatException ex){ showWarning(dialog,"Enter a valid positive amount."); }
            catch(DateTimeParseException ex){ showWarning(dialog,"Enter date in DD-MM-YYYY format."); }
        });
        delete.addActionListener(e -> {
            int row=table.getSelectedRow(); if(row==-1){showWarning(dialog,"Select a transaction to delete.");return;}
            if(JOptionPane.showConfirmDialog(dialog,"Delete selected transaction?","Confirm Delete",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                currentUser.transactions.remove(row); saveData(); refreshTransactionTable(model); refreshDashboard();
            }
        });
        clear.addActionListener(e->{amount.setText("");desc.setText("");date.setText(LocalDate.now().format(DATE_FORMAT));});
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){keepDashboardNormal(parent);}});
        dialog.setVisible(true); keepDashboardNormal(parent);
    }

    static void refreshTransactionTable(DefaultTableModel model) {
        model.setRowCount(0);
        for(Transaction t:currentUser.transactions) model.addRow(new Object[]{t.type,money(t.amount),t.category,t.date.format(DATE_FORMAT),t.description});
    }

    // ==================== MODULE 2: CATEGORIES ====================

    static void showCategoryWindow(JFrame parent) {
        JDialog dialog=createModuleDialog(parent,"Category Management",650,540);
        JPanel root=moduleRoot("CATEGORY MANAGEMENT","Create, edit and organize your personal spending categories.");
        JPanel card=whiteCard(); card.setLayout(new BorderLayout(12,12));
        DefaultListModel<String> model=new DefaultListModel<>(); for(String c:currentUser.categories)model.addElement(c);
        JList<String> list=new JList<>(model); list.setFont(new Font("Arial",Font.PLAIN,15)); list.setFixedCellHeight(36); list.setSelectionBackground(new Color(200,230,201));
        JTextField field=new JTextField(); styleTextField(field);
        JButton add=new JButton("ADD CATEGORY"); JButton edit=new JButton("EDIT"); JButton del=new JButton("DELETE"); stylePrimary(add); styleBlue(edit); styleDanger(del);
        JPanel input=new JPanel(new BorderLayout(8,8)); input.setOpaque(false); input.add(formLabel("CATEGORY NAME"),BorderLayout.NORTH); input.add(field,BorderLayout.CENTER); input.add(add,BorderLayout.EAST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false); actions.add(edit); actions.add(del);
        card.add(input,BorderLayout.NORTH); card.add(new JScrollPane(list),BorderLayout.CENTER); card.add(actions,BorderLayout.SOUTH); root.add(card,BorderLayout.CENTER); dialog.add(root);

        add.addActionListener(e->{String c=field.getText().trim(); if(c.isEmpty()){showWarning(dialog,"Enter a category name.");return;} if(currentUser.categories.contains(c)){showWarning(dialog,"Category already exists.");return;} currentUser.categories.add(c);model.addElement(c);field.setText("");saveData();showSuccess(dialog,"Category added successfully.");});
        edit.addActionListener(e->{int i=list.getSelectedIndex(); if(i==-1){showWarning(dialog,"Select a category.");return;} String n=field.getText().trim();if(n.isEmpty()){showWarning(dialog,"Enter the new category name.");return;} if(currentUser.categories.contains(n)&&!currentUser.categories.get(i).equals(n)){showWarning(dialog,"Category already exists.");return;} currentUser.categories.set(i,n);model.set(i,n);field.setText("");saveData();showSuccess(dialog,"Category updated successfully.");});
        del.addActionListener(e->{int i=list.getSelectedIndex();if(i==-1){showWarning(dialog,"Select a category.");return;}if(currentUser.categories.size()<=1){showWarning(dialog,"At least one category must remain.");return;}if(JOptionPane.showConfirmDialog(dialog,"Delete selected category?","Confirm Delete",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){currentUser.categories.remove(i);model.remove(i);saveData();}});
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&list.getSelectedValue()!=null)field.setText(list.getSelectedValue());});
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){keepDashboardNormal(parent);}});
        dialog.setVisible(true); keepDashboardNormal(parent);
    }

    // ==================== MODULE 3: BUDGET ====================

    static void showBudgetWindow(JFrame parent) {
        JDialog dialog=createModuleDialog(parent,"Budget Management",650,500);
        JPanel root=moduleRoot("BUDGET MANAGEMENT","Set your monthly spending limit and monitor budget usage.");
        JPanel card=whiteCard(); card.setLayout(new GridBagLayout()); GridBagConstraints g=new GridBagConstraints(); g.insets=new Insets(9,12,9,12); g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1;
        JTextField budgetField=new JTextField(String.valueOf(currentUser.monthlyBudget)); styleTextField(budgetField);
        JLabel spent=centerLabel(money(totalExpense()),15,Font.BOLD,RED); JLabel remaining=centerLabel(money(currentUser.monthlyBudget-totalExpense()),15,Font.BOLD,GREEN); JLabel status=centerLabel(budgetStatus(),15,Font.BOLD,GREEN);
        addRow(card,g,0,"MONTHLY BUDGET",budgetField); addRow(card,g,1,"TOTAL SPENT",spent); addRow(card,g,2,"REMAINING",remaining); addRow(card,g,3,"STATUS",status);
        JButton save=new JButton("SAVE BUDGET"); stylePrimary(save); g.gridx=0;g.gridy=4;g.gridwidth=2;card.add(save,g); root.add(card,BorderLayout.CENTER); dialog.add(root);
        save.addActionListener(e->{try{double b=Double.parseDouble(budgetField.getText().trim());if(b<=0)throw new NumberFormatException();currentUser.monthlyBudget=b;saveData();refreshDashboard();showSuccess(dialog,"Budget updated successfully.");dialog.dispose();keepDashboardNormal(parent);}catch(NumberFormatException ex){showWarning(dialog,"Enter a valid positive budget.");}});
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){keepDashboardNormal(parent);}});
        dialog.setVisible(true); keepDashboardNormal(parent);
    }

    static void addRow(JPanel p, GridBagConstraints g, int row, String text, Component comp) {
        g.gridy=row;g.gridwidth=1;g.gridx=0;g.weightx=.35;p.add(formLabel(text),g);g.gridx=1;g.weightx=.65;p.add(comp,g);
    }

    // ==================== MODULE 4: REPORTS ====================

    static void showReportWindow(JFrame parent) {
        JDialog dialog=createModuleDialog(parent,"Reports & Analytics",1050,720);
        JPanel root=moduleRoot("REPORTS & ANALYTICS","Analyze spending with Daily, Weekly and Monthly views.");

        JPanel periodBar=new JPanel(new FlowLayout(FlowLayout.CENTER,12,10));periodBar.setBackground(Color.WHITE);
        JButton daily=new JButton("DAILY"),weekly=new JButton("WEEKLY"),monthly=new JButton("MONTHLY");styleBlue(daily);styleButton(weekly,PURPLE,Color.WHITE);stylePrimary(monthly);
        periodBar.add(daily);periodBar.add(weekly);periodBar.add(monthly);

        JPanel summary=new JPanel(new GridLayout(1,4,10,10));summary.setOpaque(false);
        JLabel ri=centerLabel("",15,Font.BOLD,GREEN), re=centerLabel("",15,Font.BOLD,RED), rb=centerLabel("",15,Font.BOLD,BLUE), rl=centerLabel("",15,Font.BOLD,ORANGE);
        summary.add(reportCard("INCOME",ri,GREEN_LIGHT));summary.add(reportCard("EXPENSE",re,RED_LIGHT));summary.add(reportCard("NET BALANCE",rb,BLUE_LIGHT));summary.add(reportCard("BUDGET LEFT",rl,ORANGE_LIGHT));

        CategoryChartPanel chart=new CategoryChartPanel(); chart.setPreferredSize(new Dimension(700,310));
        JTextArea report=new JTextArea(); report.setEditable(false); report.setFont(new Font("Monospaced",Font.PLAIN,13)); report.setBorder(new EmptyBorder(12,12,12,12));
        JScrollPane reportScroll=new JScrollPane(report); reportScroll.setPreferredSize(new Dimension(700,180));

        JPanel center=new JPanel(new BorderLayout(10,10));center.setOpaque(false);center.add(periodBar,BorderLayout.NORTH);center.add(chart,BorderLayout.CENTER);center.add(reportScroll,BorderLayout.SOUTH);
        root.add(summary,BorderLayout.NORTH);root.add(center,BorderLayout.CENTER);dialog.add(root);

        Runnable[] updater = new Runnable[1];
        updater[0] = () -> {};
        java.util.function.Consumer<String> setPeriod = period -> {
            PeriodTotals totals = calculatePeriod(period);
            ri.setText(money(totals.income)); re.setText(money(totals.expense)); rb.setText(money(totals.income-totals.expense)); rl.setText(money(currentUser.monthlyBudget-totals.expense));
            report.setText(generatePeriodReport(period)); chart.setPeriod(period); chart.repaint();
        };
        daily.addActionListener(e->setPeriod.accept("Daily")); weekly.addActionListener(e->setPeriod.accept("Weekly")); monthly.addActionListener(e->setPeriod.accept("Monthly"));
        setPeriod.accept("Monthly");
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){keepDashboardNormal(parent);}});
        dialog.setVisible(true); keepDashboardNormal(parent);
    }

    static JPanel reportCard(String title,JLabel value,Color bg){JPanel p=whiteCard();p.setBackground(bg);p.setLayout(new BorderLayout(4,4));p.add(centerLabel(title,11,Font.BOLD,TEXT),BorderLayout.NORTH);p.add(value,BorderLayout.CENTER);return p;}

    static class PeriodTotals { double income, expense; }

    static PeriodTotals calculatePeriod(String period) {
        PeriodTotals out=new PeriodTotals(); LocalDate today=LocalDate.now(); LocalDate start=periodStart(period,today);
        for(Transaction t:currentUser.transactions) if(!t.date.isBefore(start)&&!t.date.isAfter(today)){if("Income".equals(t.type))out.income+=t.amount;else out.expense+=t.amount;}
        return out;
    }

    static LocalDate periodStart(String period,LocalDate today){if("Daily".equals(period))return today;if("Weekly".equals(period))return today.minusDays(6);return today.withDayOfMonth(1);}

    static String generatePeriodReport(String period){
        LocalDate today=LocalDate.now(); LocalDate start=periodStart(period,today); PeriodTotals totals=calculatePeriod(period);
        Map<String,Double> cats=getPeriodCategoryTotals(period);
        StringBuilder sb=new StringBuilder();
        sb.append("EXPENSE TRACKER - ").append(period.toUpperCase()).append(" REPORT\n");
        sb.append("============================================================\n");
        sb.append("Period : ").append(start.format(DATE_FORMAT)).append(" to ").append(today.format(DATE_FORMAT)).append("\n");
        sb.append("Income : ").append(money(totals.income)).append("\n");
        sb.append("Expense: ").append(money(totals.expense)).append("\n");
        sb.append("Net    : ").append(money(totals.income-totals.expense)).append("\n\n");
        sb.append("CATEGORY-WISE EXPENSES\n"); sb.append("------------------------------------------------------------\n");
        for(Map.Entry<String,Double> e:cats.entrySet())sb.append(String.format(Locale.US,"%-22s %12s%n",e.getKey(),money(e.getValue())));
        return sb.toString();
    }

    static Map<String,Double> getPeriodCategoryTotals(String period){
        LocalDate today=LocalDate.now(),start=periodStart(period,today); Map<String,Double> m=new LinkedHashMap<>();
        for(String c:currentUser.categories)m.put(c,0.0);
        for(Transaction t:currentUser.transactions)if("Expense".equals(t.type)&&!t.date.isBefore(start)&&!t.date.isAfter(today))m.put(t.category,m.getOrDefault(t.category,0.0)+t.amount);
        return m;
    }

    // ==================== CHARTS ====================

    static class DashboardChartPanel extends JPanel {
        DashboardChartPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Map<String,Double> totals = new LinkedHashMap<>();
            for (String c : currentUser.categories) totals.put(c, 0.0);
            for (Transaction t : currentUser.transactions)
                if ("Expense".equals(t.type))
                    totals.put(t.category, totals.getOrDefault(t.category, 0.0) + t.amount);

            double total = 0;
            for (double v : totals.values()) total += v;
            if (total <= 0) {
                g.setColor(MUTED);
                g.setFont(new Font("Arial", Font.PLAIN, 13));
                g.drawString("No expense data yet", 18, 35);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(150, getHeight() - 25);
            int x = 18, y = (getHeight() - size) / 2;
            Color[] colors = {new Color(46,125,50), new Color(25,118,210), new Color(239,125,34), new Color(123,31,162), new Color(0,150,136)};
            int startAngle = 90, idx = 0;

            for (Map.Entry<String,Double> e : totals.entrySet()) {
                int angle = (int)Math.round(e.getValue() / total * 360);
                g2.setColor(colors[idx % colors.length]);
                g2.fillArc(x, y, size, size, startAngle, -angle);
                startAngle -= angle;
                idx++;
            }
            g2.setColor(Color.WHITE);
            int hole = (int)(size * .52);
            g2.fillOval(x + (size-hole)/2, y + (size-hole)/2, hole, hole);
            g2.setColor(TEXT);
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            String totalText = money(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalText, x + (size-fm.stringWidth(totalText))/2, y + size/2 - 2);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            String sub = "Total Spent";
            fm = g2.getFontMetrics();
            g2.setColor(MUTED);
            g2.drawString(sub, x + (size-fm.stringWidth(sub))/2, y + size/2 + 15);

            int lx = x + size + 28, ly = 18;
            idx = 0;
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            for (Map.Entry<String,Double> e : totals.entrySet()) {
                g2.setColor(colors[idx % colors.length]);
                g2.fillOval(lx, ly + 2, 10, 10);
                g2.setColor(TEXT);
                g2.drawString(e.getKey(), lx + 17, ly + 11);
                String amount = money(e.getValue());
                g2.setColor(MUTED);
                g2.drawString(amount, lx + 105, ly + 11);
                int pct = (int)Math.round(e.getValue()/total*100);
                g2.drawString(pct + "%", lx + 190, ly + 11);
                ly += 25;
                idx++;
            }
            g2.dispose();
        }
    }

    static class CategoryChartPanel extends JPanel {
        String period="Monthly";
        CategoryChartPanel(){setBackground(Color.WHITE);setBorder(BorderFactory.createTitledBorder("Category-wise Spending"));}
        void setPeriod(String p){period=p;}
        protected void paintComponent(Graphics g){super.paintComponent(g);drawBars(g,getPeriodCategoryTotals(period),"No expense data available for this period.");}
    }

    static void drawBars(Graphics g,Map<String,Double> totals,String emptyMessage){
        if(totals.isEmpty())return;
        double max=0;for(double v:totals.values())max=Math.max(max,v);
        if(max<=0){g.setColor(MUTED);g.setFont(new Font("Arial",Font.PLAIN,14));g.drawString(emptyMessage,25,40);return;}
        int n=totals.size();int left=35;int right=25;int bottom=48;int top=25;int gap=14;int available=Math.max(100,g.getClipBounds().width-left-right);int bar=Math.max(28,(available-gap*(n-1))/n);
        int base=g.getClipBounds().height-bottom;int usable=Math.max(60,g.getClipBounds().height-top-bottom);
        int i=0;
        for(Map.Entry<String,Double> e:totals.entrySet()){
            int h=(int)((e.getValue()/max)*usable);int x=left+i*(bar+gap);int y=base-h;
            g.setColor(GREEN);g.fillRoundRect(x,y,bar,h,8,8);g.setColor(TEXT);g.setFont(new Font("Arial",Font.PLAIN,11));
            String name=e.getKey();if(name.length()>11)name=name.substring(0,10)+"…";
            FontMetrics fm=g.getFontMetrics();g.drawString(name,x+Math.max(0,(bar-fm.stringWidth(name))/2),base+18);
            g.drawString(String.format(Locale.US,"₹%.0f",e.getValue()),x,Math.max(16,y-5));i++;
        }
    }
}
