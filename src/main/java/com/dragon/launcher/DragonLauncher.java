package com.dragon.launcher;

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
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DragonLauncher extends JFrame {
    private static final String OWNER_LOGIN = "admin";
    private static final String OWNER_PASS  = "dragon2026";
    private static final String BIN_URL     = "https://npoint.io";
    private static final String ZIP_URL     = "https://github.io";

    private int mouseX, mouseY;
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardsPanel = new JPanel(cardLayout);

    private JTextField loginField;
    private JPasswordField passField;
    private JLabel loginStatus;
    private JButton loginBtn;

    private JLabel userBadgeLabel;
    private JTextField mcNickField;
    private JLabel mainStatus;
    private JButton playBtn;

    private String currentUser = "";
    private String currentRank = "";

    public DragonLauncher() {
        setUndecorated(true);
        setSize(680, 420);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        setShape(new RoundRectangle2D.Double(0, 0, 680, 420, 28, 28));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(13, 9, 26), getWidth(), getHeight(), new Color(36, 14, 64));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(new Color(168, 85, 247, 110));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 28, 28);
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

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(14, 22, 5, 18));

        JLabel brand = new JLabel("DRAGON VISUALS • 1.21.4");
        brand.setFont(new Font("SansSerif", Font.BOLD, 12));
        brand.setForeground(new Color(192, 132, 252));

        JPanel winBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        winBtns.setOpaque(false);
        JButton minBtn = createTopBtn("—", new Color(148, 163, 184));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        JButton closeBtn = createTopBtn("✕", new Color(248, 113, 113));
        closeBtn.addActionListener(e -> System.exit(0));
        winBtns.add(minBtn);
        winBtns.add(closeBtn);

        topBar.add(brand, BorderLayout.WEST);
        topBar.add(winBtns, BorderLayout.EAST);

        cardsPanel.setOpaque(false);
        cardsPanel.add(buildLoginScreen(), "LOGIN");
        cardsPanel.add(buildMainScreen(), "MAIN");

        root.add(topBar, BorderLayout.NORTH);
        root.add(cardsPanel, BorderLayout.CENTER);
        add(root);
    }

    private JButton createTopBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 8, 2, 8));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JPanel buildLoginScreen() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 140, 30, 140));

        JLabel title = new JLabel("Вход в систему");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Используйте аккаунт с сайта DragonVisuals");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 165, 210));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginField = createStyledInput();
        passField = new JPasswordField();
        styleInput(passField);

        loginBtn = createPurpleBtn("Авторизоваться");
        loginBtn.addActionListener(e -> new Thread(this::doLogin).start());

        loginStatus = new JLabel(" ");
        loginStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginStatus.setForeground(new Color(248, 113, 113));
        loginStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(6));
        p.add(sub);
        p.add(Box.createVerticalStrut(24));
        p.add(createFieldLabel("Логин:"));
        p.add(Box.createVerticalStrut(4));
        p.add(loginField);
        p.add(Box.createVerticalStrut(12));
        p.add(createFieldLabel("Пароль:"));
        p.add(Box.createVerticalStrut(4));
        p.add(passField);
        p.add(Box.createVerticalStrut(22));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(12));
        p.add(loginStatus);
        return p;
    }

    private JPanel buildMainScreen() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(15, 28, 28, 28));

        JPanel leftCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 14, 38, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(192, 132, 252, 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        leftCard.setOpaque(false);
        leftCard.setPreferredSize(new Dimension(220, 0));
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));
        leftCard.setBorder(new EmptyBorder(20, 18, 20, 18));

        JLabel profTitle = new JLabel("ПРОФИЛЬ");
        profTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        profTitle.setForeground(new Color(167, 139, 250));

        userBadgeLabel = new JLabel("admin [Owner]");
        userBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        userBadgeLabel.setForeground(Color.WHITE);

        mcNickField = createStyledInput();
        mcNickField.setText("Bratellaly");

        JButton logoutBtn = new JButton("Сменить аккаунт");
        logoutBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        logoutBtn.setForeground(new Color(203, 213, 225));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorder(BorderFactory.createLineBorder(new Color(168, 85, 247, 100), 1));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> cardLayout.show(cardsPanel, "LOGIN"));

        leftCard.add(profTitle);
        leftCard.add(Box.createVerticalStrut(6));
        leftCard.add(userBadgeLabel);
        leftCard.add(Box.createVerticalStrut(22));
        leftCard.add(createFieldLabel("Ник в Minecraft:"));
        leftCard.add(Box.createVerticalStrut(6));
        leftCard.add(mcNickField);
        leftCard.add(Box.createVerticalGlue());
        leftCard.add(logoutBtn);

        JPanel rightCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 14, 38, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(192, 132, 252, 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        rightCard.setOpaque(false);
        rightCard.setLayout(new BoxLayout(rightCard, BoxLayout.Y_AXIS));
        rightCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel verTitle = new JLabel("DragonVisuals 1.21.4");
        verTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        verTitle.setForeground(Color.WHITE);

        JLabel verDesc = new JLabel("<html><body style='width: 290px; color: #cbd5e1;'>Автоматическое обновление визуалов, синхронизация VIP-статуса и запуск клиента Fabric 1.21.4.</body></html>");
        verDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        playBtn = createPurpleBtn("ЗАПУСТИТЬ ИГРУ");
        playBtn.setMaximumSize(new Dimension(340, 46));
        playBtn.addActionListener(e -> new Thread(this::installAndLaunchGame).start());

        mainStatus = new JLabel("Готов к запуску");
        mainStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mainStatus.setForeground(new Color(167, 139, 250));

        rightCard.add(verTitle);
        rightCard.add(Box.createVerticalStrut(12));
        rightCard.add(verDesc);
        rightCard.add(Box.createVerticalGlue());
        rightCard.add(mainStatus);
        rightCard.add(Box.createVerticalStrut(12));
        rightCard.add(playBtn);

        p.add(leftCard, BorderLayout.WEST);
        p.add(rightCard, BorderLayout.CENTER);
        return p;
    }
    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(new Color(203, 213, 225));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField createStyledInput() {
        JTextField tf = new JTextField();
        styleInput(tf);
        return tf;
    }

    private void styleInput(JTextField tf) {
        tf.setMaximumSize(new Dimension(380, 36));
        tf.setBackground(new Color(12, 8, 24));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 51, 234), 1),
                new EmptyBorder(5, 10, 5, 10)
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
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
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
                HttpClient client = HttpClient.newHttpClient();
                String json = client.send(HttpRequest.newBuilder().uri(URI.create(BIN_URL)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
                JsonObject db = JsonParser.parseString(json).getAsJsonObject();
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

            if (!currentRank.equalsIgnoreCase("Owner") && !currentRank.equalsIgnoreCase("Moder") && !currentRank.equalsIgnoreCase("VIP")) {
                SwingUtilities.invokeLater(() -> { loginStatus.setText("Нет VIP статуса на сайте!"); loginBtn.setEnabled(true); });
                return;
            }

            SwingUtilities.invokeLater(() -> {
                userBadgeLabel.setText(currentUser + " [" + currentRank + "]");
                loginBtn.setEnabled(true);
                cardLayout.show(cardsPanel, "MAIN");
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> { loginStatus.setText("Ошибка сети!"); loginBtn.setEnabled(true); });
        }
    }

    private void installAndLaunchGame() {
        playBtn.setEnabled(false);
        try {
            SwingUtilities.invokeLater(() -> mainStatus.setText("Обновление DragonVisuals..."));
            Path mcDir = Paths.get(System.getenv("APPDATA"), ".minecraft");
            Path modsDir = mcDir.resolve("mods");
            Path configDir = mcDir.resolve("config");
            Files.createDirectories(modsDir);
            Files.createDirectories(configDir);

            Files.writeString(configDir.resolve("dragonvisuals_account.txt"), currentUser + ":" + currentRank);

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
                SwingUtilities.invokeLater(() -> mainStatus.setText("Мод обновлён! Профиль " + vers.getName() + " готов."));
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
