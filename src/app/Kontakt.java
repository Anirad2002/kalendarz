package app;

import jakarta.mail.internet.InternetAddress;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Kontakt implements Comparable<Kontakt> {

    private long id;
    private String imie;
    private String nazwisko;
    private Phonenumber.PhoneNumber telefon;
    private InternetAddress email;
    private List<Zdarzenie> zdarzenia = new ArrayList<>();

    private static long nextTempId = -1;

    public Kontakt(String imie, String nazwisko, String telefon, String email) {
        this.id = nextTempId--;
        this.imie = imie;
        this.nazwisko = nazwisko;
        setTelefon(telefon);
        setEmail(email);
    }

    public Kontakt(long id, String imie, String nazwisko, String telefon, String email) {
        this(imie, nazwisko, telefon, email);
        this.id = id;
    }

    // Getters / Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getImie() { return imie; }
    public void setImie(String imie) { this.imie = imie; }

    public String getNazwisko() { return nazwisko; }
    public void setNazwisko(String nazwisko) { this.nazwisko = nazwisko; }

    public Phonenumber.PhoneNumber getTelefon() { return telefon; }
    public void setTelefon(String telefon) {
        if (telefon == null || telefon.trim().isEmpty()) {
            this.telefon = null;
            return;
        }

        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            this.telefon = phoneUtil.parse(telefon, "PL");
        } catch (Exception e) {
            this.telefon = odczytajStaryFormatTelefonu(telefon);
            if (this.telefon == null) {
                System.err.println("Niepoprawny numer telefonu: " + telefon);
            }
        }
    }

    public InternetAddress getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            this.email = null;
            return;
        }

        try {
            this.email = new InternetAddress(email);
            this.email.validate();
        } catch (Exception e) {
            this.email = null;
            System.err.println("Niepoprawny email: " + email);
        }
    }

    public List<Zdarzenie> getZdarzenia() { return zdarzenia; }
    public void setZdarzenia(List<Zdarzenie> zdarzenia) { this.zdarzenia = zdarzenia; }

    @Override
    public int compareTo(Kontakt k) { return this.nazwisko.compareTo(k.nazwisko); }

    public static class SortujPoImieniu implements Comparator<Kontakt> {
        public int compare(Kontakt k1, Kontakt k2) { return k1.getImie().compareTo(k2.getImie()); }
    }

    public static class SortujPoEmail implements Comparator<Kontakt> {
        public int compare(Kontakt k1, Kontakt k2) {
            String email1 = k1.getEmail() != null ? k1.getEmail().getAddress() : "";
            String email2 = k2.getEmail() != null ? k2.getEmail().getAddress() : "";
            return email1.compareTo(email2);
        }
    }

    @Override
    public String toString() {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String telefonTekst = telefon != null
                ? phoneUtil.format(telefon, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                : "brak";
        String emailTekst = email != null ? email.getAddress() : "brak";

        return imie + " " + nazwisko + ", tel: " +
               telefonTekst + ", email: " + emailTekst;
    }

    private Phonenumber.PhoneNumber odczytajStaryFormatTelefonu(String telefon) {
        Pattern pattern = Pattern.compile("Country Code:\\s*(\\d+)\\s+National Number:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(telefon);
        if (!matcher.find()) {
            return null;
        }

        return new Phonenumber.PhoneNumber()
                .setCountryCode(Integer.parseInt(matcher.group(1)))
                .setNationalNumber(Long.parseLong(matcher.group(2)));
    }
}
