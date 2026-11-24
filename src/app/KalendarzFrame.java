package app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class KalendarzFrame extends JFrame {

    private final List<Kontakt> kontakty = new ArrayList<>();
    private final List<Zdarzenie> zdarzenia = new ArrayList<>();
    private final DateTimeFormatter widokDaty = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DefaultTableModel kontaktyModel = model("ID", "Imię", "Nazwisko", "Telefon", "Email", "Zdarzenia");
    private final DefaultTableModel zdarzeniaModel = model("ID", "Nazwa", "Data", "Opis", "Kontakty", "Lat", "Lng");
    private final JTable kontaktyTable = tabela(kontaktyModel, "Tabela kontaktów.");
    private final JTable zdarzeniaTable = tabela(zdarzeniaModel, "Tabela zdarzeń.");

    private final JTextField imieField = pole("Imię kontaktu.");
    private final JTextField nazwiskoField = pole("Nazwisko kontaktu.");
    private final JTextField telefonField = pole("Numer telefonu, np. +48600100200.");
    private final JTextField emailField = pole("Adres email.");

    private final JTextField nazwaField = pole("Nazwa zdarzenia.");
    private final JTextField opisField = pole("Opis zdarzenia.");
    private final JTextField dataField = pole("Data: 2026-05-15T18:00 albo 2026-05-15 18:00.");
    private final JTextField latField = pole("Szerokość geograficzna.");
    private final JTextField lngField = pole("Długość geograficzna.");

    private final JComboBox<String> sortKontaktCombo = new JComboBox<>(new String[] {"Nazwisko", "Imię", "Email"});
    private final JComboBox<String> sortZdarzenieCombo = new JComboBox<>(new String[] {"Data", "Nazwa", "Opis"});
    private final JComboBox<Kontakt> uczestnikCombo = new JComboBox<>();
    private final JProgressBar progressBar = new JProgressBar();
    private final JSlider rozmiarTekstuSlider = new JSlider(12, 22, 14);
    private final JLabel statusLabel = new JLabel("Gotowe.");

    private JDesktopPane desktopPane;

    public KalendarzFrame() {
        super("Kalendarz - tryb graficzny");
        DBManager.loadData(kontakty, zdarzenia);
        DaneStartowe.uzupelnijJesliPuste(kontakty, zdarzenia);

        ustawOkno();
        budujGui();
        odswiez("Dane wczytane do pamięci.");
    }

    private void ustawOkno() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                zamknij();
            }
        });
    }

    private void budujGui() {
        setJMenuBar(menu());
        kontaktyTable.getSelectionModel().addListSelectionListener(e -> pokazKontakt(wybranyKontakt()));
        zdarzeniaTable.getSelectionModel().addListSelectionListener(e -> pokazZdarzenie(wybraneZdarzenie()));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, zakladkiTabel(), panelEdycji());
        split.setResizeWeight(0.62);
        split.setDividerLocation(670);
        add(split, BorderLayout.CENTER);
        add(panelStatusu(), BorderLayout.SOUTH);
    }

    private JMenuBar menu() {
        JMenuBar bar = new JMenuBar();
        bar.add(menu("Plik",
                item("Odczytaj z bazy", this::odczytajZBazy),
                item("Zapisz do bazy", this::zapiszDoBazy),
                null,
                item("Odczytaj XML...", this::odczytajXml),
                item("Zapisz XML...", this::zapiszXml),
                null,
                item("Wyjście", this::zamknij)));
        bar.add(menu("Edycja",
                item("Nowy kontakt", this::czyscKontakt),
                item("Nowe zdarzenie", this::czyscZdarzenie)));
        bar.add(menu("Narzędzia", item("Kolor tła...", this::wybierzKolor)));
        bar.add(menu("Pomoc", item("O programie", this::oProgramie)));
        return bar;
    }

    private JTabbedPane zakladkiTabel() {
        sortKontaktCombo.setToolTipText("Sortowanie kontaktów.");
        sortKontaktCombo.addActionListener(e -> sortujKontakty());
        sortZdarzenieCombo.setToolTipText("Sortowanie zdarzeń.");
        sortZdarzenieCombo.addActionListener(e -> sortujZdarzenia());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Kontakty", panelTabeli(kontaktyTable, sortKontaktCombo, "Odświeżono kontakty."));
        tabs.addTab("Zdarzenia", panelTabeli(zdarzeniaTable, sortZdarzenieCombo, "Odświeżono zdarzenia."));
        return tabs;
    }

    private JPanel panelTabeli(JTable table, JComboBox<String> sortCombo, String status) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel();
        top.add(new JLabel("Sortuj:"));
        top.add(sortCombo);
        top.add(button("Odśwież", "Odświeża tabelę.", () -> odswiez(status)));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JDesktopPane panelEdycji() {
        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(238, 242, 247));

        JInternalFrame frame = new JInternalFrame("Formularze edycji", false, false, false, false);
        frame.setContentPane(formularze());
        frame.setVisible(true);
        desktopPane.add(frame, JLayeredPane.DEFAULT_LAYER);

        desktopPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                frame.setBounds(12, 12, Math.max(330, desktopPane.getWidth() - 24),
                        Math.max(480, desktopPane.getHeight() - 24));
            }
        });
        frame.setBounds(12, 12, 390, 560);
        return desktopPane;
    }

    private JTabbedPane formularze() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Kontakt", formKontakt());
        tabs.addTab("Zdarzenie", formZdarzenie());
        return tabs;
    }

    private JPanel formKontakt() {
        JPanel panel = formPanel();
        int r = 0;
        row(panel, r++, "Imię:", imieField);
        row(panel, r++, "Nazwisko:", nazwiskoField);
        row(panel, r++, "Telefon:", telefonField);
        row(panel, r++, "Email:", emailField);
        wide(panel, r, buttons(
                button("Nowy", "Czyści formularz kontaktu.", this::czyscKontakt),
                button("Dodaj", "Dodaje kontakt.", this::dodajKontakt),
                button("Zapisz zmiany", "Zapisuje kontakt.", this::edytujKontakt),
                button("Usuń", "Usuwa kontakt.", this::usunKontakt)));
        return panel;
    }

    private JPanel formZdarzenie() {
        JPanel panel = formPanel();
        uczestnikCombo.setToolTipText("Kontakt dodawany jako uczestnik zdarzenia.");

        int r = 0;
        row(panel, r++, "Nazwa:", nazwaField);
        row(panel, r++, "Opis:", opisField);
        row(panel, r++, "Data:", dataField);
        row(panel, r++, "Lat:", latField);
        row(panel, r++, "Lng:", lngField);
        row(panel, r++, "Uczestnik:", uczestnikCombo);
        wide(panel, r, buttons(
                button("Nowe", "Czyści formularz zdarzenia.", this::czyscZdarzenie),
                button("Dodaj", "Dodaje zdarzenie.", this::dodajZdarzenie),
                button("Zapisz zmiany", "Zapisuje zdarzenie.", this::edytujZdarzenie),
                button("Usuń", "Usuwa zdarzenie.", this::usunZdarzenie),
                button("Dodaj uczestnika", "Łączy kontakt ze zdarzeniem.", this::dodajUczestnika)));
        return panel;
    }

    private JPanel panelStatusu() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        progressBar.setStringPainted(true);
        progressBar.setToolTipText("Liczba rekordów w pamięci.");
        rozmiarTekstuSlider.setToolTipText("Rozmiar tekstu w tabelach.");
        rozmiarTekstuSlider.addChangeListener(e -> ustawRozmiarTekstu());
        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(rozmiarTekstuSlider, BorderLayout.EAST);
        return panel;
    }

    private void odswiez(String status) {
        wypelnijKontakty();
        wypelnijZdarzenia();
        wypelnijComboKontaktow();
        progressBar.setMaximum(Math.max(1, kontakty.size() + zdarzenia.size()));
        progressBar.setValue(kontakty.size() + zdarzenia.size());
        progressBar.setString((kontakty.size() + zdarzenia.size()) + " rekordów");
        statusLabel.setText(status);
    }

    private void wypelnijKontakty() {
        kontaktyModel.setRowCount(0);
        for (Kontakt k : kontakty) {
            kontaktyModel.addRow(new Object[] {
                    k.getId(), k.getImie(), k.getNazwisko(), telefon(k, true),
                    k.getEmail() != null ? k.getEmail().getAddress() : "", k.getZdarzenia().size()
            });
        }
    }

    private void wypelnijZdarzenia() {
        zdarzeniaModel.setRowCount(0);
        for (Zdarzenie z : zdarzenia) {
            double lat = wsp(z, true);
            double lng = wsp(z, false);
            zdarzeniaModel.addRow(new Object[] {
                    z.getId(), z.getNazwa(), z.getData().format(widokDaty),
                    z.getOpis(), z.getKontakty().size(), lat, lng
            });
        }
    }

    private void wypelnijComboKontaktow() {
        Kontakt wybrany = (Kontakt) uczestnikCombo.getSelectedItem();
        DefaultComboBoxModel<Kontakt> model = new DefaultComboBoxModel<>();
        for (Kontakt k : kontakty) model.addElement(k);
        uczestnikCombo.setModel(model);
        if (wybrany != null) uczestnikCombo.setSelectedItem(wybrany);
    }

    private void sortujKontakty() {
        String wybor = (String) sortKontaktCombo.getSelectedItem();
        if ("Imię".equals(wybor)) Collections.sort(kontakty, new Kontakt.SortujPoImieniu());
        else if ("Email".equals(wybor)) Collections.sort(kontakty, new Kontakt.SortujPoEmail());
        else Collections.sort(kontakty);
        odswiez("Posortowano kontakty.");
    }

    private void sortujZdarzenia() {
        String wybor = (String) sortZdarzenieCombo.getSelectedItem();
        if ("Nazwa".equals(wybor)) Collections.sort(zdarzenia, new Zdarzenie.SortujPoNazwie());
        else if ("Opis".equals(wybor)) Collections.sort(zdarzenia, new Zdarzenie.SortujPoOpis());
        else Collections.sort(zdarzenia);
        odswiez("Posortowano zdarzenia.");
    }

    private void pokazKontakt(Kontakt k) {
        if (k == null) return;
        imieField.setText(k.getImie());
        nazwiskoField.setText(k.getNazwisko());
        telefonField.setText(telefon(k, false));
        emailField.setText(k.getEmail() != null ? k.getEmail().getAddress() : "");
    }

    private void pokazZdarzenie(Zdarzenie z) {
        if (z == null) return;
        nazwaField.setText(z.getNazwa());
        opisField.setText(z.getOpis());
        dataField.setText(z.getData().toString());
        latField.setText(String.valueOf(wsp(z, true)));
        lngField.setText(String.valueOf(wsp(z, false)));
    }

    private void czyscKontakt() {
        kontaktyTable.clearSelection();
        imieField.setText("");
        nazwiskoField.setText("");
        telefonField.setText("");
        emailField.setText("");
        statusLabel.setText("Nowy kontakt.");
    }

    private void czyscZdarzenie() {
        zdarzeniaTable.clearSelection();
        nazwaField.setText("");
        opisField.setText("");
        dataField.setText("2026-05-15T18:00");
        latField.setText("52.258333");
        lngField.setText("21.0");
        statusLabel.setText("Nowe zdarzenie.");
    }

    private void dodajKontakt() {
        kontakty.add(new Kontakt(imieField.getText(), nazwiskoField.getText(), telefonField.getText(), emailField.getText()));
        odswiez("Dodano kontakt.");
    }

    private void edytujKontakt() {
        Kontakt k = wybranyKontakt();
        if (k == null) {
            info("Wybierz kontakt w tabeli.");
            return;
        }
        k.setImie(imieField.getText());
        k.setNazwisko(nazwiskoField.getText());
        k.setTelefon(telefonField.getText());
        k.setEmail(emailField.getText());
        odswiez("Zmieniono kontakt.");
    }

    private void usunKontakt() {
        Kontakt k = wybranyKontakt();
        if (k == null) {
            info("Wybierz kontakt do usunięcia.");
            return;
        }
        for (Zdarzenie z : new ArrayList<>(k.getZdarzenia())) z.getKontakty().remove(k);
        kontakty.remove(k);
        czyscKontakt();
        odswiez("Usunięto kontakt.");
    }

    private void dodajZdarzenie() {
        LocalDateTime data = dataZFormularza();
        if (data == null) return;
        zdarzenia.add(new Zdarzenie(nazwaField.getText(), opisField.getText(), data, lokalizacja()));
        odswiez("Dodano zdarzenie.");
    }

    private void edytujZdarzenie() {
        Zdarzenie z = wybraneZdarzenie();
        LocalDateTime data = dataZFormularza();
        if (z == null || data == null) {
            if (z == null) info("Wybierz zdarzenie w tabeli.");
            return;
        }
        z.setNazwa(nazwaField.getText());
        z.setOpis(opisField.getText());
        z.setData(data);
        z.setLokalizacja(lokalizacja());
        odswiez("Zmieniono zdarzenie.");
    }

    private void usunZdarzenie() {
        Zdarzenie z = wybraneZdarzenie();
        if (z == null) {
            info("Wybierz zdarzenie do usunięcia.");
            return;
        }
        for (Kontakt k : new ArrayList<>(z.getKontakty())) k.getZdarzenia().remove(z);
        zdarzenia.remove(z);
        czyscZdarzenie();
        odswiez("Usunięto zdarzenie.");
    }

    private void dodajUczestnika() {
        Kontakt k = (Kontakt) uczestnikCombo.getSelectedItem();
        Zdarzenie z = wybraneZdarzenie();
        if (k == null || z == null) {
            info("Wybierz zdarzenie i kontakt.");
            return;
        }
        if (!z.getKontakty().contains(k)) z.getKontakty().add(k);
        if (!k.getZdarzenia().contains(z)) k.getZdarzenia().add(z);
        odswiez("Dodano uczestnika do zdarzenia.");
    }

    private void odczytajZBazy() {
        DBManager.loadData(kontakty, zdarzenia);
        DaneStartowe.uzupelnijJesliPuste(kontakty, zdarzenia);
        odswiez("Odczytano dane z bazy.");
    }

    private void zapiszDoBazy() {
        if (DBManager.saveData(kontakty, zdarzenia)) odswiez("Zapisano dane do bazy.");
        else info("Nie udało się zapisać danych do bazy.");
    }

    private void odczytajXml() {
        File plik = wybierzPlik(true);
        if (plik == null) return;
        if (XmlHelper.odczytajZXmlDoList(plik.getAbsolutePath(), kontakty, zdarzenia)) {
            odswiez("Odczytano XML: " + plik.getName());
        } else {
            info("Nie udało się odczytać pliku XML.");
        }
    }

    private void zapiszXml() {
        File plik = wybierzPlik(false);
        if (plik == null) return;
        if ("kalendarz.db".equalsIgnoreCase(plik.getName())) {
            info("Nie zapisuj XML jako kalendarz.db. Wybierz plik z rozszerzeniem .xml.");
            return;
        }
        XmlHelper.zapiszDoXml(kontakty, zdarzenia, plik.getAbsolutePath());
        odswiez("Zapisano XML: " + plik.getName());
    }

    private void wybierzKolor() {
        Color kolor = JColorChooser.showDialog(this, "Wybierz kolor tła", desktopPane.getBackground());
        if (kolor != null) {
            desktopPane.setBackground(kolor);
            statusLabel.setText("Zmieniono kolor tła.");
        }
    }

    private void oProgramie() {
        JOptionPane.showMessageDialog(this,
                "Kalendarz\nProjekt zespołowy 2025-2026\nTryb GUI wykonany w Swing.",
                "O programie", JOptionPane.INFORMATION_MESSAGE);
    }

    private void zamknij() {
        int decyzja = JOptionPane.showConfirmDialog(this, "Zapisać dane do bazy przed zamknięciem?",
                "Zamykanie programu", JOptionPane.YES_NO_CANCEL_OPTION);
        if (decyzja == JOptionPane.CANCEL_OPTION || decyzja == JOptionPane.CLOSED_OPTION) return;
        if (decyzja == JOptionPane.YES_OPTION && !DBManager.saveData(kontakty, zdarzenia)) {
            info("Nie udało się zapisać danych do bazy.");
            return;
        }
        dispose();
        System.exit(0);
    }

    private Kontakt wybranyKontakt() {
        Long id = wybraneId(kontaktyTable, kontaktyModel);
        if (id == null) return null;
        for (Kontakt k : kontakty) if (k.getId() == id) return k;
        return null;
    }

    private Zdarzenie wybraneZdarzenie() {
        Long id = wybraneId(zdarzeniaTable, zdarzeniaModel);
        if (id == null) return null;
        for (Zdarzenie z : zdarzenia) if (z.getId() == id) return z;
        return null;
    }

    private Long wybraneId(JTable table, DefaultTableModel model) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return ((Number) model.getValueAt(modelRow, 0)).longValue();
    }

    private LocalDateTime dataZFormularza() {
        try {
            String tekst = dataField.getText().trim().replace(" ", "T");
            return LocalDateTime.parse(tekst.length() == 10 ? tekst + "T00:00" : tekst);
        } catch (Exception e) {
            info("Niepoprawna data. Użyj formatu 2026-05-15T18:00.");
            return null;
        }
    }

    private Lokalizacja lokalizacja() {
        return new Lokalizacja(liczba(latField.getText()) + ",0,0", liczba(lngField.getText()) + ",0,0");
    }

    private double liczba(String tekst) {
        try {
            return Double.parseDouble(tekst.trim().replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double wsp(Zdarzenie z, boolean lat) {
        if (z.getLokalizacja() == null || z.getLokalizacja().getWspolrzedne() == null) return 0.0;
        return lat ? z.getLokalizacja().getWspolrzedne().getLatitude()
                : z.getLokalizacja().getWspolrzedne().getLongitude();
    }

    private String telefon(Kontakt k, boolean international) {
        if (k.getTelefon() == null) return "";
        PhoneNumberUtil.PhoneNumberFormat format = international
                ? PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                : PhoneNumberUtil.PhoneNumberFormat.E164;
        return PhoneNumberUtil.getInstance().format(k.getTelefon(), format);
    }

    private File wybierzPlik(boolean open) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setSelectedFile(new File("kalendarz.xml"));
        chooser.setDialogTitle(open ? "Odczytaj XML" : "Zapisz XML");
        int result = open ? chooser.showOpenDialog(this) : chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return null;

        File plik = chooser.getSelectedFile();
        if (!open && !plik.getName().toLowerCase().endsWith(".xml")) {
            plik = new File(plik.getParentFile(), plik.getName() + ".xml");
        }
        return plik;
    }

    private void ustawRozmiarTekstu() {
        int size = rozmiarTekstuSlider.getValue();
        Font font = kontaktyTable.getFont().deriveFont((float) size);
        kontaktyTable.setFont(font);
        zdarzeniaTable.setFont(font);
        kontaktyTable.setRowHeight(size + 8);
        zdarzeniaTable.setRowHeight(size + 8);
    }

    private void info(String text) {
        JOptionPane.showMessageDialog(this, text, "Kalendarz", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(text);
    }

    private DefaultTableModel model(Object... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable tabela(DefaultTableModel model, String tooltip) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setToolTipText(tooltip);
        return table;
    }

    private JTextField pole(String tooltip) {
        JTextField field = new JTextField();
        field.setToolTipText(tooltip);
        return field;
    }

    private JMenu menu(String title, JMenuItem... items) {
        JMenu menu = new JMenu(title);
        for (JMenuItem item : items) {
            if (item == null) menu.addSeparator();
            else menu.add(item);
        }
        return menu;
    }

    private JMenuItem item(String title, Runnable action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(e -> action.run());
        return item;
    }

    private JButton button(String title, String tooltip, Runnable action) {
        JButton button = new JButton(title);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JPanel buttons(JButton... buttons) {
        JPanel panel = new JPanel();
        for (JButton button : buttons) panel.add(button);
        return panel;
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private void row(JPanel panel, int row, String label, Component component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(4, 2, 4, 8);
        panel.add(new JLabel(label), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(4, 2, 4, 2);
        panel.add(component, right);
    }

    private void wide(JPanel panel, int row, Component component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 2, 2, 2);
        panel.add(component, gbc);
    }
}
