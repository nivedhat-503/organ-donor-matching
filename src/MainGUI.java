import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// Graphical front end. It uses exactly the same MatchService as the console menu (Main),
// so all rules, validation and database code are shared.
public class MainGUI extends JFrame {

    // ---------- look and feel ----------
    private static final Color PRIMARY = new Color(0, 105, 120);
    private static final Color HEADER_SUBTEXT = new Color(210, 235, 240);
    private static final Color URGENT_5 = new Color(255, 200, 200);   // critical
    private static final Color URGENT_4 = new Color(255, 225, 190);
    private static final Color URGENT_3 = new Color(255, 247, 190);

    // ---------- tab positions ----------
    private static final int TAB_HOME = 0;
    private static final int TAB_DONOR = 1;
    private static final int TAB_RECIPIENT = 2;
    private static final int TAB_WAITING = 3;
    private static final int TAB_MATCH = 4;
    private static final int TAB_HISTORY = 5;
    private static final int TAB_REPORTS = 6;

    private static final String ALL_ORGANS = "All organs";
    private static final String[] RECIPIENT_COLUMNS =
            {"ID", "Name", "Age", "Blood", "Organ", "City", "Urgency", "Days waiting"};
    private static final String[] MATCH_COLUMNS =
            {"Rank", "ID", "Name", "Age", "Blood", "Organ", "City", "Urgency", "Days waiting"};

    private final MatchService service = new MatchService();

    private JTabbedPane tabs;
    private JLabel statusBar;

    // Home tab
    private JLabel statDonors;
    private JLabel statWaiting;
    private JLabel statMatches;
    private JLabel statAvgWait;

    // Register donor tab
    private JTextField donorName;
    private JSpinner donorAge;
    private JComboBox<String> donorBlood;
    private JComboBox<String> donorOrgan;
    private JTextField donorCity;
    private JButton donorSubmit;

    // Register recipient tab
    private JTextField recName;
    private JSpinner recAge;
    private JComboBox<String> recBlood;
    private JComboBox<String> recOrgan;
    private JTextField recCity;
    private JSpinner recUrgency;
    private JButton recSubmit;

    // Waiting list tab
    private JComboBox<String> waitingOrganFilter;
    private JTextField waitingSearch;
    private JLabel waitingCount;
    private DefaultTableModel waitingModel;
    private JTable waitingTable;
    private TableRowSorter<DefaultTableModel> waitingSorter;

    // Find match tab
    private JComboBox<Donor> donorPicker;
    private JButton findButton;
    private DefaultTableModel matchModel;
    private JTable matchTable;
    private JLabel bestMatchLabel;
    private JButton confirmButton;
    private Donor matchesForDonor;   // the donor the table on screen was ranked for

    // History and reports tabs
    private JTextArea historyArea;
    private JTextArea reportsArea;

