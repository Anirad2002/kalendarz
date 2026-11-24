package app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && "-gui".equalsIgnoreCase(args[0])) {
            uruchomTrybGraficzny();
            return;
        }

        if (args.length > 0 && !"-txt".equalsIgnoreCase(args[0])) {
            System.out.println("Nieznany tryb: " + args[0]);
            System.out.println("Użycie: java app.Main -txt albo java app.Main -gui");
        }

        uruchomTrybTekstowy();
    }

    private static void uruchomTrybGraficzny() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                KalendarzFrame frame = new KalendarzFrame();
                frame.setVisible(true);
            }
        });
    }

    private static void uruchomTrybTekstowy() {
        ArrayList<Kontakt> kontakty = new ArrayList<>();
        ArrayList<Zdarzenie> zdarzenia = new ArrayList<>();
        DBManager.loadData(kontakty, zdarzenia);

        if (DaneStartowe.uzupelnijJesliPuste(kontakty, zdarzenia)) {
            System.out.println("Baza danych pusta - dodaję przykładowe dane.");
        }

        Collections.sort(zdarzenia);
        System.out.println("=== Zdarzenia posortowane po dacie ===");
        for (Zdarzenie z : zdarzenia) System.out.println(z);

        Collections.sort(zdarzenia, new Zdarzenie.SortujPoNazwie());
        System.out.println("\n=== Zdarzenia posortowane po nazwie ===");
        for (Zdarzenie z : zdarzenia) System.out.println(z);

        Collections.sort(zdarzenia, new Zdarzenie.SortujPoOpis());
        System.out.println("\n=== Zdarzenia posortowane po opisie ===");
        for (Zdarzenie z : zdarzenia) System.out.println(z);

        Collections.sort(kontakty);
        System.out.println("\n=== Kontakty posortowane po nazwisku ===");
        for (Kontakt k : kontakty) System.out.println(k);

        Collections.sort(kontakty, new Kontakt.SortujPoImieniu());
        System.out.println("\n=== Kontakty posortowane po imieniu ===");
        for (Kontakt k : kontakty) System.out.println(k);

        Collections.sort(kontakty, new Kontakt.SortujPoEmail());
        System.out.println("\n=== Kontakty posortowane po email ===");
        for (Kontakt k : kontakty) System.out.println(k);

        System.out.println("\n--- Symulacja zmian w trakcie działania ---");
        Kontakt nowyKontakt = znajdzKontaktPoEmail(kontakty, "jan.kowalski@example.com");
        if (nowyKontakt == null) {
            nowyKontakt = new Kontakt("Jan", "Kowalski", "+48700700800", "jan.kowalski@example.com");
            kontakty.add(nowyKontakt);
        } else {
            nowyKontakt.setTelefon("+48700700800");
        }

        Zdarzenie noweZdarzenie = znajdzZdarzeniePoNazwie(zdarzenia, "Spotkanie integracyjne");
        if (noweZdarzenie == null) {
            noweZdarzenie = new Zdarzenie("Spotkanie integracyjne", "spotkanie w biurze",
                    LocalDateTime.of(2026, 5, 15, 18, 0), new Lokalizacja("52,15,30", "21,0,0"));
            zdarzenia.add(noweZdarzenie);
        } else {
            noweZdarzenie.setOpis("spotkanie w biurze");
        }

        polacz(nowyKontakt, noweZdarzenie);
        System.out.println("Dodano lub zaktualizowano kontakt i zdarzenie.");

        XmlHelper.zapiszDoXml(kontakty, zdarzenia, "kalendarz.xml");

        System.out.println("\n=== Odczyt z pliku XML (kopia tymczasowa) ===");
        XmlHelper.odczytajZXml("kalendarz.xml");

        System.out.println("\n--- Zakończenie programu - zapis do bazy danych ---");
        if (DBManager.saveData(kontakty, zdarzenia)) {
            System.out.println("Dane zostały zapisane do SQLite. Program kończy działanie.");
        } else {
            System.out.println("Nie udało się zapisać danych do SQLite.");
        }
    }

    private static Kontakt znajdzKontaktPoEmail(List<Kontakt> kontakty, String email) {
        for (Kontakt k : kontakty) {
            if (k.getEmail() != null && k.getEmail().getAddress().equalsIgnoreCase(email)) {
                return k;
            }
        }
        return null;
    }

    private static Zdarzenie znajdzZdarzeniePoNazwie(List<Zdarzenie> zdarzenia, String nazwa) {
        for (Zdarzenie z : zdarzenia) {
            if (z.getNazwa().equalsIgnoreCase(nazwa)) {
                return z;
            }
        }
        return null;
    }

    private static void polacz(Kontakt kontakt, Zdarzenie zdarzenie) {
        if (!kontakt.getZdarzenia().contains(zdarzenie)) {
            kontakt.getZdarzenia().add(zdarzenie);
        }
        if (!zdarzenie.getKontakty().contains(kontakt)) {
            zdarzenie.getKontakty().add(kontakt);
        }
    }
}
