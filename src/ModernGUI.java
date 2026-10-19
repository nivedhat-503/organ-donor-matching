import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModernGUI extends JFrame {

    interface Backend {
        List<String> bloodGroups();
        List<String> organs();
        void registerDonor(String name, int age, String bloodGroup, String organ, String city) throws Exception;
        void registerRecipient(String name, int age, String bloodGroup, String organ, int urgency, String city) throws Exception;
        List<Object[]> waitingList();
        List<Object[]> availableDonors();
        List<Object[]> findMatches(int donorId) throws Exception;
        void confirmMatch(int donorId, int recipientId) throws Exception;
        List<Object[]> history();
        int[] stats();
    }

    static final Color SIDEBAR = new Color(0x1F2937);
    static final Color ACCENT = new Color(0x0F766E);
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
        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JLabel disclaimer = new JLabel("Educational demo with fictional data. Not for real medical or allocation decisions.");
        disclaimer.setFont(BASE.deriveFont(12f));
        disclaimer.setForeground(MUTED);
        disclaimer.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));
        footer.add(status, BorderLayout.WEST);
        footer.add(disclaimer, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        show("Dashboard");
    }

    // sidebar and page switching

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
        brand.setBorder(BorderFactory.createEmptyBorder(0, 8, 24, 0));
        side.add(brand);

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

    // helpers

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

        // without this, 10 sorts before 9
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(m);
        Comparator<Object> cmp = new Comparator<Object>() {
            public int compare(Object a, Object b) {
                if (a instanceof Number && b instanceof Number) {
                    return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
                }
                return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
            }
        };
        for (int c = 0; c < cols.length; c++) sorter.setComparator(c, cmp);
        t.setRowSorter(sorter);

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

    // dashboard

    private JComponent buildDashboard() {
        JPanel p = page("Dashboard", "Current numbers");
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

    // forms

    private JComponent buildDonorForm() {
        JPanel p = page("Register Donor", "All fields are required. Donors must be 18 to 65.");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        final JTextField name = field();
        final JSpinner age = new JSpinner(new SpinnerNumberModel(30, 18, 65, 1));
        age.setFont(BASE);
        final JComboBox<String> bg = combo(backend.bloodGroups());
        final JComboBox<String> organ = combo(backend.organs());
        final JTextField city = field();
        addRow(form, 0, "Name *", name);
        addRow(form, 1, "Age *", age);
        addRow(form, 2, "Blood group *", bg);
        addRow(form, 3, "Organ *", organ);
        addRow(form, 4, "City *", city);

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
        JPanel p = page("Register Recipient", "All fields are required. Urgency 5 is the most urgent.");
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
        addRow(form, 5, "City *", city);

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

    // waiting list

    private JComponent buildWaitingList() {
        JPanel p = page("Waiting List", "Search by name or city, or filter by organ.");
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
        t.getColumnModel().getColumn(7).setPreferredWidth(110);

        final JTextField search = field();
        search.setToolTipText("Name or city");
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

    // find match

    private int rankedForDonor = -1;

    private JComponent buildFindMatch() {
        JPanel p = page("Find Match", "Pick a donor, find matches, then confirm one.");
        final JComboBox<String> donors = new JComboBox<String>();
        donors.setFont(BASE);
        final List<Integer> donorIds = new ArrayList<Integer>();
        final JTable t = table(new String[]{"Rank", "ID", "Name", "Blood", "Urgency", "Days waiting", "City"});
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
                    JOptionPane.showMessageDialog(ModernGUI.this, "No available donors.", "Find match", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                try {
                    int donorId = donorIds.get(i);
                    List<Object[]> rows = backend.findMatches(donorId);
                    fill(t, rows);
                    rankedForDonor = donorId;
                    if (rows.isEmpty()) {
                        confirm.setEnabled(false);
                        say("No compatible recipient for donor #" + donorId);
                    } else {
                        t.setRowSelectionInterval(0, 0);
                        confirm.setEnabled(true);
                        say(rows.size() + " candidate(s) for donor #" + donorId + ", best one selected");
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

    // history

    private JComponent buildHistory() {
        JPanel p = page("History", "Confirmed matches");
        final JTable t = table(new String[]{"Donor", "Recipient", "Organ", "Date"});
        p.add(scroll(t), BorderLayout.CENTER);
        refreshers.put("History", new Runnable() {
            public void run() { fill(t, backend.history()); }
        });
        return p;
    }

    // backend implementation

    static class ServiceBackend implements Backend {
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final MatchService service = new MatchService();
        private final DonorDAO donorDAO = new DonorDAO();
        private final RecipientDAO recipientDAO = new RecipientDAO();
        private final MatchDAO matchDAO = new MatchDAO();

        static void checkConnection() throws Exception {
            try (java.sql.Connection con = DBConnection.getConnection()) {
                con.isValid(2);
            }
        }

        public List<String> bloodGroups() { return MatchService.BLOOD_GROUPS; }

        public List<String> organs() { return MatchService.ORGANS; }

        public void registerDonor(String name, int age, String bloodGroup, String organ, String city) throws Exception {
            service.registerDonor(name, age, bloodGroup, organ, city);
        }

        public void registerRecipient(String name, int age, String bloodGroup, String organ,
                                      int urgency, String city) throws Exception {
            // service takes city before urgency
            service.registerRecipient(name, age, bloodGroup, organ, city, urgency);
        }

        public List<Object[]> waitingList() {
            try {
                List<Object[]> rows = new ArrayList<Object[]>();
                for (Recipient r : service.getWaitingList(null)) {
                    rows.add(new Object[]{
                            r.getRecipientId(), r.getName(), r.getAge(), r.getBloodGroup(),
                            r.getOrganNeeded(), r.getUrgency(), r.getCity(), MatchService.daysWaiting(r)});
                }
                return rows;
            } catch (Exception e) {
                throw failure("load the waiting list", e);
            }
        }

        public List<Object[]> availableDonors() {
            try {
                List<Object[]> rows = new ArrayList<Object[]>();
                for (Donor d : service.getAvailableDonors()) {
                    rows.add(new Object[]{d.getDonorId(), d.getName(), d.getBloodGroup(), d.getOrganType(), d.getCity()});
                }
                return rows;
            } catch (Exception e) {
                throw failure("load the available donors", e);
            }
        }

        // already sorted by the query
        public List<Object[]> findMatches(int donorId) throws Exception {
            List<Object[]> rows = new ArrayList<Object[]>();
            int rank = 1;
            for (Recipient r : service.findBestMatches(donorId)) {
                rows.add(new Object[]{
                        rank == 1 ? "1 (best)" : String.valueOf(rank),
                        r.getRecipientId(), r.getName(), r.getBloodGroup(),
                        r.getUrgency(), MatchService.daysWaiting(r), r.getCity()});
                rank++;
            }
            return rows;
        }

        public void confirmMatch(int donorId, int recipientId) throws Exception {
            service.confirmMatch(donorId, recipientId);
        }

        public List<Object[]> history() {
            try {
                List<Object[]> rows = new ArrayList<Object[]>();
                for (MatchRecord m : matchDAO.getAllMatches()) {
                    Donor d = donorDAO.getDonorById(m.getDonorId());
                    Recipient r = recipientDAO.getRecipientById(m.getRecipientId());
                    rows.add(new Object[]{
                            d == null ? "(unknown)" : d.getName(),
                            r == null ? "(unknown)" : r.getName(),
                            d == null ? "" : d.getOrganType(),
                            m.getMatchDate().format(DATE_FORMAT)});
                }
                return rows;
            } catch (Exception e) {
                throw failure("load the match history", e);
            }
        }

        public int[] stats() {
            try {
                return new int[]{
                        service.getAvailableDonors().size(),
                        service.getWaitingList(null).size(),
                        service.getMatchHistory().size(),
                        (int) Math.round(service.averageWaitingDays())};
            } catch (Exception e) {
                throw failure("load the dashboard numbers", e);
            }
        }

        // list methods can't throw checked exceptions, so wrap them
        private static IllegalStateException failure(String action, Exception e) {
            return new IllegalStateException("Could not " + action + ": " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Something went wrong", JOptionPane.ERROR_MESSAGE);
            }
        });

        try {
            ServiceBackend.checkConnection();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the database:\n" + ex.getMessage()
                  + "\n\nCheck that MySQL is running and the password in config.properties is correct.",
                    "Database connection failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ModernGUI(new ServiceBackend()).setVisible(true);
            }
        });
    }
}