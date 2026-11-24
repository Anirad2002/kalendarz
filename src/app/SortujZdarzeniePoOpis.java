package app;

import java.util.Comparator;

public class SortujZdarzeniePoOpis implements Comparator<Zdarzenie> {

    @Override
    public int compare(Zdarzenie z1, Zdarzenie z2) {
        return z1.getOpis().compareTo(z2.getOpis());
    }
}