    public MainGUI() {
        super("Organ Donor Matching System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        tabs = new JTabbedPane();
        tabs.addTab("Home", buildHomeTab());
        tabs.addTab("Register Donor", buildDonorTab());
        tabs.addTab("Register Recipient", buildRecipientTab());
        tabs.addTab("Waiting List", buildWaitingTab());
        tabs.addTab("Find Match", buildMatchTab());
        tabs.addTab("History", buildHistoryTab());
        tabs.addTab("Reports", buildReportsTab());
        tabs.addChangeListener(e -> {
            refreshAll();             // reload data whenever a tab is opened
            updateDefaultButton();    // so Enter submits the form on screen
        });

        add(buildBanner(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
        refreshAll();
    }

    // =====================================================================
    // Banner and Home tab
    // =====================================================================

    private JPanel buildBanner() {
        JLabel title = new JLabel("Organ Donor Matching System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Coordinator console: register donors and recipients, find and confirm matches");
        subtitle.setForeground(HEADER_SUBTEXT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(PRIMARY);
        banner.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        banner.add(text, BorderLayout.WEST);
        return banner;
    }

    private JPanel buildHomeTab() {
        statDonors = statValueLabel();
        statWaiting = statValueLabel();
        statMatches = statValueLabel();
        statAvgWait = statValueLabel();

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.add(statCard("Available donors", statDonors));
        cards.add(statCard("Recipients waiting", statWaiting));
        cards.add(statCard("Matches made", statMatches));
        cards.add(statCard("Average wait (days)", statAvgWait));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(quickButton("Register a donor", TAB_DONOR));
        actions.add(quickButton("Register a recipient", TAB_RECIPIENT));
        actions.add(quickButton("Find a match", TAB_MATCH));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(cards);
        top.add(Box.createVerticalStrut(16));
        top.add(actions);

        JTextArea how = new JTextArea(
                "For a chosen donor, the system:\n"
              + "  1. keeps recipients who are still waiting and need the same organ,\n"
              + "  2. keeps only those with a compatible blood group,\n"
              + "  3. ranks them by urgency (5 = critical), then by how long they have waited, then by same city.\n\n"
              + "Confirming a match marks both people as matched and records it in the history.\n\n"
              + "This is an educational project with fictional data and simplified rules. "
              + "Real organ allocation also uses tissue typing, organ size, distance and clinical review.");
        how.setEditable(false);
        how.setLineWrap(true);
        how.setWrapStyleWord(true);
        how.setOpaque(false);

        JPanel howPanel = new JPanel(new BorderLayout());
        howPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("How matching works"),
                BorderFactory.createEmptyBorder(6, 10, 10, 10)));
        howPanel.add(how, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(top, BorderLayout.NORTH);
        panel.add(howPanel, BorderLayout.CENTER);
        return panel;
    }

    private JLabel statValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 32f));
        label.setForeground(PRIMARY);
        return label;
    }

    private JPanel statCard(String caption, JLabel value) {
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(Color.GRAY);

        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.add(value, BorderLayout.CENTER);
        card.add(captionLabel, BorderLayout.SOUTH);
        return card;
    }

    private JButton quickButton(String text, int tabIndex) {
        JButton button = new JButton(text);
        button.addActionListener(e -> tabs.setSelectedIndex(tabIndex));
        return button;
    }

    // =====================================================================
    // Register tabs
    // =====================================================================

    private JPanel buildDonorTab() {
        donorName = new JTextField(22);
        donorName.setToolTipText("Full name of the donor");
        donorAge = new JSpinner(new SpinnerNumberModel(30, 1, 120, 1));
        donorAge.setToolTipText("Donors must be between 18 and 65");
        donorBlood = new JComboBox<>(MatchService.BLOOD_GROUPS.toArray(new String[0]));
        donorOrgan = new JComboBox<>(MatchService.ORGANS.toArray(new String[0]));
        donorCity = new JTextField(22);
        donorCity.setToolTipText("City where the donor is located");

        donorSubmit = primaryButton("Register donor");
        donorSubmit.addActionListener(e -> onRegisterDonor());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clearDonorForm());

