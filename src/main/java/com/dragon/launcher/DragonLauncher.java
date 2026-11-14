package com.dragon.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DragonLauncher extends JFrame {
    // Настройки вашего проекта
    private static final String OWNER_LOGIN = "admin";
    private static final String OWNER_PASS  = "dragon2026";
    private static final String BIN_URL     = "https://api.npoint.io/e73eff1574594e20adab";
    private static final String ZIP_URL     = "https://github.io";

    private JTextField loginField;
    private JPasswordField passField;
    private JLabel statusLabel;
    private JButton actionBtn;
    private String currentRank = null;
    private String currentUser = null;

    public DragonLauncher() {
        setTitle("DragonLauncher — 1.21.4");
        setSize(420, 340);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(18, 10, 38), getWidth(), getHeight(), new Color(45, 17, 77));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(25, 35, 25, 35));

        JLabel title = new JLabel("DRAGON VISUALS");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(216, 180, 254));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Официальный лаунчер • Fabric 1.21.4");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 165, 210));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginField = createInput("Логин с сайта");
        passField = new JPasswordField();
        styleField(passField);

        actionBtn = new JButton("Войти и установить мод");
        actionBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        actionBtn.setBackground(new Color(147, 51, 234));
        actionBtn.setForeground(Color.WHITE);
        actionBtn.setFocusPainted(false);
        actionBtn.setBorder(new EmptyBorder(12, 20, 12, 20));
        actionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionBtn.addActionListener(e -> new Thread(this::processLoginAndInstall).start());

        statusLabel = new JLabel("Введите данные от аккаунта с сайта");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(203, 213, 225));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(title);
        root.add(Box.createVerticalStrut(5));
        root.add(sub);
        root.add(Box.createVerticalStrut(22));
        root.add(loginField);
        root.add(Box.createVerticalStrut(12));
        root.add(passField);
        root.add(Box.createVerticalStrut(20));
        root.add(actionBtn);
        root.add(Box.createVerticalStrut(16));
        root.add(statusLabel);

        add(root);
    }

    private JTextField createInput(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        styleField(tf);
        return tf;
    }

    private void styleField(JTextField tf) {
        tf.setMaximumSize(new Dimension(340, 38));
        tf.setBackground(new Color(15, 10, 28));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(168, 85, 247), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    private void processLoginAndInstall() {
        actionBtn.setEnabled(false);
        String user = loginField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        setStatus("Проверка аккаунта...", Color.LIGHT_GRAY);

        try {
            if (user.equalsIgnoreCase(OWNER_LOGIN) && pass.equals(OWNER_PASS)) {
                currentUser = user;
                currentRank = "Owner";
            } else {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BIN_URL)).GET().build();
                String json = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                JsonObject db = JsonParser.parseString(json).getAsJsonObject();

                if (!db.has(user)) {
                    setStatus("Ошибка: аккаунт не найден на сайте!", Color.PINK);
                    actionBtn.setEnabled(true);
                    return;
                }
                JsonObject uObj = db.getAsJsonObject(user);
                if (!uObj.get("p").getAsString().equals(pass)) {
                    setStatus("Ошибка: неверный пароль!", Color.PINK);
                    actionBtn.setEnabled(true);
                    return;
                }
                if (uObj.has("banned") && uObj.get("banned").getAsBoolean()) {
                    setStatus("Ваш аккаунт заблокирован!", Color.RED);
                    actionBtn.setEnabled(true);
                    return;
                }
                currentUser = user;
                currentRank = uObj.get("r").getAsString();
            }

            if (!currentRank.equalsIgnoreCase("Owner") && !currentRank.equalsIgnoreCase("Moder") && !currentRank.equalsIgnoreCase("VIP")) {
                setStatus("У вас нет VIP доступа! Получите его на сайте.", Color.ORANGE);
                actionBtn.setEnabled(true);
                return;
            }

            setStatus("Вход выполнен (" + currentRank + "). Скачивание мода...", new Color(192, 132, 252));
            installModToMinecraft();

        } catch (Exception ex) {
            setStatus("Ошибка сети: " + ex.getMessage(), Color.PINK);
            actionBtn.setEnabled(true);
        }
    }

    private void installModToMinecraft() throws Exception {
        String appData = System.getenv("APPDATA");
        Path mcDir = Paths.get(appData, ".minecraft");
        Path modsDir = mcDir.resolve("mods");
        Path configDir = mcDir.resolve("config");

        Files.createDirectories(modsDir);
        Files.createDirectories(configDir);

        // 1. Записываем профиль для мода
        Files.writeString(configDir.resolve("dragonvisuals_account.txt"), currentUser + ":" + currentRank);

        // 2. Скачиваем и распаковываем архив с модом в папку mods
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(ZIP_URL)).GET().build();
        byte[] zipBytes = client.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                    Path outFile = modsDir.resolve(Paths.get(entry.getName()).getFileName().toString());
                    Files.write(outFile, zis.readAllBytes());
                }
            }
        }

        setStatus("Готово! Мод установлен в .minecraft/mods. Запускайте игру!", new Color(74, 222, 128));
        actionBtn.setText("Обновить мод ещё раз");
        actionBtn.setEnabled(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DragonLauncher().setVisible(true));
    }
}
