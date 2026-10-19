import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ModernGUI - a fresh Swing front end with a sidebar layout.
 *
 * All data access goes through the small Backend interface below.
 * DemoBackend (in-memory sample data) lets the window run without a database.
 * To connect your real project, implement Backend using MatchService (see ServiceBackend
 * at the bottom of this file) and change the one line marked  <<< SWITCH BACKEND HERE.
 */
public class ModernGUI extends JFrame {

    // ------------------------------------------------------------------ backend contract
    interface Backend {
        List<String> bloodGroups();
        List<String> organs();
        void registerDonor(String name, int age, String bloodGroup, String organ, String city) throws Exception;
        void registerRecipient(String name, int age, String bloodGroup, String organ, int urgency, String city) throws Exception;
        /** rows: id, name, age, blood group, organ, urgency, city, days waiting */
        List<Object[]> waitingList();
        /** rows: id, name, blood group, organ, city */
        List<Object[]> availableDonors();
        /** rows: rank, recipient id, name, blood group, urgency, days waiting, score */
        List<Object[]> findMatches(int donorId) throws Exception;
        void confirmMatch(int donorId, int recipientId) throws Exception;
        /** rows: donor name, recipient name, organ, date */
        List<Object[]> history();
        /** availableDonors, recipientsWaiting, matchesMade, averageWaitDays */
        int[] stats();
    }

    // ------------------------------------------------------------------ theme
    static final Color SIDEBAR = new Color(0x1F2937);
    static final Color SIDEBAR_HOVER = new Color(0x374151);
    static final Color ACCENT = new Color(0x0F766E);
    static final Color ACCENT_DARK = new Color(0x115E59);
    static final Color BG = new Color(0xF3F4F6);
    static final Color CARD = Color.WHITE;
    static final Color TEXT = new Color(0x111827);
    static final Color MUTED = new Color(0x6B7280);
    static final Color LINE = new Color(0xE5E7EB);
    static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font BOLD = BASE.deriveFont(Font.BOLD);
    static final Font TITLE = BASE.deriveFont(Font.BOLD, 24f);
    static final Font BIG = BASE.deriveFont(Font.BOLD, 30f);

    private final Backend backend;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel status = new JLabel(" Ready");
    private final java.util.Map<String, JButton> navButtons = new java.util.LinkedHashMap<String, JButton>();
    private final java.util.Map<String, Runnable> refreshers = new java.util.HashMap<String, Runnable>();

    public ModernGUI(Backend backend) {
        super("Organ Donor Matching");
        this.backend = backend;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);

        content.setBackground(BG);
        addPage("Dashboard", buildDashboard());
        addPage("Register Donor", buildDonorForm());
        addPage("Register Recipient", buildRecipientForm());
        addPage("Waiting List", buildWaitingList());
        addPage("Find Match", buildFindMatch());
        addPage("History", buildHistory());
        root.add(content, BorderLayout.CENTER);

