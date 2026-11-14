package com.dragon.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DragonLauncher extends JFrame {
    private static final String OWNER_LOGIN = "admin";
    private static final String OWNER_PASS  = "dragon2026";
    private static final String BIN_URL     = "https://npoint.io";
    private static final String ZIP_URL     = "https://github.io";

    private int mouseX, mouseY;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final List<JButton> navButtons = new ArrayList<>();
    private String activeTab = "PLAY";

    private JButton sideAuthBtn;
    private JLabel sideUserRankLbl;

    private JTextField loginField;
    private JPasswordField passField;
    private JLabel loginStatus;
    private JButton loginBtn;

    private JTextField mcNickField;
    private JTextField mcPathField;
    private JLabel mainStatus;
    private JLabel clientSyncLbl;
    private JButton playBtn;

    private JPanel configsListPanel;
    private JPanel vipCreateBox;
    private JLabel cfgStatusLbl;

    private String currentUser = "";
    private String currentRank = "";

    public DragonLauncher() {
        setUndecorated(true);
        setSize(780, 490);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        setShape(new RoundRectangle2D.Double(0, 0, 780, 490, 26, 26));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(11, 8, 22), getWidth(), getHeight(), new Color(26, 12, 48));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.setColor(new Color(168, 85, 247, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 26, 26);
                g2.dispose();
            }
        };
        root.setOpaque(false);

        root.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) { setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY); }
        });

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.add(buildTopWindowBar(), BorderLayout.NORTH);

        contentPanel.setOpaque(false);
        contentPanel.add(buildPlayTab(), "PLAY");
        contentPanel.add(buildConfigsTab(), "CONFIGS");
        contentPanel.add(buildSettingsTab(), "SETTINGS");
        contentPanel.add(buildLoginTab(), "LOGIN");

        rightWrapper.add(contentPanel, BorderLayout.CENTER);
        root.add(rightWrapper, BorderLayout.CENTER);
        add(root);

        switchTab("PLAY");
    }

    private JPanel buildTopWindowBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 24, 4, 14));

        JLabel verTag = new JLabel("MINECRAFT 1.21.4 • FABRIC");
        verTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        verTag.setForeground(new Color(167, 139, 250));

        JPanel winBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        winBtns.setOpaque(false);
        JButton minBtn = createWinBtn("—", new Color(160, 165, 185));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        JButton closeBtn = createWinBtn("✕", new Color(248, 113, 113));
        closeBtn.addActionListener(e -> System.exit(0));
        winBtns.add(minBtn);
        winBtns.add(closeBtn);

        bar.add(verTag, BorderLayout.WEST);
        bar.add(winBtns, BorderLayout.EAST);
        return bar;
    }

    private JButton createWinBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 12, 30, 235));
                g2.fillRoundRect(0, 0, getWidth() + 20, getHeight(), 26, 26);
                g2.setColor(new Color(192, 132, 252, 45));
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(200, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(22, 16, 20, 16));

        JLabel logo = new JLabel("✦ DRAGONVISUALS");
        logo.setFont(new Font("SansSerif", Font.BOLD, 13));
        logo.setForeground(new Color(233, 213, 255));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        side.add(logo);
        side.add(Box.createVerticalStrut(30));

        side.add(createNavBtn("▶  Запуск", "PLAY"));
        side.add(Box.createVerticalStrut(8));
        side.add(createNavBtn("⚙  Конфиги", "CONFIGS"));
        side.add(Box.createVerticalStrut(8));
        side.add(createNavBtn("☰  Настройки", "SETTINGS"));

        side.add(Box.createVerticalGlue());

        sideUserRankLbl = new JLabel("Гость (не авторизован)");
        sideUserRankLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sideUserRankLbl.setForeground(new Color(148, 163, 184));
        sideUserRankLbl.setBorder(new EmptyBorder(0, 10, 6, 0));
        sideUserRankLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        sideAuthBtn = createNavBtn("↪  Войти", "LOGIN");
        side.add(sideUserRankLbl);
        side.add(sideAuthBtn);
        return side;
    }

    private JButton createNavBtn(String text, String tabId) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (tabId.equals(activeTab)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, new Color(126, 34, 206, 215), getWidth(), 0, new Color(88, 28, 135, 165));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(new Color(192, 132, 252, 145));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        b.putClientProperty("tabId", tabId);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(new Color(203, 213, 225));
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setMaximumSize(new Dimension(170, 40));
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> switchTab(tabId));
        navButtons.add(b);
        return b;
    }

    private void switchTab(String tabId) {
        activeTab = tabId;
        for (JButton b : navButtons) {
            String id = (String) b.getClientProperty("tabId");
            b.setForeground(tabId.equals(id) ? Color.WHITE : new Color(180, 185, 205));
            b.repaint();
        }
        contentLayout.show(contentPanel, tabId);
        if ("CONFIGS".equals(tabId)) {
            updateVipCreateVisibility();
            new Thread(this::fetchConfigsFromSite).start();
        }
    }

    private JPanel buildPlayTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(14, 34, 26, 34));

        JPanel box = createRoundedBox(22);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(24, 28, 24, 28));
        box.setMaximumSize(new Dimension(510, 380));

        JLabel h = new JLabel("DragonVisuals Client");
        h.setFont(new Font("SansSerif", Font.BOLD, 25));
        h.setForeground(Color.WHITE);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel d = new JLabel("<html><div style='width:390px;text-align:center;color:#cbd5e1;'>Фирменный визуальный клиент: ClickGui, TargetESP, JumpCircles, Trails, ChinaHat, 3D Katana и PurpleGlow.</div></html>");
        d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        d.setAlignmentX(Component.CENTER_ALIGNMENT);

        clientSyncLbl = new JLabel("• Профиль: Гость  |  Обновление: Авто");
        clientSyncLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        clientSyncLbl.setForeground(new Color(192, 132, 252));
        clientSyncLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nl = new JLabel("Игровой никнейм:");
        nl.setForeground(new Color(203, 213, 225));
        nl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nl.setAlignmentX(Component.CENTER_ALIGNMENT);

        mcNickField = createStyledInput();
        mcNickField.setText("Bratellaly");
        mcNickField.setMaximumSize(new Dimension(320, 38));

        mainStatus = new JLabel("Готов к запуску");
        mainStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mainStatus.setForeground(new Color(167, 139, 250));
        mainStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        playBtn = createPurpleBtn("ЗАПУСТИТЬ DRAGONVISUALS");
        playBtn.setMaximumSize(new Dimension(320, 46));
        playBtn.addActionListener(e -> new Thread(this::installAndLaunchGame).start());

        box.add(h);
        box.add(Box.createVerticalStrut(8));
        box.add(d);
        box.add(Box.createVerticalStrut(14));
        box.add(clientSyncLbl);
        box.add(Box.createVerticalStrut(20));
        box.add(nl);
        box.add(Box.createVerticalStrut(6));
        box.add(mcNickField);
        box.add(Box.createVerticalGlue());
        box.add(mainStatus);
        box.add(Box.createVerticalStrut(10));
        box.add(playBtn);

        p.add(Box.createVerticalGlue());
        p.add(box);
        p.add(Box.createVerticalGlue());
        return p;
    }
    private JPanel buildConfigsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 26, 20, 26));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel title = new JLabel("Облачные конфиги");
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        title.setForeground(Color.WHITE);

        JButton refreshBtn = createPurpleBtn("↻ Обновить");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        refreshBtn.addActionListener(e -> new Thread(this::fetchConfigsFromSite).start());

        topRow.add(title, BorderLayout.WEST);
        topRow.add(refreshBtn, BorderLayout.EAST);

        vipCreateBox = createRoundedBox(16);
        vipCreateBox.setLayout(new BorderLayout(10, 0));
        vipCreateBox.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel vipText = new JLabel("<html><span style='color:#e9d5ff;font-size:12px;'><b>VIP:</b> Отправить текущие настройки визуалов на сайт</span></html>");
        JButton createCfgBtn = createPurpleBtn("+ Создать конфиг");
        createCfgBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        createCfgBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        createCfgBtn.addActionListener(e -> promptAndUploadConfig());
        vipCreateBox.add(vipText, BorderLayout.CENTER);
        vipCreateBox.add(createCfgBtn, BorderLayout.EAST);
        vipCreateBox.setVisible(false);

        JPanel northWrap = new JPanel();
        northWrap.setOpaque(false);
        northWrap.setLayout(new BoxLayout(northWrap, BoxLayout.Y_AXIS));
        northWrap.add(topRow);
        northWrap.add(Box.createVerticalStrut(10));
        northWrap.add(vipCreateBox);

        configsListPanel = new JPanel();
        configsListPanel.setOpaque(false);
        configsListPanel.setLayout(new BoxLayout(configsListPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(configsListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        cfgStatusLbl = new JLabel("Нажмите «Обновить» для синхронизации с сайтом.");
        cfgStatusLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cfgStatusLbl.setForeground(new Color(167, 139, 250));

        p.add(northWrap, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(cfgStatusLbl, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildSettingsTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 34, 28, 34));

        JPanel box = createRoundedBox(20);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(22, 24, 22, 24));
        box.setMaximumSize(new Dimension(510, 260));

        JLabel h = new JLabel("Настройки клиента");
        h.setFont(new Font("SansSerif", Font.BOLD, 20));
        h.setForeground(Color.WHITE);

        JLabel l = new JLabel("Путь к директории игры (.minecraft):");
        l.setForeground(new Color(203, 213, 225));
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));

        mcPathField = createStyledInput();
        mcPathField.setText(Paths.get(System.getenv("APPDATA"), ".minecraft").toString());
        mcPathField.setMaximumSize(new Dimension(460, 38));

        JLabel verInfo = new JLabel("DragonLauncher v2.3  •  dnastykpro-rgb.github.io");
        verInfo.setForeground(new Color(148, 163, 184));
        verInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        box.add(h);
        box.add(Box.createVerticalStrut(18));
        box.add(l);
        box.add(Box.createVerticalStrut(6));
        box.add(mcPathField);
        box.add(Box.createVerticalGlue());
        box.add(verInfo);
        p.add(box);
        return p;
    }

    private JPanel buildLoginTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 100, 30, 100));

        JLabel title = new JLabel("Авторизация");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Используйте аккаунт с сайта DragonVisuals");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 165, 210));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginField = createStyledInput();
        passField = new JPasswordField();
        styleInput(passField);

        loginBtn = createPurpleBtn("Войти в аккаунт");
        loginBtn.addActionListener(e -> new Thread(this::doLogin).start());

        loginStatus = new JLabel(" ");
        loginStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginStatus.setForeground(new Color(248, 113, 113));
        loginStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(6));
        p.add(sub);
        p.add(Box.createVerticalStrut(22));
        p.add(loginField);
        p.add(Box.createVerticalStrut(12));
        p.add(passField);
        p.add(Box.createVerticalStrut(20));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(10));
        p.add(loginStatus);
        return p;
    }
    private void updateVipCreateVisibility() {
        boolean isVipPlus = currentRank.equalsIgnoreCase("VIP")
                || currentRank.equalsIgnoreCase("Moder")
                || currentRank.equalsIgnoreCase("Owner");
        vipCreateBox.setVisible(isVipPlus);
    }

    private void fetchConfigsFromSite() {
        SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("Загрузка конфигов с сайта..."));
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BIN_URL)).GET().build();
            String json = client.send(req, HttpResponse.BodyHandlers.ofString()).body().trim();
            JsonObject db = json.startsWith("{") ? JsonParser.parseString(json).getAsJsonObject() : new JsonObject();
            JsonArray arr = db.has("configs") && db.get("configs").isJsonArray() ? db.getAsJsonArray("configs") : new JsonArray();

            SwingUtilities.invokeLater(() -> {
                configsListPanel.removeAll();
                if (arr.isEmpty()) {
                    cfgStatusLbl.setText("На сайте пока нет опубликованных конфигов.");
                } else {
                    for (JsonElement el : arr) {
                        JsonObject c = el.getAsJsonObject();
                        configsListPanel.add(createConfigRow(c));
                        configsListPanel.add(Box.createVerticalStrut(8));
                    }
                    cfgStatusLbl.setText("Доступно конфигов: " + arr.size());
                }
                configsListPanel.revalidate();
                configsListPanel.repaint();
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("Не удалось загрузить конфиги: " + ex.getMessage()));
        }
    }

    private JPanel createConfigRow(JsonObject c) {
        JPanel row = createRoundedBox(16);
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(520, 72));

        String name = c.has("name") ? c.get("name").getAsString() : "Config";
        String author = c.has("author") ? c.get("author").getAsString() : "VIP";
        String desc = c.has("desc") ? c.get("desc").getAsString() : "";
        String content = c.has("content") ? c.get("content").getAsString() : "{}";

        JLabel info = new JLabel("<html><b style='color:#ffffff;font-size:13px;'>" + name + "</b> <span style='color:#c084fc;'>• " + author + "</span><br><span style='color:#cbd5e1;font-size:11px;'>" + desc + "</span></html>");

        JButton addBtn = createPurpleBtn("Добавить");
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        addBtn.addActionListener(e -> installConfigLocally(name, content));

        row.add(info, BorderLayout.CENTER);
        row.add(addBtn, BorderLayout.EAST);
        return row;
    }

    private void installConfigLocally(String name, String content) {
        try {
            Path cfgDir = Paths.get(mcPathField.getText().trim(), "DragonVisuals", "configs");
            Files.createDirectories(cfgDir);
            String safeName = name.replaceAll("[^a-zA-Z0-9а-яА-Я_-]", "_") + ".json";
            Files.writeString(cfgDir.resolve(safeName), content, StandardCharsets.UTF_8);
            cfgStatusLbl.setText("✓ Конфиг «" + name + "» добавлен в DragonVisuals!");
        } catch (Exception ex) {
            cfgStatusLbl.setText("Ошибка сохранения: " + ex.getMessage());
        }
    }

           private void promptAndUploadConfig() {
        String cfgName = JOptionPane.showInputDialog(this, "Введите название нового конфига:", "Создать конфиг на сайте", JOptionPane.PLAIN_MESSAGE);
        if (cfgName == null || cfgName.trim().isEmpty()) return;
        String cfgDesc = JOptionPane.showInputDialog(this, "Краткое описание (необязательно):", "Описание конфига", JOptionPane.PLAIN_MESSAGE);

        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("Отправка текущего конфига на сайт..."));
                Path currentFile = Paths.get(mcPathField.getText().trim(), "DragonVisuals", "current.json");
                String content = Files.exists(currentFile) ? Files.readString(currentFile, StandardCharsets.UTF_8) : "{\"theme\":\"PurpleGlow\",\"ClickGui\":true}";

                // 1. Быстрый GET через HttpURLConnection с таймаутом 7 сек
                java.net.HttpURLConnection getConn = (java.net.HttpURLConnection) URI.create(BIN_URL).toURL().openConnection();
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(7000);
                getConn.setReadTimeout(7000);
                String rawDb = new String(getConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                getConn.disconnect();

                JsonObject db = rawDb.startsWith("{") ? JsonParser.parseString(rawDb).getAsJsonObject() : new JsonObject();
                db.remove("__configs");
                JsonArray arr = db.has("configs") && db.get("configs").isJsonArray() ? db.getAsJsonArray("configs") : new JsonArray();

                JsonObject newCfg = new JsonObject();
                newCfg.addProperty("id", System.currentTimeMillis());
                newCfg.addProperty("name", cfgName.trim());
                newCfg.addProperty("desc", (cfgDesc == null || cfgDesc.trim().isEmpty()) ? "Конфиг от " + currentUser : cfgDesc.trim());
                newCfg.addProperty("content", content);
                newCfg.addProperty("author", currentUser);
                newCfg.addProperty("rank", currentRank);
                newCfg.addProperty("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

                JsonArray updated = new JsonArray();
                updated.add(newCfg);
                for (JsonElement el : arr) updated.add(el);
                db.add("configs", updated);

                               // 2. Мгновенный POST с автоматическим переходом по 307/308 редиректу
                byte[] out = new Gson().toJson(db).getBytes(StandardCharsets.UTF_8);
                String targetUrl = BIN_URL;
                int code = 0;

                for (int i = 0; i < 3; i++) {
                    java.net.HttpURLConnection postConn = (java.net.HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
                    postConn.setInstanceFollowRedirects(false);
                    postConn.setRequestMethod("POST");
                    postConn.setDoOutput(true);
                    postConn.setConnectTimeout(7000);
                    postConn.setReadTimeout(7000);
                    postConn.setFixedLengthStreamingMode(out.length);
                    postConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    try (OutputStream os = postConn.getOutputStream()) {
                        os.write(out);
                    }

                    code = postConn.getResponseCode();
                    String loc = postConn.getHeaderField("Location");
                    postConn.disconnect();

                    if ((code == 307 || code == 308 || code == 301 || code == 302) && loc != null && !loc.isEmpty()) {
                        targetUrl = loc.startsWith("http") ? loc : URI.create(targetUrl).resolve(loc).toString();
                    } else {
                        break;
                    }
                }

                if (code >= 200 && code < 300) {
                    fetchConfigsFromSite();
                    SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("✓ Конфиг «" + cfgName.trim() + "» опубликован на сайте!"));
                } else {
                    int finalCode = code;
                    SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("Ошибка сервера: код " + finalCode));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> cfgStatusLbl.setText("Ошибка отправки: " + ex.getMessage()));
            }
        }).start();
    }

    private JPanel createRoundedBox(int radius) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 14, 38, 195));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.setColor(new Color(192, 132, 252, 55));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JTextField createStyledInput() {
        JTextField tf = new JTextField();
        styleInput(tf);
        return tf;
    }

    private void styleInput(JTextField tf) {
        tf.setMaximumSize(new Dimension(320, 38));
        tf.setBackground(new Color(14, 10, 28));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 51, 234, 150), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    private JButton createPurpleBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(147, 51, 234), getWidth(), 0, new Color(107, 33, 168));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void doLogin() {
        loginBtn.setEnabled(false);
        String u = loginField.getText().trim();
        String p = new String(passField.getPassword()).trim();
        SwingUtilities.invokeLater(() -> loginStatus.setText("Проверка данных..."));

        try {
            if (u.equalsIgnoreCase(OWNER_LOGIN) && p.equals(OWNER_PASS)) {
                currentUser = u; currentRank = "Owner";
            } else {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BIN_URL)).GET().build();
                String json = client.send(req, HttpResponse.BodyHandlers.ofString()).body().trim();
                JsonObject db = json.startsWith("{") ? JsonParser.parseString(json).getAsJsonObject() : new JsonObject();
                if (!db.has(u) || !db.getAsJsonObject(u).get("p").getAsString().equals(p)) {
                    SwingUtilities.invokeLater(() -> { loginStatus.setText("Неверный логин или пароль!"); loginBtn.setEnabled(true); });
                    return;
                }
                JsonObject obj = db.getAsJsonObject(u);
                if (obj.has("banned") && obj.get("banned").getAsBoolean()) {
                    SwingUtilities.invokeLater(() -> { loginStatus.setText("Аккаунт заблокирован!"); loginBtn.setEnabled(true); });
                    return;
                }
                currentUser = u; currentRank = obj.get("r").getAsString();
            }

            SwingUtilities.invokeLater(() -> {
                sideUserRankLbl.setText(currentUser + " [" + currentRank + "]");
                sideAuthBtn.setText("↪  Сменить аккаунт");
                clientSyncLbl.setText("• Профиль: " + currentUser + " [" + currentRank + "]  |  Обновление: Авто");
                loginBtn.setEnabled(true);
                updateVipCreateVisibility();
                switchTab("PLAY");
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> { loginStatus.setText("Ошибка сети!"); loginBtn.setEnabled(true); });
        }
    }

    private void installAndLaunchGame() {
        playBtn.setEnabled(false);
        try {
            SwingUtilities.invokeLater(() -> mainStatus.setText("Обновление DragonVisuals..."));
            Path mcDir = Paths.get(mcPathField.getText().trim());
            Path modsDir = mcDir.resolve("mods");
            Path configDir = mcDir.resolve("config");
            Files.createDirectories(modsDir);
            Files.createDirectories(configDir);

            if (!currentUser.isEmpty()) {
                Files.writeString(configDir.resolve("dragonvisuals_account.txt"), currentUser + ":" + currentRank);
            }

            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            byte[] zipBytes = client.send(HttpRequest.newBuilder().uri(URI.create(ZIP_URL)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                        Files.write(modsDir.resolve(Paths.get(entry.getName()).getFileName().toString()), zis.readAllBytes());
                    }
                }
            }

            SwingUtilities.invokeLater(() -> mainStatus.setText("Запуск Minecraft 1.21.4..."));
            launchInstalledMinecraft(mcDir, mcNickField.getText().trim());
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> { mainStatus.setText("Ошибка: " + ex.getMessage()); playBtn.setEnabled(true); });
        }
    }

    private void launchInstalledMinecraft(Path mcDir, String nick) throws Exception {
        Path tlauncher = mcDir.resolve("TLauncher.exe");
        Path legacy = Paths.get(System.getenv("APPDATA"), ".tlauncher", "legacy", "Minecraft.exe");
        if (Files.exists(tlauncher)) {
            new ProcessBuilder(tlauncher.toString()).start();
        } else if (Files.exists(legacy)) {
            new ProcessBuilder(legacy.toString()).start();
        } else {
            File versionsDir = mcDir.resolve("versions").toFile();
            File[] vers = versionsDir.listFiles((dir, name) -> name.contains("fabric") && name.contains("1.21.4"));
            if (vers != null && vers.length > 0) {
                String vName = java.util.Arrays.asList(vers).get(0).getName();
                SwingUtilities.invokeLater(() -> mainStatus.setText("Мод обновлён! Профиль " + vName + " готов."));
            } else {
                SwingUtilities.invokeLater(() -> mainStatus.setText("Мод установлен! Откройте ваш лаунчер Minecraft."));
            }
        }
        SwingUtilities.invokeLater(() -> playBtn.setEnabled(true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DragonLauncher().setVisible(true));
    }
}
