package app;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class DBManager {

    private static final File DB_FILE = new File("kalendarz.db").getAbsoluteFile();
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE.getPath();
    private static boolean sterownikSqliteZaladowany = false;

    static {
        zaladujSterownikSqlite();
    }

    public static void loadData(List<Kontakt> kontakty, List<Zdarzenie> zdarzenia) {
        kontakty.clear();
        zdarzenia.clear();

        File odzyskanyXml = zabezpieczPlikBazy();
        if (odzyskanyXml != null && XmlHelper.odczytajZXmlDoList(odzyskanyXml.getAbsolutePath(), kontakty, zdarzenia)) {
            saveData(kontakty, zdarzenia);
            return;
        }

        try (Connection conn = openConnection()) {
            przygotujBaze(conn);

            String sqlKontakt = "SELECT id, imie, nazwisko, telefon, email FROM Kontakty";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlKontakt)) {
                while (rs.next()) {
                    Kontakt k = new Kontakt(
                            rs.getLong("id"),
                            rs.getString("imie"),
                            rs.getString("nazwisko"),
                            rs.getString("telefon"),
                            rs.getString("email"));
                    kontakty.add(k);
                }
            }

            String sqlZdarzenie = "SELECT id, nazwa, opis, data, lat, lng FROM Zdarzenia";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlZdarzenie)) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String nazwa = rs.getString("nazwa");
                    String opis = rs.getString("opis");
                    LocalDateTime data = LocalDateTime.parse(rs.getString("data"));
                    double lat = rs.getDouble("lat");
                    double lng = rs.getDouble("lng");
                    Lokalizacja lok = new Lokalizacja(lat + ",0,0", lng + ",0,0");
                    zdarzenia.add(new Zdarzenie(id, nazwa, opis, data, lok));
                }
            }

            Map<Long, Kontakt> kontaktMap = new HashMap<>();
            for (Kontakt k : kontakty) {
                kontaktMap.put(k.getId(), k);
            }

            Map<Long, Zdarzenie> zdarzenieMap = new HashMap<>();
            for (Zdarzenie z : zdarzenia) {
                zdarzenieMap.put(z.getId(), z);
            }

            String sqlRel = "SELECT kontakt_id, zdarzenie_id FROM Kontakt_Zdarzenie";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlRel)) {
                while (rs.next()) {
                    Kontakt k = kontaktMap.get(rs.getLong("kontakt_id"));
                    Zdarzenie z = zdarzenieMap.get(rs.getLong("zdarzenie_id"));
                    if (k != null && z != null) {
                        polacz(k, z);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading data from SQLite database:");
            e.printStackTrace();
        }
    }

    public static boolean saveData(List<Kontakt> kontakty, List<Zdarzenie> zdarzenia) {
        String deleteJunction = "DELETE FROM Kontakt_Zdarzenie";
        String deleteZdarzenia = "DELETE FROM Zdarzenia";
        String deleteKontakty = "DELETE FROM Kontakty";

        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            przygotujBaze(conn);
            conn.setAutoCommit(false);

            try {
                stmt.executeUpdate(deleteJunction);
                stmt.executeUpdate(deleteZdarzenia);
                stmt.executeUpdate(deleteKontakty);
                try {
                    stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('Kontakty', 'Zdarzenia')");
                } catch (SQLException e) {
                    // sqlite_sequence is created only after AUTOINCREMENT is used.
                }

                String insertKontakt = "INSERT INTO Kontakty (imie, nazwisko, telefon, email) VALUES (?, ?, ?, ?)";
                Map<Kontakt, Long> newContactIds = new HashMap<>();
                try (PreparedStatement pstmt = conn.prepareStatement(insertKontakt, Statement.RETURN_GENERATED_KEYS)) {
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    for (Kontakt k : kontakty) {
                        pstmt.setString(1, k.getImie());
                        pstmt.setString(2, k.getNazwisko());
                        if (k.getTelefon() != null) {
                            String formattedNumber = phoneUtil.format(k.getTelefon(), PhoneNumberUtil.PhoneNumberFormat.E164);
                            pstmt.setString(3, formattedNumber);
                        } else {
                            pstmt.setString(3, null);
                        }
                        pstmt.setString(4, k.getEmail() != null ? k.getEmail().getAddress() : null);
                        pstmt.executeUpdate();
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                newContactIds.put(k, rs.getLong(1));
                            }
                        }
                    }
                }

                String insertZdarzenie = "INSERT INTO Zdarzenia (nazwa, opis, data, lat, lng) VALUES (?, ?, ?, ?, ?)";
                Map<Zdarzenie, Long> newEventIds = new HashMap<>();
                try (PreparedStatement pstmt = conn.prepareStatement(insertZdarzenie, Statement.RETURN_GENERATED_KEYS)) {
                    for (Zdarzenie z : zdarzenia) {
                        pstmt.setString(1, z.getNazwa());
                        pstmt.setString(2, z.getOpis());
                        pstmt.setString(3, z.getData().toString());
                        if (z.getLokalizacja() != null && z.getLokalizacja().getWspolrzedne() != null) {
                            pstmt.setDouble(4, z.getLokalizacja().getWspolrzedne().getLatitude());
                            pstmt.setDouble(5, z.getLokalizacja().getWspolrzedne().getLongitude());
                        } else {
                            pstmt.setDouble(4, 0.0);
                            pstmt.setDouble(5, 0.0);
                        }
                        pstmt.executeUpdate();
                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                newEventIds.put(z, rs.getLong(1));
                            }
                        }
                    }
                }

                String insertJunction = "INSERT OR IGNORE INTO Kontakt_Zdarzenie (kontakt_id, zdarzenie_id) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertJunction)) {
                    Set<String> zapisaneRelacje = new HashSet<>();

                    for (Kontakt k : kontakty) {
                        for (Zdarzenie z : k.getZdarzenia()) {
                            zapiszRelacje(pstmt, zapisaneRelacje, newContactIds.get(k), newEventIds.get(z));
                        }
                    }

                    for (Zdarzenie z : zdarzenia) {
                        for (Kontakt k : z.getKontakty()) {
                            zapiszRelacje(pstmt, zapisaneRelacje, newContactIds.get(k), newEventIds.get(z));
                        }
                    }
                }

                conn.commit();

                for (Map.Entry<Kontakt, Long> entry : newContactIds.entrySet()) {
                    entry.getKey().setId(entry.getValue());
                }
                for (Map.Entry<Zdarzenie, Long> entry : newEventIds.entrySet()) {
                    entry.getKey().setId(entry.getValue());
                }

                System.out.println("Dane zapisane do bazy SQLite.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving data to SQLite database:");
            e.printStackTrace();
            return false;
        }
    }

    private static Connection openConnection() throws SQLException {
        zaladujSterownikSqlite();
        zabezpieczPlikBazy();
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private static void przygotujBaze(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Kontakty ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "imie TEXT NOT NULL, "
                    + "nazwisko TEXT NOT NULL, "
                    + "telefon TEXT, "
                    + "email TEXT)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Zdarzenia ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nazwa TEXT NOT NULL, "
                    + "opis TEXT, "
                    + "data TEXT NOT NULL, "
                    + "lat REAL, "
                    + "lng REAL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Kontakt_Zdarzenie ("
                    + "kontakt_id INTEGER NOT NULL, "
                    + "zdarzenie_id INTEGER NOT NULL, "
                    + "PRIMARY KEY (kontakt_id, zdarzenie_id), "
                    + "FOREIGN KEY (kontakt_id) REFERENCES Kontakty(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (zdarzenie_id) REFERENCES Zdarzenia(id) ON DELETE CASCADE)");
        }
    }

    private static void zapiszRelacje(PreparedStatement pstmt, Set<String> zapisaneRelacje,
                                      Long kontaktId, Long zdarzenieId) throws SQLException {
        if (kontaktId == null || zdarzenieId == null) {
            return;
        }

        String klucz = kontaktId + ":" + zdarzenieId;
        if (zapisaneRelacje.add(klucz)) {
            pstmt.setLong(1, kontaktId);
            pstmt.setLong(2, zdarzenieId);
            pstmt.executeUpdate();
        }
    }

    private static void polacz(Kontakt k, Zdarzenie z) {
        if (!k.getZdarzenia().contains(z)) {
            k.getZdarzenia().add(z);
        }
        if (!z.getKontakty().contains(k)) {
            z.getKontakty().add(k);
        }
    }

    private static File zabezpieczPlikBazy() {
        if (!DB_FILE.exists() || DB_FILE.length() == 0 || jestSqlite(DB_FILE)) {
            return null;
        }

        boolean xml = jestXml(DB_FILE);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File backup = new File(DB_FILE.getParentFile(),
                "kalendarz.db.backup-" + timestamp + (xml ? ".xml" : ".bad"));

        try {
            Files.move(DB_FILE.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Plik kalendarz.db nie był bazą SQLite. Przeniesiono do: " + backup.getName());
            return xml ? backup : null;
        } catch (IOException e) {
            System.err.println("Nie udało się zabezpieczyć niepoprawnego pliku kalendarz.db.");
            e.printStackTrace();
            return null;
        }
    }

    private static boolean jestSqlite(File plik) {
        byte[] naglowek = "SQLite format 3\u0000".getBytes(StandardCharsets.US_ASCII);
        byte[] dane = new byte[naglowek.length];
        try (java.io.FileInputStream in = new java.io.FileInputStream(plik)) {
            if (in.read(dane) != naglowek.length) {
                return false;
            }
            for (int i = 0; i < naglowek.length; i++) {
                if (dane[i] != naglowek[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean jestXml(File plik) {
        try {
            byte[] dane = Files.readAllBytes(plik.toPath());
            String poczatek = new String(dane, 0, Math.min(dane.length, 120), StandardCharsets.UTF_8).trim();
            return poczatek.startsWith("<?xml") || poczatek.startsWith("<kalendarz");
        } catch (IOException e) {
            return false;
        }
    }

    private static synchronized void zaladujSterownikSqlite() {
        if (sterownikSqliteZaladowany) {
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            sterownikSqliteZaladowany = true;
            return;
        } catch (ClassNotFoundException e) {
            // Eclipse sometimes runs an old launch classpath; then load jars from lib.
        }

        try {
            File sqliteJar = new File("lib/sqlite-jdbc-3.45.3.0.jar");
            File slf4jApiJar = new File("lib/slf4j-api-2.0.13.jar");
            File slf4jSimpleJar = new File("lib/slf4j-simple-2.0.13.jar");

            URL[] urls = {
                    sqliteJar.toURI().toURL(),
                    slf4jApiJar.toURI().toURL(),
                    slf4jSimpleJar.toURI().toURL()
            };

            URLClassLoader loader = new URLClassLoader(urls, DBManager.class.getClassLoader());
            Class<?> driverClass = Class.forName("org.sqlite.JDBC", true, loader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverAdapter(driver));
            sterownikSqliteZaladowany = true;
        } catch (Exception e) {
            System.err.println("SQLite JDBC Driver not found. Add sqlite-jdbc jar to lib or Eclipse Build Path.");
            e.printStackTrace();
        }
    }

    private static class DriverAdapter implements Driver {
        private final Driver driver;

        DriverAdapter(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}
