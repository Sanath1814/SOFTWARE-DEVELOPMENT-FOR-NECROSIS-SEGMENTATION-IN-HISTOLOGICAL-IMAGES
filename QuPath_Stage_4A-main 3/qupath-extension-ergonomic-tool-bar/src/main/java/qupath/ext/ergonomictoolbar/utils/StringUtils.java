package qupath.ext.ergonomictoolbar.utils;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Utility class for string manipulation.
 */
public class StringUtils {
  /**
   * Comparator for strings that contain a numeric part.
   */
  public static Comparator<String> numericStringComparator = (s1, s2) -> {
    // Extract the numeric part by removing all non-digit characters except the decimal point
    String num1 = s1.replaceAll("[^\\d.]", "");
    String num2 = s2.replaceAll("[^\\d.]", "");

    // Convert to float for comparison
    float float1 = Float.parseFloat(num1);
    float float2 = Float.parseFloat(num2);

    // Compare the float values
    return Float.compare(float1, float2);
  };

  /**
   * Validate if the input string can be parsed as a positive float.
   *
   * @param input The input string to validate.
   * @return True if the input is a valid positive float, otherwise false.
   */
  public static String formatNumber(String input) {
    try {
      BigDecimal number = new BigDecimal(input);

      // Si le nombre est entier, on le laisse tel quel
      if (number.stripTrailingZeros().scale() <= 0) {
        return (number.intValue() > 0 ? String.valueOf(number.intValue()) : null);
      }

      return (number.floatValue() > 0 ? String.valueOf(number.floatValue()) : null);
    } catch (NumberFormatException | NullPointerException e) {
      return null;
    }
  }

  /**
   * Méthode utilitaire pour vérifier si une chaîne est un nombre (entier ou décimal).
   */
  public static boolean isNumeric(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    try {
      Double.parseDouble(
          str);
      // Remplacement de la virgule par un point pour la compatibilité avec Double.parseDouble
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
