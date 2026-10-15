import java.io.*;
import java.security.SecureRandom;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CodeHistoryStore store = CodeHistoryStore.getInstance();
        CodeGenerator generator = new CodeGenerator();
        boolean running = true;
        while (running) {
            System.out.println("1 Wygeneruj kod");
            System.out.println("2 Zapisz historię");
            System.out.println("3 Wczytaj historię");
            System.out.println("0 Wyjście");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        System.out.print("Podaj długość (4,6,12,16): ");
                        int length = Integer.parseInt(scanner.nextLine().trim());
                        String code = generator.generate(length);
                        System.out.println("Kod: " + code);
                        System.out.println("1 Wyświetl ponownie");
                        System.out.println("2 Zapisz do pliku");
                        System.out.println("0 Powrót");
                        String action = scanner.nextLine().trim();
                        if ("1".equals(action)) {
                            System.out.println("Kod: " + code);
                        } else if ("2".equals(action)) {
                            System.out.print("Nazwa pliku: ");
                            String fileName = scanner.nextLine().trim();
                            saveToFile(fileName, code);
                            System.out.println("Zapisano");
                        }
                        break;
                    case "2":
                        store.serialize("secure_data.ser");
                        System.out.println("Zapisano secure_data.ser");
                        break;
                    case "3":
                        store.deserialize("secure_data.ser");
                        System.out.println("Wczytano secure_data.ser");
                        break;
                    case "0":
                        store.serialize("secure_data.ser");
                        running = false;
                        break;
                    default:
                        System.out.println("Nieprawidłowy wybór");
                }
            } catch (Exception e) {
                System.out.println("Błąd: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void saveToFile(String fileName, String code) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(code);
        }
    }
}

class CodeGenerator {
    private final SecureRandom random = new SecureRandom();
    private final CodeHistoryStore store = CodeHistoryStore.getInstance();
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public String generate(int length) {
        if (length != 4 && length != 6 && length != 12 && length != 16) {
            throw new IllegalArgumentException("Dozwolone długości: 4,6,12,16");
        }
        if (store.isLocked(length)) {
            throw new IllegalStateException("Nie można zmienić kodu o długości " + length + " w ciągu godziny");
        }
        String alphabet = (length == 4 || length == 6) ? DIGITS : ALPHANUM;
        String code;
        int attempts = 0;
        do {
            code = generateCode(length, alphabet);
            attempts++;
            if (attempts > 1000) {
                throw new IllegalStateException("Nie można wygenerować unikalnego kodu");
            }
        } while (store.isInHistory(length, code));
        store.add(length, code);
        return code;
    }

    private String generateCode(int length, String alphabet) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}

class CodeHistoryStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final CodeHistoryStore INSTANCE = new CodeHistoryStore();
    private static final long LOCK_MILLIS = 3600_000L;
    private static final int MAX_HISTORY = 3;
    private final Map<Integer, Deque<String>> history = new HashMap<>();
    private final Map<Integer, Long> lastChange = new HashMap<>();

    private CodeHistoryStore() {
    }

    public static CodeHistoryStore getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isLocked(int length) {
        Long last = lastChange.get(length);
        if (last == null) {
            return false;
        }
        return System.currentTimeMillis() - last < LOCK_MILLIS;
    }

    public synchronized boolean isInHistory(int length, String code) {
        Deque<String> deque = history.get(length);
        return deque != null && deque.contains(code);
    }

    public synchronized void add(int length, String code) {
        Deque<String> deque = history.computeIfAbsent(length, k -> new ArrayDeque<>());
        deque.remove(code);
        deque.addFirst(code);
        while (deque.size() > MAX_HISTORY) {
            deque.removeLast();
        }
        lastChange.put(length, System.currentTimeMillis());
    }

    public synchronized void serialize(String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this);
        }
    }

    public synchronized void deserialize(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            CodeHistoryStore loaded = (CodeHistoryStore) ois.readObject();
            this.history.clear();
            this.history.putAll(loaded.history);
            this.lastChange.clear();
            this.lastChange.putAll(loaded.lastChange);
        }
    }

    public synchronized Map<Integer, List<String>> getHistoryView() {
        Map<Integer, List<String>> view = new HashMap<>();
        for (Map.Entry<Integer, Deque<String>> entry : history.entrySet()) {
            view.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return view;
    }
}

//Tobiasz Burzynski 5TP
