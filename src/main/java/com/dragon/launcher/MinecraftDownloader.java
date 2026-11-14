package com.dragon.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MinecraftDownloader {
    private static final String MC_VERSION = "1.21.4";
    private static final String MOJANG_MANIFEST = "https://mojang.com";
    private static final String FABRIC_META = "https://fabricmc.net" + MC_VERSION;
    private static final String MODRINTH_FAPI = "https://modrinth.com" + MC_VERSION + "%22%5D&loaders=%5B%22fabric%22%5D"; 

        private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static void downloadAndLaunch(Path mcDir, String nickname, int ramMb, Consumer<String> status) throws Exception {
        Path versionsDir = mcDir.resolve("versions").resolve(MC_VERSION);
        Path libsDir = mcDir.resolve("libraries");
        Path nativesDir = versionsDir.resolve("natives");
        Path assetsDir = mcDir.resolve("assets");
        Path modsDir = mcDir.resolve("mods");

        Files.createDirectories(versionsDir);
        Files.createDirectories(libsDir);
        Files.createDirectories(nativesDir);
        Files.createDirectories(assetsDir.resolve("indexes"));
        Files.createDirectories(assetsDir.resolve("objects"));
        Files.createDirectories(modsDir);

        // 1. Получаем манифест Mojang для 1.21.4
        status.accept("Получение данных Minecraft " + MC_VERSION + "...");
        JsonObject manifest = JsonParser.parseString(getString(MOJANG_MANIFEST)).getAsJsonObject();
        String versionUrl = null;
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            if (MC_VERSION.equals(v.get("id").getAsString())) {
                versionUrl = v.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) throw new RuntimeException("Версия " + MC_VERSION + " не найдена!");

        JsonObject verJson = JsonParser.parseString(getString(versionUrl)).getAsJsonObject();

        // 2. Скачиваем client.jar
        Path clientJar = versionsDir.resolve(MC_VERSION + ".jar");
        if (!Files.exists(clientJar)) {
            status.accept("Скачивание клиента Minecraft " + MC_VERSION + "...");
            String clientUrl = verJson.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
            downloadFile(clientUrl, clientJar);
        }

        // 3. Скачиваем библиотеки Mojang и распаковываем natives
        List<String> classpath = new ArrayList<>();
        JsonArray libs = verJson.getAsJsonArray("libraries");
        int count = 0;
        for (JsonElement el : libs) {
            JsonObject lib = el.getAsJsonObject();
            if (!isRuleAllowed(lib)) continue;

            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("artifact")) {
                JsonObject art = downloads.getAsJsonObject("artifact");
                String path = art.get("path").getAsString();
                String url = art.get("url").getAsString();
                Path target = libsDir.resolve(path);
                if (!Files.exists(target)) {
                    count++;
                    if (count % 5 == 0) status.accept("Загрузка библиотек игры (" + count + ")...");
                    downloadFile(url, target);
                }
                classpath.add(target.toAbsolutePath().toString());
                if (path.contains("natives-windows") && !path.contains("arm64") && !path.contains("x86")) {
                    extractNatives(target, nativesDir);
                }
            }
        }
        // 4. Скачиваем ассеты (звуки, языки, иконки)
        JsonObject assetIndexObj = verJson.getAsJsonObject("assetIndex");
        String assetId = assetIndexObj.get("id").getAsString();
        Path indexFile = assetsDir.resolve("indexes").resolve(assetId + ".json");
        if (!Files.exists(indexFile)) {
            status.accept("Загрузка индекса ресурсов...");
            downloadFile(assetIndexObj.get("url").getAsString(), indexFile);
        }
        JsonObject objects = JsonParser.parseString(Files.readString(indexFile, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("objects");
        int assetCount = 0;
        for (String key : objects.keySet()) {
            String hash = objects.getAsJsonObject(key).get("hash").getAsString();
            String sub = hash.substring(0, 2);
            Path objPath = assetsDir.resolve("objects").resolve(sub).resolve(hash);
            if (!Files.exists(objPath)) {
                assetCount++;
                if (assetCount % 40 == 0) status.accept("Скачивание ресурсов (" + assetCount + ")...");
                downloadFile("https://minecraft.net" + sub + "/" + hash, objPath);
            }
        }

        // 5. Скачиваем Fabric Loader для 1.21.4
        status.accept("Проверка Fabric Loader " + MC_VERSION + "...");
        JsonArray loaders = JsonParser.parseString(getString(FABRIC_META)).getAsJsonArray();
        String loaderVer = loaders.get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();
        JsonObject fabricProfile = JsonParser.parseString(getString(FABRIC_META + "/" + loaderVer + "/profile/json")).getAsJsonObject();
        String mainClass = fabricProfile.get("mainClass").getAsString();

        for (JsonElement el : fabricProfile.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String baseUrl = lib.has("url") ? lib.get("url").getAsString() : "https://fabricmc.net";
            if (!baseUrl.endsWith("/")) baseUrl += "/";
            String mavenPath = mavenToPath(name);
            Path target = libsDir.resolve(mavenPath);
            if (!Files.exists(target)) {
                status.accept("Загрузка Fabric: " + name.split(":"));
                downloadFile(baseUrl + mavenPath, target);
            }
            classpath.add(target.toAbsolutePath().toString());
        }

        classpath.add(clientJar.toAbsolutePath().toString());

        // 6. Скачиваем Fabric API (если ещё не установлен)
        boolean hasFapi = false;
        try (var stream = Files.list(modsDir)) {
            hasFapi = stream.anyMatch(p -> p.getFileName().toString().toLowerCase().contains("fabric-api"));
        }
        if (!hasFapi) {
            status.accept("Скачивание Fabric API для " + MC_VERSION + "...");
            JsonArray fapiVersions = JsonParser.parseString(getString(MODRINTH_FAPI)).getAsJsonArray();
            if (!fapiVersions.isEmpty()) {
                JsonArray files = fapiVersions.get(0).getAsJsonObject().getAsJsonArray("files");
                JsonObject primary = files.get(0).getAsJsonObject();
                String fapiUrl = primary.get("url").getAsString();
                String fapiName = primary.get("filename").getAsString();
                downloadFile(fapiUrl, modsDir.resolve(fapiName));
            }
        }
        // 7. Запускаем процесс Minecraft 1.21.4 напрямую
        status.accept("Запуск Minecraft " + MC_VERSION + "...");
        String javaExe = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
        if (!Files.exists(Path.of(javaExe))) {
            javaExe = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        }

        String nick = (nickname == null || nickname.trim().isEmpty()) ? "DragonPlayer" : nickname.trim();
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-Xmx" + ramMb + "M");
        cmd.add("-Xms1024M");
        cmd.add("-Djava.library.path=" + nativesDir.toAbsolutePath());
        cmd.add("-Dminecraft.launcher.brand=DragonLauncher");
        cmd.add("-Dminecraft.launcher.version=2.5");
        cmd.add("-cp");
        cmd.add(String.join(";", classpath));
        cmd.add(mainClass);
        cmd.add("--username");
        cmd.add(nick);
        cmd.add("--version");
        cmd.add("fabric-loader-" + loaderVer + "-" + MC_VERSION);
        cmd.add("--gameDir");
        cmd.add(mcDir.toAbsolutePath().toString());
        cmd.add("--assetsDir");
        cmd.add(assetsDir.toAbsolutePath().toString());
        cmd.add("--assetIndex");
        cmd.add(assetId);
        cmd.add("--uuid");
        cmd.add(uuid);
        cmd.add("--accessToken");
        cmd.add("0");
        cmd.add("--userType");
        cmd.add("legacy");

        new ProcessBuilder(cmd).directory(mcDir.toFile()).start();
        status.accept("✓ Игра запущена! (" + nick + ")");
    }

    private static boolean isRuleAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        boolean allowed = false;
        for (JsonElement rEl : lib.getAsJsonArray("rules")) {
            JsonObject rule = rEl.getAsJsonObject();
            String action = rule.get("action").getAsString();
            if (rule.has("os")) {
                String osName = rule.getAsJsonObject("os").get("name").getAsString();
                if ("windows".equalsIgnoreCase(osName)) {
                    allowed = "allow".equals(action);
                }
            } else {
                allowed = "allow".equals(action);
            }
        }
        return allowed;
    }

       private static String mavenToPath(String coord) {
        java.util.List<String> parts = java.util.Arrays.asList(coord.split(":"));
        String group = parts.get(0).replace('.', '/');
        String artifact = parts.get(1);
        String version = parts.get(2);
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    private static void extractNatives(Path jarPath, Path nativesDir) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dll")) {
                    String fileName = Path.of(entry.getName()).getFileName().toString();
                    Files.copy(zis, nativesDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ignored) {}
    }

    private static String getString(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "DragonLauncher/2.5 (dnastykpro-rgb.github.io)")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + res.statusCode() + " при запросе к " + url);
        }
        return res.body();
    }

    private static void downloadFile(String url, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "DragonLauncher/2.5 (dnastykpro-rgb.github.io)")
                .GET()
                .build();
        byte[] data = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
        Files.write(target, data);
    }
}

