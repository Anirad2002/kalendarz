package app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Zdarzenie implements Comparable<Zdarzenie> {

    private long id;
    private String nazwa;
    private String opis;
    private LocalDateTime data;
    private Lokalizacja lokalizacja;
    private List<Kontakt> kontakty = new ArrayList<>();

    private static long nextTempId = -1;

    public Zdarzenie(String nazwa, String opis, LocalDateTime data, Lokalizacja lokalizacja) {
        this.id = nextTempId--;
        this.nazwa = nazwa;
        this.opis = opis;
        this.data = data;
        this.lokalizacja = lokalizacja;
    }

    public Zdarzenie(long id, String nazwa, String opis, LocalDateTime data, Lokalizacja lokalizacja) {
        this(nazwa, opis, data, lokalizacja);
        this.id = id;
    }

    // Getters / Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNazwa() { return nazwa; }
    public void setNazwa(String nazwa) { this.nazwa = nazwa; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public Lokalizacja getLokalizacja() { return lokalizacja; }
    public void setLokalizacja(Lokalizacja lokalizacja) { this.lokalizacja = lokalizacja; }

    public List<Kontakt> getKontakty() { return kontakty; }
    public void setKontakty(List<Kontakt> kontakty) { this.kontakty = kontakty; }

    @Override
    public int compareTo(Zdarzenie z) { return this.data.compareTo(z.data); }

    public static class SortujPoNazwie implements Comparator<Zdarzenie> {
        public int compare(Zdarzenie z1, Zdarzenie z2) { return z1.getNazwa().compareTo(z2.getNazwa()); }
    }

    public static class SortujPoOpis implements Comparator<Zdarzenie> {
        public int compare(Zdarzenie z1, Zdarzenie z2) { return z1.getOpis().compareTo(z2.getOpis()); }
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return nazwa + " | " + data.format(fmt) + " | " + opis + " | " + lokalizacja;
    }
}