        return formPanel("Register a donor",
                "Fields marked * are required. Donors must be between 18 and 65 years old.",
                new String[]{"Name *", "Age *", "Blood group *", "Organ donated *", "City *"},
                new JComponent[]{donorName, donorAge, donorBlood, donorOrgan, donorCity},
                donorSubmit, clear);
    }

    private JPanel buildRecipientTab() {
        recName = new JTextField(22);
        recName.setToolTipText("Full name of the recipient");
        recAge = new JSpinner(new SpinnerNumberModel(40, 1, 120, 1));
        recBlood = new JComboBox<>(MatchService.BLOOD_GROUPS.toArray(new String[0]));
        recOrgan = new JComboBox<>(MatchService.ORGANS.toArray(new String[0]));
        recCity = new JTextField(22);
        recCity.setToolTipText("City where the recipient is located");
        recUrgency = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        recUrgency.setToolTipText("1 = low priority, 5 = critical");

        recSubmit = primaryButton("Register recipient");
        recSubmit.addActionListener(e -> onRegisterRecipient());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clearRecipientForm());

        return formPanel("Register a recipient",
                "Fields marked * are required. Urgency runs from 1 (low) to 5 (critical).",
                new String[]{"Name *", "Age *", "Blood group *", "Organ needed *", "City *", "Urgency (1-5) *"},
                new JComponent[]{recName, recAge, recBlood, recOrgan, recCity, recUrgency},
                recSubmit, clear);
    }

    // Lays out "label: field" rows with a submit and a clear button underneath
    private JPanel formPanel(String title, String hint, String[] labels, JComponent[] fields,
                             JButton submit, JButton clear) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 6, 7, 6);
        gc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < labels.length; i++) {
            fields[i].setPreferredSize(new Dimension(280, 30));

            gc.gridx = 0;
            gc.gridy = i;
            form.add(new JLabel(labels[i]), gc);

            gc.gridx = 1;
            form.add(fields[i], gc);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(submit);
        buttons.add(clear);
        gc.gridx = 1;
        gc.gridy = labels.length;
        gc.insets = new Insets(16, 6, 6, 6);
        form.add(buttons, gc);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.add(form);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(headerBlock(title, hint), BorderLayout.NORTH);
        panel.add(left, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    // Waiting list tab
    // =====================================================================

    private JPanel buildWaitingTab() {
        waitingModel = newModel(RECIPIENT_COLUMNS);
        waitingTable = new JTable(waitingModel);
        styleTable(waitingTable, 6);
        waitingSorter = new TableRowSorter<>(waitingModel);   // click a column header to sort
        waitingTable.setRowSorter(waitingSorter);

        waitingCount = new JLabel(" ");
        waitingCount.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        waitingOrganFilter = new JComboBox<>();
        waitingOrganFilter.addItem(ALL_ORGANS);
        for (String organ : MatchService.ORGANS) {
            waitingOrganFilter.addItem(organ);
        }
        waitingOrganFilter.addActionListener(e -> refreshWaitingListSafely());

        waitingSearch = new JTextField(16);
        waitingSearch.setToolTipText("Type part of a name or city to filter the list");
        waitingSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyWaitingFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyWaitingFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyWaitingFilter(); }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        controls.add(new JLabel("Organ:"));
        controls.add(waitingOrganFilter);
        controls.add(new JLabel("Search name or city:"));
        controls.add(waitingSearch);
        controls.add(Box.createHorizontalStrut(12));
        controls.add(legendLabel("Urgency 5", URGENT_5));
        controls.add(legendLabel("Urgency 4", URGENT_4));
        controls.add(legendLabel("Urgency 3", URGENT_3));

        JPanel north = new JPanel(new BorderLayout());
        north.add(headerBlock("Waiting list",
                "Everyone still waiting for an organ. Click a column title to sort."), BorderLayout.NORTH);
        north.add(controls, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(waitingTable), BorderLayout.CENTER);
        panel.add(waitingCount, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel legendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(color);
        label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return label;
    }

    private void applyWaitingFilter() {
        String text = waitingSearch.getText().trim();
        if (text.isEmpty()) {
            waitingSorter.setRowFilter(null);
        } else {
            // columns 1 (Name) and 5 (City), case-insensitive, text matched literally
            waitingSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 5));
        }
        waitingCount.setText(waitingTable.getRowCount() + " recipient(s) shown");
    }

    // =====================================================================
    // Find match tab
    // =====================================================================

    private JPanel buildMatchTab() {
        donorPicker = new JComboBox<>();
        donorPicker.setPreferredSize(new Dimension(520, 30));
        donorPicker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Donor) {
                    Donor d = (Donor) value;
                    setText("#" + d.getDonorId() + "   " + d.getName() + "   |   " + d.getBloodGroup()
                            + "  " + d.getOrganType() + "   |   " + d.getCity());
                }
                return this;
            }
        });

        findButton = primaryButton("Find matches");
        findButton.addActionListener(e -> onFindMatches());
        JButton refreshButton = new JButton("Refresh donors");
        refreshButton.addActionListener(e -> refreshAll());

        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        picker.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        picker.add(new JLabel("Available donor:"));
        picker.add(donorPicker);
        picker.add(findButton);
        picker.add(refreshButton);

        JPanel north = new JPanel(new BorderLayout());
        north.add(headerBlock("Find the best match",
                "1) Choose an available donor   2) Click Find matches   "
              + "3) Select a recipient and confirm (or double-click a row)"), BorderLayout.NORTH);
        north.add(picker, BorderLayout.CENTER);

        matchModel = newModel(MATCH_COLUMNS);
        matchTable = new JTable(matchModel);
        styleTable(matchTable, 7);
        matchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);   // no sorter: order = ranking
        matchTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onConfirmMatch();
                }
            }
        });

        bestMatchLabel = new JLabel(" ");
        confirmButton = primaryButton("Confirm selected match");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> onConfirmMatch());

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        bottom.add(bestMatchLabel, BorderLayout.CENTER);
        bottom.add(confirmButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(matchTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    // History and reports tabs
    // =====================================================================

    private JPanel buildHistoryTab() {
        historyArea = readOnlyArea();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(headerBlock("Match history", "Every confirmed match, newest first."), BorderLayout.NORTH);
        panel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReportsTab() {
        reportsArea = readOnlyArea();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(headerBlock("Reports", "Live summary of the current waiting list, donors and matches."),
                BorderLayout.NORTH);
        panel.add(new JScrollPane(reportsArea), BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================
    // Button actions
    // =====================================================================

    private void onRegisterDonor() {
        try {
            Donor donor = service.registerDonor(
                    donorName.getText(),
                    (Integer) donorAge.getValue(),
                    (String) donorBlood.getSelectedItem(),
                    (String) donorOrgan.getSelectedItem(),
                    donorCity.getText());
            setStatus("Donor registered: " + donor.getName() + " (ID " + donor.getDonorId() + ", "
                    + donor.getBloodGroup() + ", " + donor.getOrganType() + ").");
            clearDonorForm();
            refreshAll();
        } catch (Exception ex) {
            showProblem(ex);
        }
    }

    private void onRegisterRecipient() {
        try {
            Recipient recipient = service.registerRecipient(
                    recName.getText(),
                    (Integer) recAge.getValue(),
                    (String) recBlood.getSelectedItem(),
                    (String) recOrgan.getSelectedItem(),
                    recCity.getText(),
                    (Integer) recUrgency.getValue());
            setStatus("Recipient registered: " + recipient.getName() + " (ID " + recipient.getRecipientId()
                    + ", " + recipient.getBloodGroup() + ", needs " + recipient.getOrganNeeded() + ").");
            clearRecipientForm();
            refreshAll();
        } catch (Exception ex) {
            showProblem(ex);
        }
    }

    private void clearDonorForm() {
        donorName.setText("");
        donorCity.setText("");
        donorAge.setValue(30);
        donorBlood.setSelectedIndex(0);
        donorOrgan.setSelectedIndex(0);
        donorName.requestFocusInWindow();
    }

    private void clearRecipientForm() {
        recName.setText("");
        recCity.setText("");
        recAge.setValue(40);
        recBlood.setSelectedIndex(0);
        recOrgan.setSelectedIndex(0);
        recUrgency.setValue(3);
        recName.requestFocusInWindow();
    }

    private void onFindMatches() {
        Donor donor = (Donor) donorPicker.getSelectedItem();
        if (donor == null) {
            showInfo("There are no available donors right now.");
            return;
        }
        try {
            List<Recipient> candidates = service.findBestMatches(donor.getDonorId());
            matchModel.setRowCount(0);
            matchesForDonor = donor;

            if (candidates.isEmpty()) {
                bestMatchLabel.setText("No compatible recipient found for " + donor.getName() + " right now.");
                confirmButton.setEnabled(false);
                setStatus("No compatible recipient for " + donor.getName() + ".");
                return;
            }

            int rank = 1;
            for (Recipient r : candidates) {
                matchModel.addRow(new Object[]{
                        rank == 1 ? "1 (best)" : String.valueOf(rank),
                        r.getRecipientId(), r.getName(), r.getAge(), r.getBloodGroup(),
                        r.getOrganNeeded(), r.getCity(), r.getUrgency(), MatchService.daysWaiting(r)});
                rank++;
            }
            matchTable.setRowSelectionInterval(0, 0);
            bestMatchLabel.setText("Ranked for " + donor.getName() + ". Best match: "
                    + candidates.get(0).getName() + ". Select a row and click Confirm.");
            confirmButton.setEnabled(true);
            setStatus("Found " + candidates.size() + " compatible recipient(s) for " + donor.getName() + ".");
        } catch (Exception ex) {
            showProblem(ex);
        }
    }

    private void onConfirmMatch() {
        int row = matchTable.getSelectedRow();
        if (matchesForDonor == null || row < 0) {
            showInfo("Select a recipient in the table first.");
            return;
        }
        int recipientId = ((Number) matchModel.getValueAt(row, 1)).intValue();
        String recipientName = (String) matchModel.getValueAt(row, 2);

        int answer = JOptionPane.showConfirmDialog(this,
                "Match donor " + matchesForDonor.getName() + " with recipient " + recipientName + "?\n"
              + "Both will be marked as matched.",
                "Confirm match", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int matchId = service.confirmMatch(matchesForDonor.getDonorId(), recipientId);
            showInfo("Match confirmed!\n" + matchesForDonor.getName() + " and " + recipientName
                    + "\nMatch ID " + matchId);
            setStatus("Match #" + matchId + " confirmed: " + matchesForDonor.getName() + " and " + recipientName + ".");
            matchModel.setRowCount(0);
            matchesForDonor = null;
            bestMatchLabel.setText(" ");
            confirmButton.setEnabled(false);
            refreshAll();
        } catch (Exception ex) {
            showProblem(ex);
        }
    }

    // =====================================================================
    // Loading data into the screens
    // =====================================================================

    private void refreshAll() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            refreshHome();
            refreshDonorPicker();
            refreshWaitingList();
            refreshHistory();
            refreshReports();
        } catch (Exception ex) {
            showProblem(ex);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void refreshWaitingListSafely() {
        try {
            refreshWaitingList();
        } catch (Exception ex) {
            showProblem(ex);
        }
    }

    private void refreshHome() throws Exception {
        statDonors.setText(String.valueOf(service.getAvailableDonors().size()));
        statWaiting.setText(String.valueOf(service.getWaitingList(null).size()));
        statMatches.setText(String.valueOf(service.getMatchHistory().size()));
        statAvgWait.setText(String.format("%.0f", service.averageWaitingDays()));
    }

    private void refreshDonorPicker() throws Exception {
        Donor selected = (Donor) donorPicker.getSelectedItem();
        int selectedId = selected == null ? -1 : selected.getDonorId();

        DefaultComboBoxModel<Donor> model = new DefaultComboBoxModel<>();
        Donor toSelect = null;
        for (Donor d : service.getAvailableDonors()) {
            model.addElement(d);
            if (d.getDonorId() == selectedId) {
                toSelect = d;
            }
        }
        donorPicker.setModel(model);
        if (toSelect != null) {
            donorPicker.setSelectedItem(toSelect);   // keep the user's choice after a refresh
        }
    }

    private void refreshWaitingList() throws Exception {
        String choice = (String) waitingOrganFilter.getSelectedItem();
        String organ = (choice == null || ALL_ORGANS.equals(choice)) ? null : choice;

        waitingModel.setRowCount(0);
        for (Recipient r : service.getWaitingList(organ)) {
            waitingModel.addRow(new Object[]{
                    r.getRecipientId(), r.getName(), r.getAge(), r.getBloodGroup(),
                    r.getOrganNeeded(), r.getCity(), r.getUrgency(), MatchService.daysWaiting(r)});
        }
        applyWaitingFilter();
    }

    private void refreshHistory() throws Exception {
        List<String> lines = service.getMatchHistory();
        if (lines.isEmpty()) {
            historyArea.setText("No matches yet.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        historyArea.setText(sb.toString());
        historyArea.setCaretPosition(0);
    }

    private void refreshReports() throws Exception {
        StringBuilder sb = new StringBuilder();
        appendCounts(sb, "Recipients waiting, by organ", service.waitingCountByOrgan());
        sb.append(String.format("Average waiting time (current waiting list): %.1f days%n%n",
                service.averageWaitingDays()));
        appendCounts(sb, "Available donors, by blood group", service.availableDonorsByBloodGroup());
        appendCounts(sb, "Confirmed matches, by organ", service.matchesByOrgan());
        reportsArea.setText(sb.toString());
        reportsArea.setCaretPosition(0);
    }

    // One line per entry with a simple text bar, e.g.  Kidney   8  ████████
    private void appendCounts(StringBuilder sb, String title, Map<String, Integer> counts) {
        sb.append(title).append("\n");
        sb.append("-".repeat(title.length())).append("\n");
        if (counts.isEmpty()) {
            sb.append("  (none)\n\n");
            return;
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append(String.format("  %-10s %3d  %s%n",
                    e.getKey(), e.getValue(), "\u2588".repeat(Math.min(e.getValue(), 40))));
        }
        sb.append("\n");
    }

    // =====================================================================
    // Small helpers
    // =====================================================================

    // Title and one-line hint shown at the top of a tab
    private JPanel headerBlock(String title, String hint) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        block.add(titleLabel);
        block.add(Box.createVerticalStrut(4));
        block.add(hintLabel);
        return block;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setMargin(new Insets(6, 16, 6, 16));
        return button;
    }

    // Row height, bold header, and urgency colouring (urgencyColumn = position of the Urgency column)
    private void styleTable(JTable table, int urgencyColumn) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        table.setDefaultRenderer(Object.class, new UrgencyRenderer(urgencyColumn));
    }

    // A table model whose cells cannot be edited by typing in them
    private static DefaultTableModel newModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTextArea readOnlyArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setMargin(new Insets(10, 14, 10, 14));
        return area;
    }

    // Pressing Enter submits the form (or runs the search) of the tab on screen
    private void updateDefaultButton() {
        int tab = tabs.getSelectedIndex();
        if (tab == TAB_DONOR) {
            getRootPane().setDefaultButton(donorSubmit);
        } else if (tab == TAB_RECIPIENT) {
            getRootPane().setDefaultButton(recSubmit);
        } else if (tab == TAB_MATCH) {
            getRootPane().setDefaultButton(findButton);
        } else {
            getRootPane().setDefaultButton(null);
        }
    }

    private void setStatus(String message) {
        statusBar.setText(message);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Organ Donor Matching", JOptionPane.INFORMATION_MESSAGE);
    }

    // Validation problems get a friendly warning; anything else is shown as an error
    private void showProblem(Exception ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        if (ex instanceof IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, message, "Please check your input", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, message, "Could not complete the action", JOptionPane.ERROR_MESSAGE);
        }
        setStatus("Problem: " + message);
    }

    // Colours a whole row by the recipient's urgency (5 red, 4 orange, 3 yellow)
    private static class UrgencyRenderer extends DefaultTableCellRenderer {
        private final int urgencyModelColumn;

        UrgencyRenderer(int urgencyModelColumn) {
            this.urgencyModelColumn = urgencyModelColumn;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                Object u = table.getModel().getValueAt(modelRow, urgencyModelColumn);
                int urgency = (u instanceof Number) ? ((Number) u).intValue() : 0;
                if (urgency >= 5) {
                    c.setBackground(URGENT_5);
                } else if (urgency == 4) {
                    c.setBackground(URGENT_4);
                } else if (urgency == 3) {
                    c.setBackground(URGENT_3);
                } else {
                    c.setBackground(Color.WHITE);
                }
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    // Applies one font to every Swing component so the whole window looks consistent
    private static void setDefaultFont(Font font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font));
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look
        }
        setDefaultFont(new Font("Segoe UI", Font.PLAIN, 14));
        SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }
}