        status.setFont(BASE.deriveFont(12f));
        status.setForeground(MUTED);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LINE), BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        status.setOpaque(true);
        status.setBackground(CARD);
        root.add(status, BorderLayout.SOUTH);

        setContentPane(root);
        show("Dashboard");
    }

    // ------------------------------------------------------------------ navigation
    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(220, 0));
        side.setBorder(BorderFactory.createEmptyBorder(20, 12, 12, 12));

        JLabel brand = new JLabel("Organ Match");
        brand.setFont(BASE.deriveFont(Font.BOLD, 20f));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));
        side.add(brand);

        JLabel sub = new JLabel("Donor & recipient matching");
        sub.setFont(BASE.deriveFont(12f));
        sub.setForeground(new Color(0x9CA3AF));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(BorderFactory.createEmptyBorder(0, 8, 24, 0));
        side.add(sub);

        for (final String name : new String[]{"Dashboard", "Register Donor", "Register Recipient",
                "Waiting List", "Find Match", "History"}) {
            final JButton b = new JButton(name);
            b.setFont(BASE);
            b.setForeground(Color.WHITE);
            b.setBackground(SIDEBAR);
            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { show(name); }
            });
            navButtons.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }
        side.add(Box.createVerticalGlue());
        return side;
    }

    private void addPage(String name, JComponent page) {
        content.add(page, name);
    }

    private void show(String name) {
        for (java.util.Map.Entry<String, JButton> en : navButtons.entrySet()) {
            boolean sel = en.getKey().equals(name);
            en.getValue().setBackground(sel ? ACCENT : SIDEBAR);
            en.getValue().setFont(sel ? BOLD : BASE);
        }
        Runnable r = refreshers.get(name);
        if (r != null) r.run();
        cards.show(content, name);
    }

    private void say(String msg) { status.setText(" " + msg); }

    // ------------------------------------------------------------------ shared widgets
    private static JPanel page(String title, String hint) {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(TITLE);
        t.setForeground(TEXT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel h = new JLabel(hint);
        h.setFont(BASE);
        h.setForeground(MUTED);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(t);
        head.add(Box.createVerticalStrut(4));
        head.add(h);
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    private static JPanel card() {
        JPanel c = new JPanel();
        c.setBackground(CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return c;
    }

    private static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setFont(BASE);
        b.setForeground(TEXT);
        b.setBackground(new Color(0xE5E7EB));
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        l.setForeground(TEXT);
        return l;
    }

    private static JTable table(String[] cols) {
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setFont(BASE);
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setGridColor(LINE);
        t.setSelectionBackground(new Color(0xCCFBF1));
        t.setSelectionForeground(TEXT);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setBackground(new Color(0xF9FAFB));
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.setFillsViewportHeight(true);
        return t;
    }

    private static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(CARD);
        return sp;
    }

    private static void fill(JTable t, List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    private static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(BASE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return f;
    }

    private static JComboBox<String> combo(List<String> items) {
        JComboBox<String> c = new JComboBox<String>(items.toArray(new String[0]));
        c.setFont(BASE);
        return c;
    }

    private static void addRow(JPanel form, int row, String text, JComponent input) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = row; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(8, 0, 8, 16);
        form.add(label(text), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(8, 0, 8, 0);
        form.add(input, g);
    }

    // ------------------------------------------------------------------ dashboard
    private JComponent buildDashboard() {
        JPanel p = page("Dashboard", "A quick look at the current state of the matching system.");
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        final JLabel[] values = new JLabel[4];
        String[] names = {"Available donors", "Recipients waiting", "Matches made", "Avg. wait (days)"};
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 4; i++) {
            JPanel c = card();
            c.setLayout(new BorderLayout());
            values[i] = new JLabel("0");
            values[i].setFont(BIG);
            values[i].setForeground(ACCENT);
            JLabel n = new JLabel(names[i]);
            n.setFont(BASE);
            n.setForeground(MUTED);
            c.add(values[i], BorderLayout.CENTER);
            c.add(n, BorderLayout.SOUTH);
            row.add(c);
        }
        body.add(Box.createVerticalStrut(8));
        body.add(row);
        body.add(Box.createVerticalStrut(20));

        JPanel actions = card();
        actions.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        actions.add(label("Quick actions:"));
        actions.add(quick("Register donor", "Register Donor"));
        actions.add(quick("Register recipient", "Register Recipient"));
        actions.add(quick("Find a match", "Find Match"));
        body.add(actions);
        body.add(Box.createVerticalStrut(20));

        JPanel how = card();
        how.setLayout(new BoxLayout(how, BoxLayout.Y_AXIS));
        how.setAlignmentX(Component.LEFT_ALIGNMENT);
        how.add(label("How matching works"));
        how.add(Box.createVerticalStrut(6));
        JLabel txt = new JLabel("<html>A recipient is a candidate when blood group and organ are compatible with the donor. "
                + "Candidates are ranked by urgency first, then by how long they have waited. "
                + "The coordinator always confirms the final match; the ranking is a suggestion, not a medical decision.</html>");
        txt.setFont(BASE);
        txt.setForeground(MUTED);
        how.add(txt);
        how.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        body.add(how);

        p.add(body, BorderLayout.CENTER);
        refreshers.put("Dashboard", new Runnable() {
            public void run() {
                int[] s = backend.stats();
                for (int i = 0; i < 4; i++) values[i].setText(String.valueOf(s[i]));
            }
        });
        return p;
    }

    private JButton quick(String text, final String target) {
        JButton b = primary(text);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show(target); }
        });
        return b;
    }

    // ------------------------------------------------------------------ registration forms
    private JComponent buildDonorForm() {
        JPanel p = page("Register Donor", "Fields marked * are required.");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        final JTextField name = field();
        final JSpinner age = new JSpinner(new SpinnerNumberModel(30, 18, 80, 1));
        age.setFont(BASE);
        final JComboBox<String> bg = combo(backend.bloodGroups());
        final JComboBox<String> organ = combo(backend.organs());
        final JTextField city = field();
        addRow(form, 0, "Name *", name);
        addRow(form, 1, "Age *", age);
        addRow(form, 2, "Blood group *", bg);
        addRow(form, 3, "Organ *", organ);
        addRow(form, 4, "City", city);

        final JButton submit = primary("Register donor");
        JButton clear = secondary("Clear");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.add(submit);
        btns.add(clear);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1; g.gridy = 5; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(16, 0, 0, 0);
        form.add(btns, g);

        final Runnable reset = new Runnable() {
            public void run() {
                name.setText(""); city.setText(""); age.setValue(30);
                bg.setSelectedIndex(0); organ.setSelectedIndex(0); name.requestFocusInWindow();
            }
        };
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { reset.run(); }
        });
        ActionListener go = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    backend.registerDonor(name.getText().trim(), (Integer) age.getValue(),
                            (String) bg.getSelectedItem(), (String) organ.getSelectedItem(), city.getText().trim());
                    say("Donor registered: " + name.getText().trim() + " (" + bg.getSelectedItem() + ", " + organ.getSelectedItem() + ")");
                    reset.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ModernGUI.this, ex.getMessage(), "Cannot register donor", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        submit.addActionListener(go);
        name.addActionListener(go);
        city.addActionListener(go);

        JPanel c = card();
        c.setLayout(new BorderLayout());
        c.add(form, BorderLayout.NORTH);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        c.setPreferredSize(new Dimension(520, 380));
        wrap.add(c);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildRecipientForm() {
        JPanel p = page("Register Recipient", "Add a patient to the waiting list. Urgency 5 means most urgent.");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        final JTextField name = field();
        final JSpinner age = new JSpinner(new SpinnerNumberModel(30, 1, 100, 1));
        age.setFont(BASE);
        final JComboBox<String> bg = combo(backend.bloodGroups());
        final JComboBox<String> organ = combo(backend.organs());
        final JSpinner urgency = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        urgency.setFont(BASE);
        final JTextField city = field();
        addRow(form, 0, "Name *", name);
        addRow(form, 1, "Age *", age);
        addRow(form, 2, "Blood group *", bg);
        addRow(form, 3, "Organ needed *", organ);
        addRow(form, 4, "Urgency (1-5) *", urgency);
        addRow(form, 5, "City", city);

        final JButton submit = primary("Add to waiting list");
        JButton clear = secondary("Clear");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.add(submit);
        btns.add(clear);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1; g.gridy = 6; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(16, 0, 0, 0);
        form.add(btns, g);

        final Runnable reset = new Runnable() {
            public void run() {
                name.setText(""); city.setText(""); age.setValue(30); urgency.setValue(3);
                bg.setSelectedIndex(0); organ.setSelectedIndex(0); name.requestFocusInWindow();
            }
        };
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { reset.run(); }
        });
        ActionListener go = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    backend.registerRecipient(name.getText().trim(), (Integer) age.getValue(),
                            (String) bg.getSelectedItem(), (String) organ.getSelectedItem(),
                            (Integer) urgency.getValue(), city.getText().trim());
                    say("Recipient added: " + name.getText().trim() + " (" + bg.getSelectedItem() + ", " + organ.getSelectedItem()
                            + ", urgency " + urgency.getValue() + ")");
                    reset.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ModernGUI.this, ex.getMessage(), "Cannot register recipient", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        submit.addActionListener(go);
        name.addActionListener(go);
        city.addActionListener(go);

        JPanel c = card();
        c.setLayout(new BorderLayout());
        c.add(form, BorderLayout.NORTH);
        c.setPreferredSize(new Dimension(520, 440));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(c);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ waiting list
    private JComponent buildWaitingList() {
        JPanel p = page("Waiting List", "Search by name or city, or filter by organ. Rows are colored by urgency.");
        final JTable t = table(new String[]{"ID", "Name", "Age", "Blood", "Organ", "Urgency", "City", "Days waiting"});
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                if (!sel) {
                    int u = 0;
                    try { u = Integer.parseInt(String.valueOf(tb.getValueAt(r, 5))); } catch (Exception ignored) { }
                    comp.setBackground(u >= 5 ? new Color(0xFEE2E2) : u == 4 ? new Color(0xFFEDD5) : u == 3 ? new Color(0xFEF9C3) : Color.WHITE);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });

        t.getColumnModel().getColumn(0).setPreferredWidth(40);
        t.getColumnModel().getColumn(1).setPreferredWidth(180);
        t.getColumnModel().getColumn(2).setPreferredWidth(50);
        t.getColumnModel().getColumn(3).setPreferredWidth(60);

        final JTextField search = field();
        search.setToolTipText("Type a name or city to filter the list");
        final JComboBox<String> organ = combo(new ArrayList<String>());
        organ.addItem("All organs");
        for (String o : backend.organs()) organ.addItem(o);
        final JLabel count = new JLabel(" ");
        count.setFont(BASE);
        count.setForeground(MUTED);

        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(search, BorderLayout.CENTER);
        left.add(organ, BorderLayout.EAST);
        bar.add(left, BorderLayout.CENTER);
        bar.add(count, BorderLayout.EAST);

        final Runnable load = new Runnable() {
            public void run() {
                String q = search.getText().trim().toLowerCase();
                String o = (String) organ.getSelectedItem();
                List<Object[]> rows = new ArrayList<Object[]>();
                for (Object[] r : backend.waitingList()) {
                    boolean okOrgan = "All organs".equals(o) || String.valueOf(r[4]).equals(o);
                    boolean okText = q.isEmpty() || String.valueOf(r[1]).toLowerCase().contains(q)
                            || String.valueOf(r[6]).toLowerCase().contains(q);
                    if (okOrgan && okText) rows.add(r);
                }
                fill(t, rows);
                count.setText(rows.size() + " shown");
            }
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { load.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { load.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { load.run(); }
        });
        organ.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { load.run(); }
        });

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(scroll(t), BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        refreshers.put("Waiting List", load);
        return p;
    }

    // ------------------------------------------------------------------ find match
    private int rankedForDonor = -1;

    private JComponent buildFindMatch() {
        JPanel p = page("Find Match", "1. Choose a donor   2. Find matches   3. Select a recipient and confirm.");
        final JComboBox<String> donors = new JComboBox<String>();
        donors.setFont(BASE);
        final List<Integer> donorIds = new ArrayList<Integer>();
        final JTable t = table(new String[]{"Rank", "ID", "Name", "Blood", "Urgency", "Days waiting", "Score"});
        final JButton find = primary("Find matches");
        final JButton confirm = primary("Confirm selected match");
        confirm.setEnabled(false);

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(donors, BorderLayout.CENTER);
        top.add(find, BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(confirm);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(scroll(t), BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        refreshers.put("Find Match", new Runnable() {
            public void run() {
                donors.removeAllItems();
                donorIds.clear();
                for (Object[] d : backend.availableDonors()) {
                    donorIds.add((Integer) d[0]);
                    donors.addItem("#" + d[0] + "  " + d[1] + "  |  " + d[2] + "  " + d[3] + "  |  " + d[4]);
                }
                fill(t, new ArrayList<Object[]>());
                confirm.setEnabled(false);
                rankedForDonor = -1;
            }
        });

        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = donors.getSelectedIndex();
                if (i < 0) {
                    JOptionPane.showMessageDialog(ModernGUI.this, "There are no available donors.", "Find match", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                try {
                    int donorId = donorIds.get(i);
                    List<Object[]> rows = backend.findMatches(donorId);
                    fill(t, rows);
                    rankedForDonor = donorId;
                    if (rows.isEmpty()) {
                        confirm.setEnabled(false);
                        say("No compatible recipient found for donor #" + donorId);
                    } else {
                        t.setRowSelectionInterval(0, 0);
                        confirm.setEnabled(true);
                        say(rows.size() + " candidate(s) found for donor #" + donorId + ". The best match is selected.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ModernGUI.this, ex.getMessage(), "Find match", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        final Runnable doConfirm = new Runnable() {
            public void run() {
                int row = t.getSelectedRow();
                if (row < 0 || rankedForDonor < 0) return;
                row = t.convertRowIndexToModel(row);
                DefaultTableModel m = (DefaultTableModel) t.getModel();
                int recipientId = (Integer) m.getValueAt(row, 1);
                String rname = String.valueOf(m.getValueAt(row, 2));
                int ok = JOptionPane.showConfirmDialog(ModernGUI.this,
                        "Match donor #" + rankedForDonor + " with " + rname + " (ID " + recipientId + ")?\nThis cannot be undone.",
                        "Confirm match", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ok != JOptionPane.OK_OPTION) return;
                try {
                    backend.confirmMatch(rankedForDonor, recipientId);
                    say("Match confirmed: donor #" + rankedForDonor + " -> " + rname);
                    refreshers.get("Find Match").run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ModernGUI.this, ex.getMessage(), "Cannot confirm match", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        confirm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doConfirm.run(); }
        });
        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) doConfirm.run();
            }
        });
        return p;
    }

    // ------------------------------------------------------------------ history
    private JComponent buildHistory() {
        JPanel p = page("History", "All confirmed matches.");
        final JTable t = table(new String[]{"Donor", "Recipient", "Organ", "Date"});
        p.add(scroll(t), BorderLayout.CENTER);
        refreshers.put("History", new Runnable() {
            public void run() { fill(t, backend.history()); }
        });
        return p;
    }

    // ------------------------------------------------------------------ demo backend (no database)
    static class DemoBackend implements Backend {
        private static final List<String> BG = Arrays.asList("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+");
        private static final List<String> ORGANS = Arrays.asList("Kidney", "Liver", "Heart", "Lung", "Pancreas");
        private int nextId = 1;
        private final List<Object[]> donors = new ArrayList<Object[]>();      // id,name,bg,organ,city,available
        private final List<Object[]> recipients = new ArrayList<Object[]>();  // id,name,age,bg,organ,urgency,city,days,waiting
        private final List<Object[]> history = new ArrayList<Object[]>();

        DemoBackend() {
            registerDonorQuiet("Arjun Krishnan", "O-", "Kidney", "Chennai");
            registerDonorQuiet("Divya Nair", "A+", "Liver", "Kochi");
            registerDonorQuiet("Karthik Rao", "B+", "Kidney", "Bengaluru");
            addR("Balaji Murugan", 45, "O+", "Kidney", 5, "Chennai", 120);
            addR("Sana Iqbal", 38, "A+", "Liver", 4, "Hyderabad", 90);
            addR("Ravi Shankar", 52, "B+", "Kidney", 3, "Bengaluru", 200);
            addR("Latha Menon", 29, "AB+", "Kidney", 2, "Kochi", 40);
        }

        private void registerDonorQuiet(String n, String bg, String o, String c) {
            donors.add(new Object[]{nextId++, n, bg, o, c, Boolean.TRUE});
        }
        private void addR(String n, int a, String bg, String o, int u, String c, int d) {
            recipients.add(new Object[]{nextId++, n, a, bg, o, u, c, d, Boolean.TRUE});
        }

        public List<String> bloodGroups() { return BG; }
        public List<String> organs() { return ORGANS; }

        public void registerDonor(String name, int age, String bg, String organ, String city) throws Exception {
            if (name.isEmpty()) throw new Exception("Name must not be empty.");
            if (age < 18) throw new Exception("Donor must be at least 18 years old.");
            for (Object[] d : donors)
                if (d[1].toString().equalsIgnoreCase(name) && d[2].equals(bg) && d[3].equals(organ))
                    throw new Exception("This donor is already registered.");
            donors.add(new Object[]{nextId++, name, bg, organ, city, Boolean.TRUE});
        }

        public void registerRecipient(String name, int age, String bg, String organ, int urgency, String city) throws Exception {
            if (name.isEmpty()) throw new Exception("Name must not be empty.");
            if (urgency < 1 || urgency > 5) throw new Exception("Urgency must be between 1 and 5.");
            for (Object[] r : recipients)
                if (r[1].toString().equalsIgnoreCase(name) && r[3].equals(bg) && r[4].equals(organ) && (Boolean) r[8])
                    throw new Exception("This recipient is already on the waiting list.");
            recipients.add(new Object[]{nextId++, name, age, bg, organ, urgency, city, 0, Boolean.TRUE});
        }

        public List<Object[]> waitingList() {
            List<Object[]> out = new ArrayList<Object[]>();
            for (Object[] r : recipients)
                if ((Boolean) r[8]) out.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7]});
            return out;
        }

        public List<Object[]> availableDonors() {
            List<Object[]> out = new ArrayList<Object[]>();
            for (Object[] d : donors)
                if ((Boolean) d[5]) out.add(new Object[]{d[0], d[1], d[2], d[3], d[4]});
            return out;
        }

        private static boolean compatible(String donor, String recipient) {
            boolean donorNeg = donor.endsWith("-");
            boolean recipNeg = recipient.endsWith("-");
            if (donorNeg == false && recipNeg) return false;          // Rh+ cannot give to Rh-
            String d = donor.substring(0, donor.length() - 1);
            String r = recipient.substring(0, recipient.length() - 1);
            if (d.equals("O")) return true;
            if (r.equals("AB")) return true;
            return d.equals(r);
        }

        private Object[] find(List<Object[]> list, int id) {
            for (Object[] o : list) if ((Integer) o[0] == id) return o;
            return null;
        }

        public List<Object[]> findMatches(int donorId) throws Exception {
            Object[] d = find(donors, donorId);
            if (d == null) throw new Exception("Donor not found.");
            List<Object[]> cands = new ArrayList<Object[]>();
            for (Object[] r : recipients) {
                if (!(Boolean) r[8] || !r[4].equals(d[3]) || !compatible((String) d[2], (String) r[3])) continue;
                int score = (Integer) r[5] * 100 + (Integer) r[7];
                cands.add(new Object[]{0, r[0], r[1], r[3], r[5], r[7], score});
            }
            java.util.Collections.sort(cands, new java.util.Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) { return (Integer) b[6] - (Integer) a[6]; }
            });
            for (int i = 0; i < cands.size(); i++) cands.get(i)[0] = (i == 0) ? "1 (best)" : String.valueOf(i + 1);
            return cands;
        }

        public void confirmMatch(int donorId, int recipientId) throws Exception {
            Object[] d = find(donors, donorId);
            Object[] r = find(recipients, recipientId);
            if (d == null || r == null || !(Boolean) d[5] || !(Boolean) r[8]) throw new Exception("Donor or recipient is no longer available.");
            d[5] = Boolean.FALSE;
            r[8] = Boolean.FALSE;
            history.add(new Object[]{d[1], r[1], d[3], new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date())});
        }

        public List<Object[]> history() { return new ArrayList<Object[]>(history); }

        public int[] stats() {
            int ad = availableDonors().size();
            List<Object[]> w = waitingList();
            int sum = 0;
            for (Object[] r : w) sum += (Integer) r[7];
            return new int[]{ad, w.size(), history.size(), w.isEmpty() ? 0 : sum / w.size()};
        }
    }

    // ------------------------------------------------------------------ entry point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Backend b = new DemoBackend();                 // <<< SWITCH BACKEND HERE (e.g. new ServiceBackend())
                new ModernGUI(b).setVisible(true);
            }
        });
    }

    /*
     * Wire-up sketch for your real project. Fill in the calls with your MatchService
     * method names, convert the results to Object[] rows as described in Backend, then
     * replace  new DemoBackend()  in main() with  new ServiceBackend().
     *
     * static class ServiceBackend implements Backend {
     *     private final MatchService service = new MatchService();
     *     public List<String> bloodGroups() { return Arrays.asList(MatchService.BLOOD_GROUPS); }
     *     public List<String> organs()      { return Arrays.asList(MatchService.ORGANS); }
     *     ... one method per Backend method, calling service.registerDonor(...), service.findBestMatches(id), etc.
     * }
     */
}