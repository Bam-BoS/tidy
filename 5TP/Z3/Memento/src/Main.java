import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Caretaker caretaker = new Caretaker();

        System.out.println("=== Wzorzec Memento - Person ===");
        System.out.println("Podaj dane poczatkowe osoby:");

        String imie = readString(scanner, "Imie: ");
        String nazwisko = readString(scanner, "Nazwisko: ");
        String dataUrodzenia = readString(scanner, "Data urodzenia (np. 1990-05-12): ");
        String miejsceUrodzenia = readString(scanner, "Miejsce urodzenia: ");
        String adresEmail = readString(scanner, "Adres email: ");
        String numerTelefonu = readString(scanner, "Numer telefonu: ");

        Person person = new Person(imie, nazwisko, dataUrodzenia,
                miejsceUrodzenia, adresEmail, numerTelefonu);

        System.out.println();
        System.out.println("Utworzono osobe.");
        caretaker.save(person.save());
        System.out.println("Automatycznie zapisano snapshot [0].");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt(scanner, "Wybor: ");

            switch (choice) {
                case 1:
                    System.out.println();
                    System.out.println("Aktualny stan:");
                    System.out.println(person);
                    System.out.println();
                    break;
                case 2:
                    editPerson(scanner, person);
                    break;
                case 3:
                    caretaker.save(person.save());
                    int idx = caretaker.size() - 1;
                    System.out.println("Zapisano snapshot [" + idx + "].");
                    break;
                case 4:
                    caretaker.showAll();
                    break;
                case 5:
                    if (caretaker.size() == 0) {
                        System.out.println("Brak zapisanych snapshotow.");
                        break;
                    }
                    caretaker.showAll();
                    int index = readInt(scanner, "Podaj indeks snapshotu do przywrocenia: ");
                    if (caretaker.restore(person, index)) {
                        System.out.println("Przywrocono snapshot [" + index + "].");
                        System.out.println("Aktualny stan:");
                        System.out.println(person);
                    }
                    System.out.println();
                    break;
                case 6:
                    caretaker.showAll();
                    int delIndex = readInt(scanner, "Podaj indeks snapshotu do usuniecia: ");
                    if (caretaker.remove(delIndex)) {
                        System.out.println("Usunieto snapshot [" + delIndex + "].");
                    }
                    System.out.println();
                    break;
                case 0:
                    running = false;
                    System.out.println("Koniec programu.");
                    break;
                default:
                    System.out.println("Nieznana opcja. Sprobuj ponownie.");
                    System.out.println();
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("--- MENU ---");
        System.out.println("1. Pokaz aktualny stan osoby");
        System.out.println("2. Edytuj dane osoby");
        System.out.println("3. Zapisz snapshot (Memento)");
        System.out.println("4. Pokaz wszystkie snapshoty");
        System.out.println("5. Przywroc wybrany snapshot");
        System.out.println("6. Usun wybrany snapshot");
        System.out.println("0. Wyjdz");
    }

    private static void editPerson(Scanner scanner, Person person) {
        System.out.println();
        System.out.println("--- EDYCJA ---");
        System.out.println("1. Imie          (teraz: " + person.getImie() + ")");
        System.out.println("2. Nazwisko      (teraz: " + person.getNazwisko() + ")");
        System.out.println("3. Data urodzenia(teraz: " + person.getDataUrodzenia() + ")");
        System.out.println("4. Miejsce ur.   (teraz: " + person.getMiejsceUrodzenia() + ")");
        System.out.println("5. Adres email   (teraz: " + person.getAdresEmail() + ")");
        System.out.println("6. Numer telefonu(teraz: " + person.getNumerTelefonu() + ")");
        System.out.println("0. Powrot");

        int choice = readInt(scanner, "Co chcesz zmienic: ");
        switch (choice) {
            case 1:
                person.setImie(readString(scanner, "Nowe imie: "));
                System.out.println("Zaktualizowano imie.");
                break;
            case 2:
                person.setNazwisko(readString(scanner, "Nowe nazwisko: "));
                System.out.println("Zaktualizowano nazwisko.");
                break;
            case 3:
                person.setDataUrodzenia(readString(scanner, "Nowa data urodzenia: "));
                System.out.println("Zaktualizowano date urodzenia.");
                break;
            case 4:
                person.setMiejsceUrodzenia(readString(scanner, "Nowe miejsce urodzenia: "));
                System.out.println("Zaktualizowano miejsce urodzenia.");
                break;
            case 5:
                person.setAdresEmail(readString(scanner, "Nowy adres email: "));
                System.out.println("Zaktualizowano adres email.");
                break;
            case 6:
                person.setNumerTelefonu(readString(scanner, "Nowy numer telefonu: "));
                System.out.println("Zaktualizowano numer telefonu.");
                break;
            case 0:
                break;
            default:
                System.out.println("Nieznana opcja.");
        }
        System.out.println();
    }

    private static String readString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return line;
                }
            }
            System.out.println("Wartosc nie moze byc pusta.");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Podaj liczbe calkowita.");
            }
        }
    }
}

