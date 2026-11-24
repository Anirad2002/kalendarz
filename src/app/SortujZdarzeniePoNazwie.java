package app;

import java.util.Comparator;

public class SortujZdarzeniePoNazwie implements Comparator<Zdarzenie> {

    @Override
    public int compare(Zdarzenie z1, Zdarzenie z2) {
        return z1.getNazwa().compareTo(z2.getNazwa());
    }
}
