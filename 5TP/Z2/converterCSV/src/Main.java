import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {
        // Szukanie pliku wejsciowego automatycznie
        String inputPath = findInputFile();
        if (inputPath == null) {
            System.out.println("Nie znaleziono pliku wejsciowego. Umiesc plik CSV z raportem w katalogu projektu.");
            System.out.println("Katalog roboczy: " + System.getProperty("user.dir"));
            return;
        }
        System.out.println("Znaleziono plik: " + new File(inputPath).getAbsolutePath());

        // Domyslna klasa i nazwa pliku wyjsciowego
        String className = "5TP";
        String outputPath = className + "_Zadanie-INF04.csv";

        // Uruchomienie konwertera z progiem 60 minut
        ReportConverter converter = new TeamsAttendanceConverter(inputPath, outputPath, 60);
        converter.convert();
        System.out.println("Zapisano: " + new File(outputPath).getAbsolutePath());
    }

    // Skanowanie katalogow
    private static String findInputFile() {
        String fileName = "Zadanie programistyczne pod INF.04 - Raport obecnosci 17-09-26 (5TP).csv";
        String[] candidates = {
                fileName,
                "./" + fileName,
                "../" + fileName,
                "../../" + fileName,
                "../../../" + fileName,
                System.getProperty("user.dir") + File.separator + fileName,
                System.getProperty("user.dir") + File.separator + ".." + File.separator + fileName
        };
        for (String c : candidates) {
            if (Files.exists(Paths.get(c))) return c;
        }

        // Skanowanie do 4 poziomow w gore
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++) {
            File found = scanDirForReport(dir.toFile());
            if (found != null) return found.getAbsolutePath();
            dir = dir.getParent();
        }
        return null;
    }

    // Szukanie pliku .csv z "raport" w nazwie
    private static File scanDirForReport(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".csv") && n.contains("raport")) {
                    return f;
                }
            }
        }
        return null;
    }
}

abstract class ReportConverter {
    protected final String inputPath;
    protected final String outputPath;
    protected List<Participant> participants;

    public ReportConverter(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    // Metoda szablonowa - kolejnosc krokow algorytmu
    public final void convert() throws IOException {
        List<String> lines = readFile();
        System.out.println("Wczytano linii: " + lines.size());
        participants = parseParticipants(lines);
        System.out.println("Znaleziono uczestnikow: " + participants.size());
        transform();
        writeFile();
    }

    protected abstract List<String> readFile() throws IOException;
    protected abstract List<Participant> parseParticipants(List<String> lines);
    protected abstract void transform();
    protected abstract void writeFile() throws IOException;
}

class Participant {
    String surname;
    String firstName;
    String duration;
    int minutes;
    boolean certificate;
}

class TeamsAttendanceConverter extends ReportConverter {
    private final int minMinutes;

    public TeamsAttendanceConverter(String inputPath, String outputPath, int minMinutes) {
        super(inputPath, outputPath);
        this.minMinutes = minMinutes;
    }

    protected List<String> readFile() throws IOException {
        // Wczytanie bajtow do wykrycia kodowania
        byte[] bytes = Files.readAllBytes(Paths.get(inputPath));
        System.out.println("Rozmiar pliku w bajtach: " + bytes.length);

        String content = decode(bytes);
        List<String> lines = new ArrayList<>();
        // Normalizacja koncow linii (\r\n oraz \n)
        for (String line : content.split("\\r?\\n")) {
            lines.add(line);
        }
        return lines;
    }

