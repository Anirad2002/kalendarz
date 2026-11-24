package app;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class XmlHelper {

    public static void zapiszDoXml(List<Kontakt> kontakty, List<Zdarzenie> zdarzenia, String nazwaPliku) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element root = doc.createElement("kalendarz");
            doc.appendChild(root);

            // Save contacts with their events
            Element elemKontakty = doc.createElement("kontakty");
            root.appendChild(elemKontakty);
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

            for (Kontakt k : kontakty) {
                Element ek = doc.createElement("kontakt");
                ek.setAttribute("id", String.valueOf(k.getId()));

                Element imie = doc.createElement("imie");
                imie.setTextContent(k.getImie());
                ek.appendChild(imie);

                Element nazwisko = doc.createElement("nazwisko");
                nazwisko.setTextContent(k.getNazwisko());
                ek.appendChild(nazwisko);

                Element telefon = doc.createElement("telefon");
                if (k.getTelefon() != null) {
                    telefon.setTextContent(phoneUtil.format(k.getTelefon(), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
                } else {
                    telefon.setTextContent("");
                }
                ek.appendChild(telefon);

                Element email = doc.createElement("email");
                if (k.getEmail() != null) {
                    email.setTextContent(k.getEmail().getAddress());
                } else {
                    email.setTextContent("");
                }
                ek.appendChild(email);

                // List of event IDs (relationships)
                Element eventsElem = doc.createElement("zdarzeniaIds");
                for (Zdarzenie z : k.getZdarzenia()) {
                    Element eventIdElem = doc.createElement("zdarzenieId");
                    eventIdElem.setTextContent(String.valueOf(z.getId()));
                    eventsElem.appendChild(eventIdElem);
                }
                ek.appendChild(eventsElem);

                elemKontakty.appendChild(ek);
            }

            // Save events with their contacts
            Element elemZdarzenia = doc.createElement("zdarzenia");
            root.appendChild(elemZdarzenia);
            for (Zdarzenie z : zdarzenia) {
                Element ez = doc.createElement("zdarzenie");
                ez.setAttribute("id", String.valueOf(z.getId()));

                Element nazwa = doc.createElement("nazwa");
                nazwa.setTextContent(z.getNazwa());
                ez.appendChild(nazwa);

                Element opis = doc.createElement("opis");
                opis.setTextContent(z.getOpis());
                ez.appendChild(opis);

                Element data = doc.createElement("data");
                data.setTextContent(z.getData().toString());
                ez.appendChild(data);

                Element lat = doc.createElement("lat");
                if (z.getLokalizacja() != null && z.getLokalizacja().getWspolrzedne() != null) {
                    lat.setTextContent(String.valueOf(z.getLokalizacja().getWspolrzedne().getLatitude()));
                } else {
                    lat.setTextContent("0");
                }
                ez.appendChild(lat);

                Element lng = doc.createElement("lng");
                if (z.getLokalizacja() != null && z.getLokalizacja().getWspolrzedne() != null) {
                    lng.setTextContent(String.valueOf(z.getLokalizacja().getWspolrzedne().getLongitude()));
                } else {
                    lng.setTextContent("0");
                }
                ez.appendChild(lng);

                // List of contact IDs
                Element contactsElem = doc.createElement("kontaktyIds");
                for (Kontakt k : z.getKontakty()) {
                    Element contactIdElem = doc.createElement("kontaktId");
                    contactIdElem.setTextContent(String.valueOf(k.getId()));
                    contactsElem.appendChild(contactIdElem);
                }
                ez.appendChild(contactsElem);

                elemZdarzenia.appendChild(ez);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(nazwaPliku));
            transformer.transform(source, result);

            System.out.println("\nZapisano do pliku XML: " + nazwaPliku);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void odczytajZXml(String nazwaPliku) {
        List<Kontakt> kontakty = new ArrayList<>();
        List<Zdarzenie> zdarzenia = new ArrayList<>();
        if (!odczytajZXmlDoList(nazwaPliku, kontakty, zdarzenia)) {
            return;
        }

        System.out.println("-- Odczytane kontakty --");
        for (Kontakt k : kontakty) {
            System.out.println(k);
        }
        System.out.println("-- Odczytane zdarzenia --");
        for (Zdarzenie z : zdarzenia) {
            System.out.println(z);
        }
    }

    public static boolean odczytajZXmlDoList(String nazwaPliku, List<Kontakt> kontakty, List<Zdarzenie> zdarzenia) {
        try {
            kontakty.clear();
            zdarzenia.clear();

            File plik = new File(nazwaPliku);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(plik);
            doc.getDocumentElement().normalize();

            // Maps to reconstruct relationships
            Map<Long, Kontakt> kontaktMap = new LinkedHashMap<>();
            Map<Long, Zdarzenie> zdarzenieMap = new LinkedHashMap<>();

            // Read contacts
            NodeList listaKontaktow = doc.getElementsByTagName("kontakt");
            for (int i = 0; i < listaKontaktow.getLength(); i++) {
                Element ek = (Element) listaKontaktow.item(i);
                long id = Long.parseLong(ek.getAttribute("id"));
                String imie = ek.getElementsByTagName("imie").item(0).getTextContent();
                String nazwisko = ek.getElementsByTagName("nazwisko").item(0).getTextContent();
                String telefon = ek.getElementsByTagName("telefon").item(0).getTextContent();
                String email = ek.getElementsByTagName("email").item(0).getTextContent();
                Kontakt k = new Kontakt(id, imie, nazwisko, telefon, email);
                kontaktMap.put(id, k);
                kontakty.add(k);
            }

            // Read events
            NodeList listaZdarzen = doc.getElementsByTagName("zdarzenie");
            for (int i = 0; i < listaZdarzen.getLength(); i++) {
                Element ez = (Element) listaZdarzen.item(i);
                long id = Long.parseLong(ez.getAttribute("id"));
                String nazwa = ez.getElementsByTagName("nazwa").item(0).getTextContent();
                String opis = ez.getElementsByTagName("opis").item(0).getTextContent();
                String dataStr = ez.getElementsByTagName("data").item(0).getTextContent();
                String latStr = ez.getElementsByTagName("lat").item(0).getTextContent();
                String lngStr = ez.getElementsByTagName("lng").item(0).getTextContent();

                LocalDateTime data = LocalDateTime.parse(dataStr);
                double lat = Double.parseDouble(latStr);
                double lng = Double.parseDouble(lngStr);
                Lokalizacja lok = new Lokalizacja(lat + ",0,0", lng + ",0,0");

                Zdarzenie z = new Zdarzenie(id, nazwa, opis, data, lok);
                zdarzenieMap.put(id, z);
                zdarzenia.add(z);
            }

            // Establish relationships from contact side
            for (int i = 0; i < listaKontaktow.getLength(); i++) {
                Element ek = (Element) listaKontaktow.item(i);
                long kontId = Long.parseLong(ek.getAttribute("id"));
                Kontakt k = kontaktMap.get(kontId);
                NodeList eventIds = ek.getElementsByTagName("zdarzenieId");
                for (int j = 0; j < eventIds.getLength(); j++) {
                    long eventId = Long.parseLong(eventIds.item(j).getTextContent());
                    Zdarzenie z = zdarzenieMap.get(eventId);
                    if (z != null && !k.getZdarzenia().contains(z)) {
                        k.getZdarzenia().add(z);
                        z.getKontakty().add(k);
                    }
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
