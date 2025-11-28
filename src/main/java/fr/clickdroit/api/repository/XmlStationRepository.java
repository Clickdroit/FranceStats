package fr.clickdroit.api.repository;

import fr.clickdroit.api.models.Station;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * XML-based implementation of StationRepository.
 * Loads station data from a remote XML file and caches it locally.
 */
public class XmlStationRepository implements StationRepository {

    private static final String XML_URL = "https://donnees.roulez-eco.fr/opendata/instantane";
    private static final String CACHE_FILE = "prix_carburants_cache.xml";
    private static final int CACHE_DURATION_HOURS = 6;
    private static final int HTTP_TIMEOUT_MS = 30000;

    @Override
    public List<Station> findAll() throws DataAccessException {
        try {
            System.out.println("📂 Chargement des données des stations...");

            File xmlFile = new File(CACHE_FILE);

            boolean needDownload = !xmlFile.exists() ||
                    (System.currentTimeMillis() - xmlFile.lastModified()) > CACHE_DURATION_HOURS * 60 * 60 * 1000;

            if (needDownload || !isValidXMLFile(xmlFile)) {
                System.out.println("🌐 Téléchargement des données depuis internet...");
                if (!downloadDataFromInternet()) {
                    System.err.println("❌ Échec du téléchargement. Tentative avec fichier cache...");
                    if (!xmlFile.exists() || !isValidXMLFile(xmlFile)) {
                        throw new DataAccessException("Aucun fichier XML valide disponible.");
                    }
                }
            } else {
                System.out.println("📁 Utilisation du cache local (récent)");
            }

            return parseXMLFile(xmlFile);

        } catch (Exception e) {
            throw new DataAccessException("Erreur lors du chargement des données", e);
        }
    }

    @Override
    public void refreshData() {
        File cacheFile = new File(CACHE_FILE);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    private boolean downloadDataFromInternet() {
        try {
            URL url = new URL(XML_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("User-Agent", "PrixEssenceApp/1.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ Erreur HTTP : " + responseCode);
                return false;
            }

            try (ZipInputStream zipIn = new ZipInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(CACHE_FILE)) {

                ZipEntry entry = zipIn.getNextEntry();
                if (entry != null && entry.getName().endsWith(".xml")) {
                    System.out.println("📦 Extraction du fichier : " + entry.getName());

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;

                        if (totalBytes % (1024 * 1024) == 0) {
                            System.out.printf("\r📥 Extraction... %.1f MB", totalBytes / 1024.0 / 1024.0);
                        }
                    }
                    System.out.println("\n✅ Extraction terminée !");
                    zipIn.closeEntry();
                    return true;
                } else {
                    System.err.println("❌ Aucun fichier XML trouvé dans l'archive");
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement : " + e.getMessage());
            return false;
        }
    }

    private List<Station> parseXMLFile(File xmlFile) throws DataAccessException {
        List<Station> stations = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList stationNodes = doc.getElementsByTagName("pdv");
            int stationsIgnorees = 0;

            System.out.println("🔄 Analyse du fichier XML...");

            for (int i = 0; i < stationNodes.getLength(); i++) {
                if (i % 1000 == 0) {
                    System.out.printf("\r⚙️  Traitement... %d/%d stations", i, stationNodes.getLength());
                }

                Element stationElement = (Element) stationNodes.item(i);

                try {
                    Station station = parseStationElement(stationElement);
                    if (station != null && !station.getPrix().isEmpty()) {
                        stations.add(station);
                    } else {
                        stationsIgnorees++;
                    }
                } catch (Exception e) {
                    stationsIgnorees++;
                }
            }

            System.out.printf("\n✅ %d stations chargées (%d ignorées)%n", stations.size(), stationsIgnorees);
            return stations;

        } catch (Exception e) {
            throw new DataAccessException("Erreur lors du parsing XML", e);
        }
    }

    private Station parseStationElement(Element stationElement) {
        String cp = stationElement.getAttribute("cp");
        String ville = stationElement.getAttribute("ville");
        String adresse = stationElement.getAttribute("adresse");

        String latStr = stationElement.getAttribute("latitude");
        String lonStr = stationElement.getAttribute("longitude");

        if (latStr.isEmpty() || lonStr.isEmpty()) {
            return null;
        }

        double lat = Double.parseDouble(latStr) / 100000.0;
        double lon = Double.parseDouble(lonStr) / 100000.0;

        if (lat < -22.0 || lat > 52.0 || lon < -63.0 || lon > 56.0) {
            return null;
        }

        Station station = new Station(ville, cp, lat, lon, adresse);

        NodeList prixList = stationElement.getElementsByTagName("prix");
        for (int j = 0; j < prixList.getLength(); j++) {
            Element prix = (Element) prixList.item(j);
            String carburant = prix.getAttribute("nom");
            String valeur = prix.getAttribute("valeur");

            if (!valeur.isEmpty()) {
                try {
                    double prixValue = Double.parseDouble(valeur.replace(',', '.'));
                    if (prixValue > 0 && prixValue < 10) {
                        station.ajouterPrix(carburant, prixValue);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return station;
    }

    private boolean isValidXMLFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) return false;

            firstLine = firstLine.trim();
            return firstLine.startsWith("<?xml") || firstLine.startsWith("<pdv_liste");
        } catch (Exception e) {
            return false;
        }
    }
}