    // Wykrywanie kodowania po BOM i bajtach zerowych
    private String decode(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            System.out.println("Wykryto UTF-16 LE z BOM");
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            System.out.println("Wykryto UTF-16 BE z BOM");
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            System.out.println("Wykryto UTF-8 z BOM");
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        // Heurystyka UTF-16 bez BOM
        int nullsEven = 0;
        int nullsOdd = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                if (i % 2 == 0) nullsEven++;
                else nullsOdd++;
            }
        }
        int total = Math.max(1, bytes.length / 2);
        if (nullsOdd > total * 0.3 && nullsEven < total * 0.1) {
            System.out.println("Wykryto UTF-16 LE bez BOM");
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
        if (nullsEven > total * 0.3 && nullsOdd < total * 0.1) {
            System.out.println("Wykryto UTF-16 BE bez BOM");
            return new String(bytes, StandardCharsets.UTF_16BE);
        }

        // Fallback UTF-8, potem Windows-1250
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (utf8.indexOf('\uFFFD') < 0) {
            System.out.println("Wykryto UTF-8 bez BOM");
            return utf8;
        }

        System.out.println("Wykryto Windows-1250");
        return new String(bytes, Charset.forName("Windows-1250"));
    }

    protected List<Participant> parseParticipants(List<String> lines) {
        List<Participant> result = new ArrayList<>();

        // Wyznaczenie granic sekcji "2. Uczestnicy"
        int start = -1;
        int end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String norm = strip(lines.get(i));
            if (start < 0 && norm.contains("uczestnic")) {
                start = i + 1;
                continue;
            }
            if (start >= 0 && (norm.startsWith("3.") || norm.contains("dzialania podczas"))) {
                end = i;
                break;
            }
        }

        if (start < 0) {
            System.out.println("Nie znaleziono sekcji 2. Uczestnicy");
            return result;
        }
        System.out.println("Sekcja 2 od linii " + start + " do " + end);

        // Wykrycie separatora kolumn (TAB lub ;)
        String separator = null;
        for (int i = start; i < end; i++) {
            if (lines.get(i).contains("\t")) { separator = "\t"; break; }
            if (lines.get(i).contains(";")) { separator = ";"; break; }
        }
        System.out.println("Separator: " + (separator == null ? "brak" : (separator.equals("\t") ? "TAB" : separator)));