class Person {
    private String imie;
    private String nazwisko;
    private String dataUrodzenia;
    private String miejsceUrodzenia;
    private String adresEmail;
    private String numerTelefonu;

    public Person(String imie,
                  String nazwisko,
                  String dataUrodzenia,
                  String miejsceUrodzenia,
                  String adresEmail,
                  String numerTelefonu) {
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.dataUrodzenia = dataUrodzenia;
        this.miejsceUrodzenia = miejsceUrodzenia;
        this.adresEmail = adresEmail;
        this.numerTelefonu = numerTelefonu;
    }

    public Memento save() {
        return new Memento(imie, nazwisko, dataUrodzenia,
                miejsceUrodzenia, adresEmail, numerTelefonu);
    }

    public void restore(Memento memento) {
        this.imie = memento.getImie();
        this.nazwisko = memento.getNazwisko();
        this.dataUrodzenia = memento.getDataUrodzenia();
        this.miejsceUrodzenia = memento.getMiejsceUrodzenia();
        this.adresEmail = memento.getAdresEmail();
        this.numerTelefonu = memento.getNumerTelefonu();
    }

    public String getImie() { return imie; }
    public void setImie(String imie) { this.imie = imie; }

    public String getNazwisko() { return nazwisko; }
    public void setNazwisko(String nazwisko) { this.nazwisko = nazwisko; }

    public String getDataUrodzenia() { return dataUrodzenia; }
    public void setDataUrodzenia(String dataUrodzenia) { this.dataUrodzenia = dataUrodzenia; }

    public String getMiejsceUrodzenia() { return miejsceUrodzenia; }
    public void setMiejsceUrodzenia(String miejsceUrodzenia) { this.miejsceUrodzenia = miejsceUrodzenia; }

    public String getAdresEmail() { return adresEmail; }
    public void setAdresEmail(String adresEmail) { this.adresEmail = adresEmail; }

    public String getNumerTelefonu() { return numerTelefonu; }
    public void setNumerTelefonu(String numerTelefonu) { this.numerTelefonu = numerTelefonu; }

    @Override
    public String toString() {
        return "Person {" +
                "imie='" + imie + '\'' +
                ", nazwisko='" + nazwisko + '\'' +
                ", dataUrodzenia='" + dataUrodzenia + '\'' +
                ", miejsceUrodzenia='" + miejsceUrodzenia + '\'' +
                ", adresEmail='" + adresEmail + '\'' +
                ", numerTelefonu='" + numerTelefonu + '\'' +
                '}';
    }

    public static class Memento {
        private final String imie;
        private final String nazwisko;
        private final String dataUrodzenia;
        private final String miejsceUrodzenia;
        private final String adresEmail;
        private final String numerTelefonu;

        private Memento(String imie,
                        String nazwisko,
                        String dataUrodzenia,
                        String miejsceUrodzenia,
                        String adresEmail,
                        String numerTelefonu) {
            this.imie = imie;
            this.nazwisko = nazwisko;
            this.dataUrodzenia = dataUrodzenia;
            this.miejsceUrodzenia = miejsceUrodzenia;
            this.adresEmail = adresEmail;
            this.numerTelefonu = numerTelefonu;
        }

        private String getImie() { return imie; }
        private String getNazwisko() { return nazwisko; }
        private String getDataUrodzenia() { return dataUrodzenia; }
        private String getMiejsceUrodzenia() { return miejsceUrodzenia; }
        private String getAdresEmail() { return adresEmail; }
        private String getNumerTelefonu() { return numerTelefonu; }

        @Override
        public String toString() {
            return "Memento {" +
                    "imie='" + imie + '\'' +
                    ", nazwisko='" + nazwisko + '\'' +
                    ", dataUrodzenia='" + dataUrodzenia + '\'' +
                    ", miejsceUrodzenia='" + miejsceUrodzenia + '\'' +
                    ", adresEmail='" + adresEmail + '\'' +
                    ", numerTelefonu='" + numerTelefonu + '\'' +
                    '}';
        }
    }
}

class Caretaker {
    private final List<Person.Memento> snapshots = new ArrayList<>();

    public void save(Person.Memento memento) {
        snapshots.add(memento);
    }

    public boolean restore(Person person, int index) {
        if (index < 0 || index >= snapshots.size()) {
            System.out.println("Brak snapshotu o indeksie " + index + ".");
            return false;
        }
        person.restore(snapshots.get(index));
        return true;
    }

    public boolean remove(int index) {
        if (index < 0 || index >= snapshots.size()) {
            System.out.println("Brak snapshotu o indeksie " + index + ".");
            return false;
        }
        snapshots.remove(index);
        return true;
    }

    public void showAll() {
        System.out.println();
        if (snapshots.isEmpty()) {
            System.out.println("Brak zapisanych snapshotow.");
            System.out.println();
            return;
        }
        System.out.println("Zachowane snapshoty (" + snapshots.size() + "):");
        for (int i = 0; i < snapshots.size(); i++) {
            System.out.println("  [" + i + "] " + snapshots.get(i));
        }
        System.out.println();
    }

    public int size() {
        return snapshots.size();
    }
}

//Tobiasz Burzynski 5TP