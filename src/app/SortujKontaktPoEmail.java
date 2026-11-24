package app;

import java.util.Comparator;

public class SortujKontaktPoEmail implements Comparator<Kontakt> {

    @Override
    public int compare(Kontakt k1, Kontakt k2) {
        String email1 = k1.getEmail() != null ? k1.getEmail().getAddress() : "";
        String email2 = k2.getEmail() != null ? k2.getEmail().getAddress() : "";
        return email1.compareTo(email2);
    }
}
