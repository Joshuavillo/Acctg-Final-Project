import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
public class AccountingApp extends JFrame {
    private static final Color NAVY = new Color(31, 47, 70);
    private static final Color ACCENT = new Color(36, 116, 91);
    private static final Color BACKGROUND = new Color(246, 248, 250);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    // Report layout: label column + two amount columns (fixed-width text prints cleanly)
    private static final int LABEL_WIDTH = 36;
    private static final int AMOUNT_WIDTH = 14;

    private final Path dataDirectory = Paths.get(
            System.getProperty("user.home"), ".acctg-final-project");
    private final Path transactionsFile = dataDirectory.resolve("transactions.csv");
    private final Path accountsFile = dataDirectory.resolve("accounts.csv");
    private final List<Account> accounts = new ArrayList<Account>();
    private final List<JournalEntry> entries = new ArrayList<JournalEntry>();

    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField descriptionField = new JTextField(22);
    private final JTextField amountField = new JTextField(10);
    private final JComboBox<Account> debitAccount = new JComboBox<Account>();
    private final JComboBox<Account> creditAccount = new JComboBox<Account>();
    private final JComboBox<Account> ledgerAccount = new JComboBox<Account>();
    private final DefaultTableModel journalModel = readOnlyModel(
            "Date", "Description", "Debit account", "Debit", "Credit account", "Credit");
    private final DefaultTableModel ledgerModel = readOnlyModel(
            "Date", "Description", "Debit", "Credit", "Balance");
    private final DefaultTableModel accountsModel = readOnlyModel(
            "Account", "Type", "Total debits", "Total credits", "Balance");
    private final JLabel totalsLabel = new JLabel();
    private JTable journalTable;
    private JTable ledgerTable;
    private JTable accountsTable;

    // Date filter (null = no limit) and report display areas
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private LocalDate filterFrom;
    private LocalDate filterTo;
    private final JTextArea trialBalanceArea = reportArea();
    private final JTextArea incomeStatementArea = reportArea();
    private final JTextArea balanceSheetArea = reportArea();

    private AccountingApp() throws IOException {
        super("Ledger & Journal | Accounting");
        Files.createDirectories(dataDirectory);
        loadAccounts();
        loadEntries();
        refreshAccountSelectors();
        buildWindow();
        refreshTables();
    }