        boolean headerFound = false;
        for (int i = start; i < end; i++) {
            String line = lines.get(i).replace("\uFEFF", "");
            if (line.trim().isEmpty()) continue;

            String[] columns = splitColumns(line, separator);
            if (columns.length < 4) continue;

            String rawName = columns[0].trim();
            String duration = columns[3].trim();

            // Pomijanie pierwszego wiersza naglowkowego
            if (!headerFound) {
                String norm = strip(line);
                if (norm.contains("nazwisko") || norm.contains("pierwsze") || norm.contains("ostatnie") || norm.contains("czas udzialu")) {
                    headerFound = true;
                    continue;
                }
                headerFound = true;
            }

            String normName = strip(rawName);
            String normDuration = strip(duration);

            // Filtrowanie zmiksowanych wierszy (powtorzony naglowek w srodku sekcji)
            if (normName.contains("nazwisko") || normName.contains("imie") || normName.contains("czas") || normName.contains("udzial")) {
                System.out.println("Pomijam uszkodzony wiersz: " + line);
                continue;
            }
            if (normDuration.contains("czas") || normDuration.contains("udzial") || normDuration.contains("status")) {
                System.out.println("Pomijam uszkodzony wiersz: " + line);
                continue;
            }

            if (rawName.isEmpty() || duration.isEmpty()) continue;

            String name = cleanName(rawName);
            String[] nameParts = splitName(name);

            Participant p = new Participant();
            p.firstName = nameParts[0];
            p.surname = nameParts[1];
            p.duration = duration;
            p.minutes = parseDurationToMinutes(duration);
            p.certificate = p.minutes >= minMinutes;
            result.add(p);
        }
        return result;
    }

    // Sortowanie po nazwisku, potem po imieniu
    protected void transform() {
        Collections.sort(participants, new Comparator<Participant>() {
            public int compare(Participant a, Participant b) {
                int bySurname = a.surname.compareToIgnoreCase(b.surname);
                if (bySurname != 0) return bySurname;
                return a.firstName.compareToIgnoreCase(b.firstName);
            }
        });
    }

    protected void writeFile() throws IOException {
        List<String> outputLines = new ArrayList<>();
        // Kolejnosc kolumn: Imie, Nazwisko, Czas, Status
        outputLines.add("Imie,Nazwisko,Czas uczestnictwa,Status zaswiadczenia");
        for (Participant p : participants) {
            outputLines.add(csv(p.firstName) + "," + csv(p.surname) + "," + csv(p.duration) + "," + (p.certificate ? "TAK" : "NIE"));
        }
        Files.write(Paths.get(outputPath), outputLines, StandardCharsets.UTF_8);
    }

    // Normalizacja tekstu (bez polskich znakow, male litery)
    private String strip(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    // Podzial wiersza na kolumny
    private String[] splitColumns(String line, String separator) {
        if (separator != null && line.contains(separator)) {
            return line.split(Pattern.quote(separator), -1);
        }
        if (line.contains("\t")) return line.split("\t", -1);
        if (line.contains(";")) return line.split(";", -1);
        // Fallback - podzial po 2+ spacjach
        return line.split("\\s{2,}", -1);
    }

    // Usuwanie dopiskow typu "(Niezweryfikowany)" i nadmiarowych spacji
    private String cleanName(String value) {
        int index = value.indexOf('(');
        if (index >= 0) value = value.substring(0, index);
        return value.trim().replaceAll("\\s+", " ");
    }

    // Podzial nazwy na [imie, nazwisko] z korekta kolejnosci i brakiem imienia = "-"
    private String[] splitName(String name) {
        if (name.isEmpty()) return new String[]{"-", "-"};
        String[] tokens = name.split(" ");
        if (tokens.length == 1) return new String[]{"-", tokens[0]};

        String first = tokens[0];
        String last = tokens[tokens.length - 1];

        // Nazwy 3+ czlonowe - ostatnie slowo to nazwisko
        if (tokens.length > 2) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < tokens.length - 1; i++) {
                if (builder.length() > 0) builder.append(" ");
                builder.append(tokens[i]);
            }
            first = builder.toString();
            last = tokens[tokens.length - 1];
        }

        // Korekta odwroconej kolejnosci
        if (looksLikeSurname(first) && !looksLikeSurname(last)) {
            return new String[]{last, first};
        }
        if (isKnownFirstName(last) && !isKnownFirstName(first)) {
            return new String[]{last, first};
        }
        return new String[]{first, last};
    }

    // Heurystyka - czy slowo wyglada jak nazwisko
    private boolean looksLikeSurname(String value) {
        String text = strip(value);
        String[] endings = {"ski", "cki", "dzki", "wicz", "icz", "czyk", "uk", "ak", "ek", "ow", "ewski", "owski", "inski", "ynski"};
        for (String ending : endings) {
            if (text.endsWith(ending)) return true;
        }
        return false;
    }

    // Lista znanych imion do korekty kolejnosci
    private boolean isKnownFirstName(String value) {
        String text = strip(value);
        Set<String> names = new HashSet<>(Arrays.asList(
                "jan", "jakub", "dawid", "norbert", "wiktor", "bartosz", "kacper", "alan",
                "mateusz", "oscar", "michal", "diego", "maciej", "piotr", "julian", "mach"));
        return names.contains(text);
    }

    // Zamiana "2 godz. 8 min 15 s" na liczbe minut (sekundy >= 30 w gore)
    private int parseDurationToMinutes(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        int totalMinutes = 0;
        Matcher hoursMatcher = Pattern.compile("(\\d+)\\s*godz").matcher(duration);
        if (hoursMatcher.find()) totalMinutes += Integer.parseInt(hoursMatcher.group(1)) * 60;
        Matcher minutesMatcher = Pattern.compile("(\\d+)\\s*min").matcher(duration);
        if (minutesMatcher.find()) totalMinutes += Integer.parseInt(minutesMatcher.group(1));
        Matcher secondsMatcher = Pattern.compile("(\\d+)\\s*s").matcher(duration);
        if (secondsMatcher.find()) {
            int seconds = Integer.parseInt(secondsMatcher.group(1));
            if (seconds >= 30) totalMinutes += 1;
        }
        return totalMinutes;
    }

    // Bezpieczne kodowanie wartosci do CSV
    private String csv(String value) {
        if (value == null) value = "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

//Tobiasz Burzynski 5TP
