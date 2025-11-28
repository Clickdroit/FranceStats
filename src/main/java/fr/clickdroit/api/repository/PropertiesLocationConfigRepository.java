package fr.clickdroit.api.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Properties file-based implementation of LocationConfigRepository.
 */
public class PropertiesLocationConfigRepository implements LocationConfigRepository {

    private static final String CONFIG_FILE = "station_config.properties";

    @Override
    public double[] loadLocation() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(fis);

            String latStr = props.getProperty("latitude");
            String lonStr = props.getProperty("longitude");

            if (latStr != null && lonStr != null) {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture config : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveLocation(double[] location) {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("latitude", String.valueOf(location[0]));
            props.setProperty("longitude", String.valueOf(location[1]));
            props.setProperty("saved_date", new Date().toString());
            props.store(fos, "Configuration Station Essence");
            System.out.println("✅ Position sauvegardée !");
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }
}