    private void buildWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(980, 650));
        setSize(1120, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout(0, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel title = new JLabel("Accounting");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitle = new JLabel("Double-entry accounting made clear");
        subtitle.setForeground(new Color(211, 222, 234));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel heading = new JPanel(new GridBagLayout());
        heading.setOpaque(false);
        GridBagConstraints headingConstraints = new GridBagConstraints();
        headingConstraints.gridx = 0;
        headingConstraints.gridy = 0;
        headingConstraints.anchor = GridBagConstraints.WEST;
        heading.add(title, headingConstraints);
        headingConstraints.gridy = 1;
        headingConstraints.insets = new Insets(4, 0, 0, 0);
        heading.add(subtitle, headingConstraints);
        header.add(heading, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        content.add(buildEntryPanel(), BorderLayout.NORTH);

        JPanel reports = new JPanel(new BorderLayout(0, 6));
        reports.setOpaque(false);
        reports.add(buildFilterBar(), BorderLayout.NORTH);
        reports.add(buildTabs(), BorderLayout.CENTER);
        content.add(reports, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildEntryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 230, 236)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        addField(panel, "Date (YYYY-MM-DD)", dateField, 0, 0);
        addField(panel, "Particulars", descriptionField, 1, 0);
        addField(panel, "Amount", amountField, 2, 0);
        addField(panel, "Debit account", debitAccount, 0, 1);
        addField(panel, "Credit account", creditAccount, 1, 1);

        JButton addButton = new JButton("Post transaction");
        addButton.setBackground(ACCENT);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.addActionListener(event -> postTransaction());
        JButton addAccountButton = new JButton("Add account");
        addAccountButton.addActionListener(event -> addAccount());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(addAccountButton);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(16, 8, 2, 0);
        panel.add(actions, constraints);

        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 0;
        hintConstraints.gridy = 4;
        hintConstraints.gridwidth = 4;
        hintConstraints.anchor = GridBagConstraints.WEST;
        hintConstraints.insets = new Insets(10, 6, 0, 0);
        JLabel hint = new JLabel(
                "Each entry posts one debit and one credit for the same amount.");
        hint.setForeground(new Color(92, 105, 118));
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(hint, hintConstraints);
        return panel;
    }

    private void addField(JPanel panel, String label, java.awt.Component field,
                          int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row * 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 6, 4, 8);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(fieldLabel, constraints);

        constraints.gridy = row * 2 + 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = column == 1 ? 1.0 : 0.0;
        constraints.insets = new Insets(0, 6, 2, 8);
        panel.add(field, constraints);
    }

    // ---------- Date filter ----------

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createLineBorder(new Color(224, 230, 236)));
        JLabel label = new JLabel("Report date range (YYYY-MM-DD)   From:");
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        bar.add(label);
        bar.add(fromField);
        bar.add(new JLabel("To:"));
        bar.add(toField);

        JButton apply = new JButton("Apply filter");
        apply.addActionListener(event -> applyFilter());
        fromField.addActionListener(event -> applyFilter());
        toField.addActionListener(event -> applyFilter());
        JButton clear = new JButton("Clear");
        clear.addActionListener(event -> {
            fromField.setText("");
            toField.setText("");
            applyFilter();
        });
        bar.add(apply);
        bar.add(clear);

        JLabel note = new JLabel("Blank = no limit. Applies to Trial Balance, Income Statement and Balance Sheet.");
        note.setForeground(new Color(92, 105, 118));
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bar.add(note);
        return bar;
    }

    private void applyFilter() {
        try {
            LocalDate from = parseOptionalDate(fromField.getText());
            LocalDate to = parseOptionalDate(toField.getText());
            if (from != null && to != null && from.isAfter(to)) {
                showError("The From date must not be after the To date.");
                return;
            }
            filterFrom = from;
            filterTo = to;
            refreshReports();
        } catch (DateTimeParseException exception) {
            showError("Enter valid dates in YYYY-MM-DD format, or leave them blank.");
        }
    }

    private static LocalDate parseOptionalDate(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : LocalDate.parse(trimmed);
    }

    // ---------- Tabs ----------

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        journalTable = createTable(journalModel);
        ledgerTable = createTable(ledgerModel);
        accountsTable = createTable(accountsModel);

        JPanel journalPanel = new JPanel(new BorderLayout(0, 8));
        journalPanel.setBackground(Color.WHITE);
        totalsLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        totalsLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        journalPanel.add(totalsLabel, BorderLayout.NORTH);
        journalPanel.add(new JScrollPane(journalTable), BorderLayout.CENTER);
        tabs.addTab("Columnar Journal", journalPanel);

        JPanel ledgerPanel = new JPanel(new BorderLayout(0, 8));
        ledgerPanel.setBackground(Color.WHITE);
        JPanel ledgerHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        ledgerHeader.setBackground(Color.WHITE);
        ledgerHeader.add(new JLabel("Account:"));
        ledgerAccount.addActionListener(event -> refreshLedger());
        ledgerHeader.add(ledgerAccount);
        ledgerPanel.add(ledgerHeader, BorderLayout.NORTH);
        ledgerPanel.add(new JScrollPane(ledgerTable), BorderLayout.CENTER);
        tabs.addTab("General Ledger", ledgerPanel);
        tabs.addTab("Account Balances", new JScrollPane(accountsTable));

        tabs.addTab("Trial Balance", buildReportTab(trialBalanceArea, "Trial Balance"));
        tabs.addTab("Income Statement", buildReportTab(incomeStatementArea, "Income Statement"));
        tabs.addTab("Balance Sheet", buildReportTab(balanceSheetArea, "Balance Sheet"));
        return tabs;
    }

    private JPanel buildReportTab(JTextArea area, String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(Color.WHITE);
        JButton printButton = new JButton("Print / Save as PDF");
        printButton.addActionListener(event -> printReport(area, title));
        top.add(printButton);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        return table;
    }

    // ---------- Print-friendly view ----------

    private static JTextArea reportArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(Color.WHITE);
        area.setForeground(Color.BLACK);
        area.setMargin(new Insets(14, 18, 14, 18));
        return area;
    }

    // Plain black-on-white text, no buttons or colors. The standard print dialog
    // also lets the user choose "Save as PDF" as the printer.
    private void printReport(JTextArea area, String title) {
        try {
            area.print(new MessageFormat(title), new MessageFormat("Page {0}"));
        } catch (PrinterException exception) {
            showError("The report could not be printed: " + exception.getMessage());
        }
    }

    // ---------- Balance calculations (shared by all reports) ----------

    // Debits minus credits for an account within the date range (null = unbounded).
    private BigDecimal net(Account account, LocalDate from, LocalDate to) {
        BigDecimal total = ZERO;
        for (JournalEntry entry : entries) {
            if ((from != null && entry.date.isBefore(from))
                    || (to != null && entry.date.isAfter(to))) {
                continue;
            }
            if (entry.debitAccount.equals(account.name)) {
                total = total.add(entry.amount);
            }
            if (entry.creditAccount.equals(account.name)) {
                total = total.subtract(entry.amount);
            }
        }
        return total;
    }

    // Balance on the account's normal side (positive = normal balance).
    private BigDecimal balanceOf(Account account, LocalDate from, LocalDate to) {
        BigDecimal net = net(account, from, to);
        return account.type.isDebitNormal() ? net : net.negate();
    }

    private BigDecimal totalOfType(AccountType type, LocalDate from, LocalDate to) {
        BigDecimal total = ZERO;
        for (Account account : accounts) {
            if (account.type == type) {
                total = total.add(balanceOf(account, from, to));
            }
        }
        return total;
    }

    // ---------- Report builders ----------

    private void refreshReports() {
        refreshTrialBalance();
        refreshIncomeStatement();
        refreshBalanceSheet();
    }

    private void refreshTrialBalance() {
        StringBuilder text = new StringBuilder();
        appendHeading(text, "TRIAL BALANCE", periodText(filterFrom, filterTo));
        text.append(row("Account", "Debit", "Credit"));
        text.append(rule());
        BigDecimal totalDebits = ZERO;
        BigDecimal totalCredits = ZERO;
        for (Account account : accounts) {
            BigDecimal net = net(account, filterFrom, filterTo);
            BigDecimal debit = net.signum() > 0 ? net : ZERO;
            BigDecimal credit = net.signum() < 0 ? net.negate() : ZERO;
            text.append(row(account.name,
                    net.signum() > 0 ? format(debit) : "",
                    net.signum() < 0 ? format(credit) : ""));
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
        }
        text.append(rule());
        text.append(row("Total", format(totalDebits), format(totalCredits)));
        text.append("\n");
        if (totalDebits.compareTo(totalCredits) == 0) {
            text.append("Total debits equal total credits: BALANCED\n");
        } else {
            text.append("OUT OF BALANCE by " + format(totalDebits.subtract(totalCredits)) + "\n");
        }
        setReportText(trialBalanceArea, text);
    }

    private void refreshIncomeStatement() {
        StringBuilder text = new StringBuilder();
        appendHeading(text, "INCOME STATEMENT", periodText(filterFrom, filterTo));

        text.append("Revenues\n");
        for (Account account : accounts) {
            if (account.type == AccountType.REVENUE) {
                text.append(row("  " + account.name,
                        format(balanceOf(account, filterFrom, filterTo)), ""));
            }
        }
        BigDecimal revenues = totalOfType(AccountType.REVENUE, filterFrom, filterTo);
        text.append(row("Total revenues", "", format(revenues)));
        text.append("\n");

        text.append("Expenses\n");
        for (Account account : accounts) {
            if (account.type == AccountType.EXPENSE) {
                text.append(row("  " + account.name,
                        format(balanceOf(account, filterFrom, filterTo)), ""));
            }
        }
        BigDecimal expenses = totalOfType(AccountType.EXPENSE, filterFrom, filterTo);
        text.append(row("Total expenses", "", format(expenses)));
        text.append(rule());
        text.append(row("Net income (loss)", "", format(revenues.subtract(expenses))));
        setReportText(incomeStatementArea, text);
    }

    private void refreshBalanceSheet() {
        // A balance sheet is a point in time: everything up to the "To" date.
        LocalDate asOf = filterTo;
        StringBuilder text = new StringBuilder();
        appendHeading(text, "BALANCE SHEET",
                asOf == null ? "As of all dates" : "As of " + asOf);

        text.append("Assets\n");
        for (Account account : accounts) {
            if (account.type == AccountType.ASSET) {
                text.append(row("  " + account.name, format(balanceOf(account, null, asOf)), ""));
            }
        }
        BigDecimal assets = totalOfType(AccountType.ASSET, null, asOf);
        text.append(row("Total assets", "", format(assets)));
        text.append("\n");

        text.append("Liabilities\n");
        for (Account account : accounts) {
            if (account.type == AccountType.LIABILITY) {
                text.append(row("  " + account.name, format(balanceOf(account, null, asOf)), ""));
            }
        }
        BigDecimal liabilities = totalOfType(AccountType.LIABILITY, null, asOf);
        text.append(row("Total liabilities", "", format(liabilities)));
        text.append("\n");

        text.append("Equity\n");
        for (Account account : accounts) {
            if (account.type == AccountType.EQUITY) {
                text.append(row("  " + account.name, format(balanceOf(account, null, asOf)), ""));
            }
        }
        for (Account account : accounts) {
            if (account.type == AccountType.CONTRA_EQUITY) {
                text.append(row("  Less: " + account.name,
                        format(balanceOf(account, null, asOf).negate()), ""));
            }
        }
        // Books are not closed, so net income to date is added to equity.
        BigDecimal netIncome = totalOfType(AccountType.REVENUE, null, asOf)
                .subtract(totalOfType(AccountType.EXPENSE, null, asOf));
        text.append(row("  Net income to date", format(netIncome), ""));
        BigDecimal equity = totalOfType(AccountType.EQUITY, null, asOf)
                .subtract(totalOfType(AccountType.CONTRA_EQUITY, null, asOf))
                .add(netIncome);
        text.append(row("Total equity", "", format(equity)));
        text.append(rule());
        BigDecimal liabilitiesAndEquity = liabilities.add(equity);
        text.append(row("Total liabilities and equity", "", format(liabilitiesAndEquity)));
        text.append("\n");
        if (assets.compareTo(liabilitiesAndEquity) == 0) {
            text.append("Assets equal liabilities plus equity: BALANCED\n");
        } else {
            text.append("OUT OF BALANCE by " + format(assets.subtract(liabilitiesAndEquity)) + "\n");
        }
        setReportText(balanceSheetArea, text);
    }

    private static void setReportText(JTextArea area, StringBuilder text) {
        area.setText(text.toString());
        area.setCaretPosition(0);
    }

    private static void appendHeading(StringBuilder text, String title, String period) {
        text.append(title).append("\n");
        text.append(period).append("\n");
        text.append(rule()).append("\n");
    }

    private static String periodText(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "For all dates";
        }
        if (from == null) {
            return "Through " + to;
        }
        if (to == null) {
            return "From " + from;
        }
        return "For the period " + from + " to " + to;
    }

    private static String row(String label, String first, String second) {
        String shortLabel = label.length() > LABEL_WIDTH - 1
                ? label.substring(0, LABEL_WIDTH - 1) : label;
        return String.format("%-" + LABEL_WIDTH + "s%" + AMOUNT_WIDTH + "s%" + AMOUNT_WIDTH + "s%n",
                shortLabel, first, second);
    }

    private static String rule() {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < LABEL_WIDTH + 2 * AMOUNT_WIDTH; i++) {
            line.append('-');
        }
        return line.append("\n").toString();
    }

    // ---------- Posting and accounts ----------

    private void postTransaction() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                throw new IllegalArgumentException("Enter a description for the transaction.");
            }
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("The amount must be greater than zero.");
            }
            if (amount.scale() > 2) {
                throw new IllegalArgumentException("Enter an amount with no more than two decimal places.");
            }
            Account debit = (Account) debitAccount.getSelectedItem();
            Account credit = (Account) creditAccount.getSelectedItem();
            if (debit == null || credit == null) {
                throw new IllegalArgumentException("Add at least two accounts before posting a transaction.");
            }
            if (debit.equals(credit)) {
                throw new IllegalArgumentException("Choose different debit and credit accounts.");
            }

            JournalEntry entry = new JournalEntry(
                    date, description, debit.name, credit.name,
                    amount.setScale(2, RoundingMode.UNNECESSARY));
            List<JournalEntry> updatedEntries = new ArrayList<JournalEntry>(entries);
            updatedEntries.add(entry);
            saveEntries(updatedEntries);
            entries.add(entry);
            refreshTables();
            descriptionField.setText("");
            amountField.setText("");
        } catch (DateTimeParseException exception) {
            showError("Enter a valid date in YYYY-MM-DD format.");
        } catch (NumberFormatException exception) {
            showError("Enter a valid amount, such as 125.00.");
        } catch (IllegalArgumentException | ArithmeticException exception) {
            showError(exception.getMessage());
        } catch (IOException exception) {
            showError("The transaction could not be saved: " + exception.getMessage());
        }
    }

    private void addAccount() {
        JTextField nameField = new JTextField(18);
        JComboBox<AccountType> typeField = new JComboBox<AccountType>(AccountType.values());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Account name:"), constraints);
        constraints.gridx = 1;
        form.add(nameField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        form.add(new JLabel("Account type:"), constraints);
        constraints.gridx = 1;
        form.add(typeField, constraints);

        int choice = JOptionPane.showConfirmDialog(this, form, "Add account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Enter an account name.");
            return;
        }
        for (Account account : accounts) {
            if (account.name.equalsIgnoreCase(name)) {
                showError("An account with that name already exists.");
                return;
            }
        }

        Account account = new Account(name, (AccountType) typeField.getSelectedItem());
        List<Account> updatedAccounts = new ArrayList<Account>(accounts);
        updatedAccounts.add(account);
        try {
            saveAccounts(updatedAccounts);
            accounts.add(account);
            refreshAccountSelectors();
            refreshTables();
        } catch (IOException exception) {
            showError("The account could not be saved: " + exception.getMessage());
        }
    }

    private void refreshTables() {
        journalModel.setRowCount(0);
        BigDecimal totalDebits = ZERO;
        BigDecimal totalCredits = ZERO;
        for (JournalEntry entry : entries) {
            journalModel.addRow(new Object[]{
                    entry.date.toString(), entry.description, entry.debitAccount,
                    format(entry.amount), entry.creditAccount, format(entry.amount)});
            totalDebits = totalDebits.add(entry.amount);
            totalCredits = totalCredits.add(entry.amount);
        }
        totalsLabel.setText("Journal totals     Debits: " + format(totalDebits)
                + "     Credits: " + format(totalCredits));

        accountsModel.setRowCount(0);
        for (Account account : accounts) {
            BigDecimal debits = ZERO;
            BigDecimal credits = ZERO;
            for (JournalEntry entry : entries) {
                if (entry.debitAccount.equals(account.name)) {
                    debits = debits.add(entry.amount);
                }
                if (entry.creditAccount.equals(account.name)) {
                    credits = credits.add(entry.amount);
                }
            }
            BigDecimal balance = account.type.isDebitNormal()
                    ? debits.subtract(credits) : credits.subtract(debits);
            accountsModel.addRow(new Object[]{
                    account.name, account.type.label, format(debits),
                    format(credits), format(balance)});
        }
        refreshLedger();
        refreshReports();
    }

    private void refreshLedger() {
        ledgerModel.setRowCount(0);
        Account selected = (Account) ledgerAccount.getSelectedItem();
        if (selected == null) {
            return;
        }
        List<JournalEntry> sortedEntries = new ArrayList<JournalEntry>(entries);
        Collections.sort(sortedEntries, Comparator.comparing(entry -> entry.date));
        BigDecimal balance = ZERO;
        for (JournalEntry entry : sortedEntries) {
            boolean isDebit = entry.debitAccount.equals(selected.name);
            boolean isCredit = entry.creditAccount.equals(selected.name);
            if (!isDebit && !isCredit) {
                continue;
            }
            BigDecimal debit = isDebit ? entry.amount : ZERO;
            BigDecimal credit = isCredit ? entry.amount : ZERO;
            balance = selected.type.isDebitNormal()
                    ? balance.add(debit).subtract(credit)
                    : balance.add(credit).subtract(debit);
            ledgerModel.addRow(new Object[]{
                    entry.date.toString(), entry.description, format(debit),
                    format(credit), format(balance)});
        }
    }

    private void refreshAccountSelectors() {
        Account previousDebit = (Account) debitAccount.getSelectedItem();
        Account previousCredit = (Account) creditAccount.getSelectedItem();
        Account previousLedger = (Account) ledgerAccount.getSelectedItem();
        Account[] choices = accounts.toArray(new Account[0]);
        debitAccount.setModel(new DefaultComboBoxModel<Account>(choices));
        creditAccount.setModel(new DefaultComboBoxModel<Account>(choices));
        ledgerAccount.setModel(new DefaultComboBoxModel<Account>(choices));
        if (previousDebit != null) {
            selectAccount(debitAccount, previousDebit.name);
        }
        if (previousCredit != null) {
            selectAccount(creditAccount, previousCredit.name);
        }
        if (previousLedger != null) {
            selectAccount(ledgerAccount, previousLedger.name);
        }
    }

    private void selectAccount(JComboBox<Account> selector, String name) {
        for (int i = 0; i < selector.getItemCount(); i++) {
            if (selector.getItemAt(i).name.equals(name)) {
                selector.setSelectedIndex(i);
                return;
            }
        }
    }

    // ---------- File storage ----------

    private void loadAccounts() throws IOException {
        if (!Files.exists(accountsFile)) {
            accounts.addAll(defaultAccounts());
            saveAccounts(accounts);
            return;
        }
        List<String> lines = Files.readAllLines(accountsFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            List<String> fields = parseCsvLine(lines.get(i));
            if (fields.size() != 2) {
                throw new IOException("Invalid account data on line " + (i + 1) + ".");
            }
            try {
                accounts.add(new Account(fields.get(0), AccountType.valueOf(fields.get(1))));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid account data on line " + (i + 1) + ".", exception);
            }
        }
    }

    private void loadEntries() throws IOException {
        if (!Files.exists(transactionsFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(transactionsFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            List<String> fields = parseCsvLine(lines.get(i));
            if (fields.size() != 5) {
                throw new IOException("Invalid transaction data on line " + (i + 1) + ".");
            }
            try {
                BigDecimal amount = new BigDecimal(fields.get(4));
                if (amount.signum() <= 0 || amount.scale() > 2) {
                    throw new IllegalArgumentException("Invalid transaction amount.");
                }
                entries.add(new JournalEntry(LocalDate.parse(fields.get(0)), fields.get(1),
                        fields.get(2), fields.get(3), amount));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid transaction data on line " + (i + 1) + ".", exception);
            }
        }
    }

    private void saveAccounts(List<Account> accountsToSave) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("name,type");
        for (Account account : accountsToSave) {
            lines.add(csv(account.name) + "," + csv(account.type.name()));
        }
        writeAtomically(accountsFile, lines);
    }

    private void saveEntries(List<JournalEntry> entriesToSave) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("date,description,debit_account,credit_account,amount");
        for (JournalEntry entry : entriesToSave) {
            lines.add(csv(entry.date.toString()) + "," + csv(entry.description) + ","
                    + csv(entry.debitAccount) + "," + csv(entry.creditAccount) + ","
                    + entry.amount.toPlainString());
        }
        writeAtomically(transactionsFile, lines);
    }

    private void writeAtomically(Path destination, List<String> lines) throws IOException {
        Path temporaryFile = Files.createTempFile(dataDirectory, "accounting-", ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (quoted) {
                if (character == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (character == '"') {
                    quoted = false;
                } else {
                    field.append(character);
                }
            } else if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (character == '"' && field.length() == 0) {
                quoted = true;
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            throw new IOException("Invalid CSV data: unclosed quoted field.");
        }
        fields.add(field.toString());
        return fields;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static List<Account> defaultAccounts() {
        List<Account> defaults = new ArrayList<Account>();
        defaults.add(new Account("Cash", AccountType.ASSET));
        defaults.add(new Account("Accounts Receivable", AccountType.ASSET));
        defaults.add(new Account("Supplies", AccountType.ASSET));
        defaults.add(new Account("Equipment", AccountType.ASSET));
        defaults.add(new Account("Accounts Payable", AccountType.LIABILITY));
        defaults.add(new Account("Owner's Capital", AccountType.EQUITY));
        defaults.add(new Account("Owner's Drawings", AccountType.CONTRA_EQUITY));
        defaults.add(new Account("Service Revenue", AccountType.REVENUE));
        defaults.add(new Account("Rent Expense", AccountType.EXPENSE));
        defaults.add(new Account("Utilities Expense", AccountType.EXPENSE));
        return defaults;
    }

    private static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Unable to continue",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                AccountingApp app = new AccountingApp();
                app.setVisible(true);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null,
                        "The accounting data could not be loaded: " + exception.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            } catch (ReflectiveOperationException exception) {
                JOptionPane.showMessageDialog(null,
                        "The application appearance could not be initialized: " + exception.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            } catch (UnsupportedLookAndFeelException exception) {
                JOptionPane.showMessageDialog(null,
                        "The system appearance is unavailable: " + exception.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static final class Account {
        private final String name;
        private final AccountType type;

        private Account(String name, AccountType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum AccountType {
        ASSET("Asset", true),
        LIABILITY("Liability", false),
        EQUITY("Equity", false),
        CONTRA_EQUITY("Contra Equity", true),
        REVENUE("Revenue", false),
        EXPENSE("Expense", true);

        private final String label;
        private final boolean debitNormal;

        AccountType(String label, boolean debitNormal) {
            this.label = label;
            this.debitNormal = debitNormal;
        }

        @Override
        public String toString() {
            return label;
        }

        private boolean isDebitNormal() {
            return debitNormal;
        }
    }

    private static final class JournalEntry {
        private final LocalDate date;
        private final String description;
        private final String debitAccount;
        private final String creditAccount;
        private final BigDecimal amount;

        private JournalEntry(LocalDate date, String description, String debitAccount,
                             String creditAccount, BigDecimal amount) {
            this.date = date;
            this.description = description;
            this.debitAccount = debitAccount;
            this.creditAccount = creditAccount;
            this.amount = amount;
        }
    }
}
