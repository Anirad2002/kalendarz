package app;

import java.util.Comparator;

public class SortujKontaktPoImieniu implements Comparator<Kontakt> {

    @Override
    public int compare(Kontakt k1, Kontakt k2) {
        return k1.getImie().compareTo(k2.getImie());
    }
}
