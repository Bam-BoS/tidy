import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    static final String VERSION = "1.0";
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static Map<String, String> cfg = new LinkedHashMap<>();
    static PrintWriter logw;
    static Path logPath;

    static Path projectDir() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    static Path defaultConfigPath() {
        return projectDir().resolve("crashxl").resolve("conf.ext");
    }

    static String defaultX() {
        return projectDir().resolve("opt").toString();
    }

    static String defaultL() {
        return projectDir().resolve("crashxl.log").toString();
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0 && (args[0].equals("-c") || args[0].equals("--create-config"))) {
                createConfig(Arrays.copyOfRange(args, 1, args.length));
                return;
            }
            Path configPath = defaultConfigPath();
            if (args.length > 0 && !args[0].startsWith("-")) {
                configPath = Paths.get(args[0]);
            }
            if (!Files.exists(configPath)) {
                System.out.println("Brak pliku konfiguracyjnego, tworzenie interaktywne.");
                createConfigInteractive(configPath);
            }
            loadConfig(configPath);
            sanitizeConfig();
            initLog();
            run();
        } catch (Exception e) {
            System.err.println("Blad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void sanitizeConfig() {
        Path proj = projectDir();
        String x = cfg.get("X");
        if (x != null) {
            Path px = tp(x).toAbsolutePath().normalize();
            if (!px.startsWith(proj)) {
                cfg.put("X", defaultX());
            }
        }
        String l = cfg.get("L");
        if (l != null) {
            Path pl = tp(l).toAbsolutePath().normalize();
            if (!pl.startsWith(proj)) {
                cfg.put("L", defaultL());
            }
        }
    }

    static void run() throws IOException {
        log("START");
        String sName = cfg.getOrDefault("S", "scripts.lst");
        String r = cfg.get("R");
        String a = cfg.get("A");

        Path listPath = tryDownload(r, sName, "R");
        if (listPath == null && a != null && !a.isEmpty()) {
            log("R niedostepne, proba A");
            listPath = tryDownload(a, sName, "A");
        }
        if (listPath == null) {
            log("BLAD: nie udalo sie pobrac listy plikow");
            log("END");
            return;
        }

        Path h = tp(cfg.getOrDefault("H", "~/Downloads/"));
        Files.createDirectories(h);

        List<String> names = new ArrayList<>();
        for (String line : Files.readAllLines(listPath, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int i = line.indexOf('=');
            String n = i >= 0 ? line.substring(i + 1).trim() : line;
            if (!n.isEmpty()) names.add(n);
        }
        Files.deleteIfExists(listPath);

        for (String n : names) {
            Path dest = h.resolve(n);
            boolean ok = false;
            if (r != null && !r.isEmpty()) {
                ok = download(r, n, dest);
            }
            if (!ok && a != null && !a.isEmpty()) {
                log("Fallback A dla " + n);
                ok = download(a, n, dest);
            }
            if (ok) log("POBRANO " + n);
            else log("NIE POBRANO " + n);
        }

        checkSelfCopy();
        log("END");
    }

    static void checkSelfCopy() throws IOException {
        Path x = tp(cfg.getOrDefault("X", defaultX()));
        if (!Files.exists(x)) {
            Files.createDirectories(x);
            log("UTWORZONO katalog " + x);
        }
        String fileName = programFileName();
        Path target = x.resolve(fileName);
        Path verFile = x.resolve("version.txt");
        String existingVer = null;
        if (Files.exists(verFile)) {
            existingVer = new String(Files.readAllBytes(verFile), StandardCharsets.UTF_8).trim();
        }
        if (!Files.exists(target)) {
            copySelf(target);
            Files.write(verFile, VERSION.getBytes(StandardCharsets.UTF_8));
            log("SKOPIOWANO program do " + target + " wersja " + VERSION);
        } else if (!VERSION.equals(existingVer)) {
            copySelf(target);
            Files.write(verFile, VERSION.getBytes(StandardCharsets.UTF_8));
            log("ZAKTUALIZOWANO program do wersji " + VERSION + " w " + target);
        } else {
            log("Program aktualny w " + target + " wersja " + VERSION);
        }
    }

    static void copySelf(Path target) {
        try {
            Path src = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(src)) {
                Path cls = src.resolve("Main.class");
                if (Files.exists(cls)) {
                    Files.copy(cls, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
                Files.write(target, ("CRASHXL " + VERSION).getBytes(StandardCharsets.UTF_8));
                return;
            }
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            try {
                Files.write(target, ("CRASHXL " + VERSION).getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                log("BLAD kopiowania programu: " + ex.getMessage());
            }
        }
    }

    static String programFileName() {
        try {
            Path src = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(src)) return src.getFileName().toString();
            return "Main.class";
        } catch (Exception e) {
            return "CRASHXL.class";
        }
    }

    static Path tryDownload(String base, String name, String tag) {
        if (base == null || base.isEmpty()) return null;
        String url = base.endsWith("/") ? base + name : base + "/" + name;
        try {
            URL u = new URL(url);
            URLConnection c = u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(10000);
            Path tmp = Files.createTempFile("crashxl_", ".lst");
            try (InputStream in = c.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            log("Pobrano liste z " + tag + ": " + url);
            return tmp;
        } catch (Exception e) {
            log("BLAD pobrania listy z " + tag + " (" + url + "): " + e.getMessage());
            return null;
        }
    }

    static boolean download(String base, String name, Path dest) {
        String url = base.endsWith("/") ? base + name : base + "/" + name;
        try {
            URL u = new URL(url);
            URLConnection c = u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(10000);
            try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(dest)) {
                in.transferTo(out);
            }
            return true;
        } catch (Exception e) {
            log("BLAD pobrania " + url + ": " + e.getMessage());
            return false;
        }
    }

    static void loadConfig(Path p) throws IOException {
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int i = line.indexOf('=');
            if (i < 0) continue;
            String k = line.substring(0, i).trim().toUpperCase();
            String v = line.substring(i + 1).trim();
            cfg.put(k, v);
        }
    }

    static void saveConfig(Path p, Map<String, String> m) throws IOException {
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        }
        Files.write(p, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static void createConfig(String[] params) throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        for (String p : params) {
            int i = p.indexOf('=');
            if (i > 0) {
                m.put(p.substring(0, i).trim().toUpperCase(), p.substring(i + 1).trim());
            }
        }
        if (m.isEmpty()) {
            createConfigInteractive(defaultConfigPath());
            return;
        }
        Path configPath = m.containsKey("C")
                ? tp(m.get("C"))
                : defaultConfigPath();
        if (!m.containsKey("C")) m.put("C", configPath.toString());
        String[][] defs = {
                {"R", "https://edu.gplweb.pl/sh/"},
                {"A", "http://" + localIp() + "/sh/"},
                {"S", "scripts.lst"},
                {"H", "~/Downloads/"},
                {"X", defaultX()},
                {"L", defaultL()}
        };
        for (String[] d : defs) {
            if (!m.containsKey(d[0])) m.put(d[0], d[1]);
        }
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String k : new String[]{"C", "R", "A", "S", "H", "X", "L"}) {
            if (m.containsKey(k)) ordered.put(k, m.get(k));
        }
        saveConfig(configPath, ordered);
        System.out.println("Zapisano: " + configPath.toAbsolutePath());
    }

    static void createConfigInteractive(Path configPath) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("Nowy plik konfiguracyjny: " + configPath.toAbsolutePath());
        String r = prompt(br, "R", "https://edu.gplweb.pl/sh/");
        String a = prompt(br, "A", "http://" + localIp() + "/sh/");
        String s = prompt(br, "S", "scripts.lst");
        String h = prompt(br, "H", "~/Downloads/");
        String x = prompt(br, "X", defaultX());
        String l = prompt(br, "L", defaultL());
        Map<String, String> m = new LinkedHashMap<>();
        m.put("C", configPath.toString());
        m.put("R", r);
        m.put("A", a);
        m.put("S", s);
        m.put("H", h);
        m.put("X", x);
        m.put("L", l);
        saveConfig(configPath, m);
        System.out.println("Zapisano: " + configPath.toAbsolutePath());
    }

    static String prompt(BufferedReader br, String key, String def) throws IOException {
        System.out.print(key + " [" + def + "]: ");
        String s = br.readLine();
        if (s == null || s.trim().isEmpty()) return def;
        return s.trim();
    }

    static String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    static Path tp(String p) {
        if (p == null) return null;
        if (p.startsWith("~")) {
            p = System.getProperty("user.home") + p.substring(1);
        }
        return Paths.get(p);
    }

    static void initLog() throws IOException {
        String lp = cfg.getOrDefault("L", defaultL());
        logPath = tp(lp);
        if (Files.isDirectory(logPath)) {
            logPath = logPath.resolve("crashxl.log");
        }
        if (logPath.getParent() != null) Files.createDirectories(logPath.getParent());
        logw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(logPath.toFile(), true), StandardCharsets.UTF_8), true);
    }

    static void log(String msg) {
        String line = LocalDateTime.now().format(FMT) + " " + msg;
        System.out.println(line);
        if (logw != null) logw.println(line);
    }
}

//Tobiasz Burzynski 5TP