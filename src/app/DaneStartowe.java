package app;

import java.time.LocalDateTime;
import java.util.List;

public class DaneStartowe {

    public static boolean uzupelnijJesliPuste(List<Kontakt> kontakty, List<Zdarzenie> zdarzenia) {
        if (!kontakty.isEmpty() || !zdarzenia.isEmpty()) {
            return false;
        }

        kontakty.add(new Kontakt("Anna", "Nowak", "+48600100200", "a.nowak@gmail.com"));
        kontakty.add(new Kontakt("Piotr", "Zając", "+48601200300", "p.zajac@gmail.com"));
        kontakty.add(new Kontakt("Krzysztof", "Lewandowski", "+48602300400", "k.lewandowski@gmail.com"));
        kontakty.add(new Kontakt("Ewa", "Kamińska", "+48603400500", "e.kaminska@gmail.com"));
        kontakty.add(new Kontakt("Marek", "Szymański", "+48604500600", "m.szymanski@gmail.com"));
        kontakty.add(new Kontakt("Katarzyna", "Woźniak", "+48605600700", "k.wozniak@gmail.com"));
        kontakty.add(new Kontakt("Łukasz", "Mazur", "+48606700800", "l.mazur@gmail.com"));
        kontakty.add(new Kontakt("Tomasz", "Dąbrowski", "+48607800900", "t.dabrowski@gmail.com"));
        kontakty.add(new Kontakt("Olena", "Kovalenko", "+48608900100", "o.kovalenko@gmail.com"));
        kontakty.add(new Kontakt("Daryna", "Pasiura", "+48609000200", "d.pasiura@gmail.com"));
        
        Lokalizacja online = new Lokalizacja("50,32,40", "21,20,10");
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 03", "zajęcia online", LocalDateTime.of(2026, 3, 26, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 05", "zajęcia offline", LocalDateTime.of(2026, 3, 24, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 07", "zajęcia online", LocalDateTime.of(2026, 3, 30, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 02", "zajęcia online", LocalDateTime.of(2026, 3, 25, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 01", "zajęcia online", LocalDateTime.of(2026, 3, 28, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 09", "zajęcia online", LocalDateTime.of(2026, 4, 1, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 04", "zajęcia offline", LocalDateTime.of(2026, 3, 27, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 06", "zajęcia online", LocalDateTime.of(2026, 3, 29, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 08", "zajęcia online", LocalDateTime.of(2026, 4, 2, 8, 0), online));
        zdarzenia.add(new Zdarzenie("Projekt Zespołowy 10", "zajęcia online", LocalDateTime.of(2026, 3, 31, 8, 0), online));

        polacz(kontakty.get(0), zdarzenia.get(0));
        polacz(kontakty.get(1), zdarzenia.get(1));
        return true;
    }

    private static void polacz(Kontakt kontakt, Zdarzenie zdarzenie) {
        kontakt.getZdarzenia().add(zdarzenie);
        zdarzenie.getKontakty().add(kontakt);
    }
}
