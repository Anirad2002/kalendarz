package app;

import com.javadocmd.simplelatlng.LatLng;

public class Lokalizacja {

	private LatLng wspolrzedne;

	public Lokalizacja(String szerokosc, String dlugosc) {
		double lat = stopnieNaDziesietne(szerokosc);
		double lng = stopnieNaDziesietne(dlugosc);
		this.wspolrzedne = new LatLng(lat, lng);
	}

	private double stopnieNaDziesietne(String wspolrzedna) {
		String[] czesci = wspolrzedna.split(",");
		double stopnie = Double.parseDouble(czesci[0]);
		double minuty = Double.parseDouble(czesci[1]);
		double sekundy = Double.parseDouble(czesci[2]);
		return stopnie + minuty / 60 + sekundy / 3600;
	}

	public LatLng getWspolrzedne() {
		return wspolrzedne;
	}

	public void setWspolrzedne(String szerokosc, String dlugosc) {
		double lat = stopnieNaDziesietne(szerokosc);
		double lng = stopnieNaDziesietne(dlugosc);
		this.wspolrzedne = new LatLng(lat, lng);
	}

	public String toString() {
		return "lokalizacja: " + wspolrzedne.toString();
	}
}