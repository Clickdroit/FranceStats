package fr.clickdroit.api.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classe utilitaire pour la validation des données d'entrée
 */
public class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    private static final Validator validator;

    // Patterns de validation
    private static final Pattern CODE_POSTAL_PATTERN = Pattern.compile("^[0-9]{2,5}$");
    private static final Pattern DEPARTEMENT_PATTERN = Pattern.compile("^[0-9]{2,3}[AB]?$");
    private static final Pattern CARBURANT_PATTERN = Pattern.compile("^[A-Za-z0-9\\s-]+$");

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Valide un objet avec les annotations Bean Validation
     */
    public static <T> boolean isValid(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            logger.warn("Validation échouée pour {}: {}",
                    object.getClass().getSimpleName(),
                    violations.iterator().next().getMessage());
            return false;
        }
        return true;
    }

    /**
     * Valide un objet et retourne les erreurs
     */
    public static <T> Set<ConstraintViolation<T>> validate(T object) {
        return validator.validate(object);
    }

    /**
     * Valide les coordonnées GPS
     */
    public static boolean isValidCoordinates(double latitude, double longitude) {
        boolean valid = latitude >= -90.0 && latitude <= 90.0 &&
                longitude >= -180.0 && longitude <= 180.0;

        if (!valid) {
            logger.warn("Coordonnées invalides: lat={}, lon={}", latitude, longitude);
        }

        return valid;
    }

    /**
     * Valide les coordonnées françaises (incluant DOM-TOM)
     */
    public static boolean isValidFrenchCoordinates(double latitude, double longitude) {
        boolean valid = latitude >= -22.0 && latitude <= 52.0 &&
                longitude >= -63.0 && longitude <= 56.0;

        if (!valid) {
            logger.debug("Coordonnées hors zone française: lat={}, lon={}", latitude, longitude);
        }

        return valid;
    }

    /**
     * Valide un code postal
     */
    public static boolean isValidCodePostal(String codePostal) {
        if (codePostal == null || codePostal.trim().isEmpty()) {
            logger.warn("Code postal vide ou null");
            return false;
        }

        boolean valid = CODE_POSTAL_PATTERN.matcher(codePostal.trim()).matches();
        if (!valid) {
            logger.warn("Code postal invalide: {}", codePostal);
        }

        return valid;
    }

    /**
     * Valide un numéro de département
     */
    public static boolean isValidDepartement(String departement) {
        if (departement == null || departement.trim().isEmpty()) {
            logger.warn("Département vide ou null");
            return false;
        }

        boolean valid = DEPARTEMENT_PATTERN.matcher(departement.trim().toUpperCase()).matches();
        if (!valid) {
            logger.warn("Département invalide: {}", departement);
        }

        return valid;
    }

    /**
     * Valide un nom de carburant
     */
    public static boolean isValidCarburant(String carburant) {
        if (carburant == null || carburant.trim().isEmpty()) {
            logger.warn("Carburant vide ou null");
            return false;
        }

        boolean valid = CARBURANT_PATTERN.matcher(carburant.trim()).matches() &&
                carburant.trim().length() <= 50;
        if (!valid) {
            logger.warn("Carburant invalide: {}", carburant);
        }

        return valid;
    }

    /**
     * Valide un prix de carburant
     */
    public static boolean isValidPrix(double prix) {
        boolean valid = prix > 0.0 && prix < 10.0; // Prix réaliste entre 0 et 10€/L
        if (!valid) {
            logger.warn("Prix invalide: {}", prix);
        }

        return valid;
    }

    /**
     * Valide un rayon de recherche
     */
    public static boolean isValidRayon(double rayon) {
        boolean valid = rayon > 0.0 && rayon <= 1000.0; // Rayon max 1000km
        if (!valid) {
            logger.warn("Rayon invalide: {}", rayon);
        }

        return valid;
    }

    /**
     * Sanitise une chaîne de caractères
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("[<>\"'&]", "")  // Caractères potentiellement dangereux
                .replaceAll("\\s+", " ");    // Multiples espaces
    }

    /**
     * Valide une URL
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new java.net.URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            logger.warn("URL invalide: {}", url);
            return false;
        }
    }

    /**
     * Valide un nom de ville
     */
    public static boolean isValidVille(String ville) {
        if (ville == null || ville.trim().isEmpty()) {
            return false;
        }

        String cleaned = ville.trim();
        boolean valid = cleaned.length() >= 2 &&
                cleaned.length() <= 100 &&
                cleaned.matches("^[a-zA-ZÀ-ÿ\\s'-]+$");

        if (!valid) {
            logger.warn("Nom de ville invalide: {}", ville);
        }

        return valid;
    }
}