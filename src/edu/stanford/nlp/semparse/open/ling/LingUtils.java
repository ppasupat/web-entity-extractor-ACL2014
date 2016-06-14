package edu.stanford.nlp.semparse.open.ling;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fig.basic.LogInfo;

public class LingUtils {
  
  // Compute an abstraction of a string
  public static String computePhraseShape(String x) {
    StringBuilder buf = new StringBuilder();
    char lastc = 0;
    for (int i = 0; i < x.length(); i++) {
      char c = x.charAt(i);
      if (Character.isDigit(c)) c = '0';
      else if (Character.isLetter(c)) c = Character.isLowerCase(c) ? 'a' : 'A';
      else if (Character.isWhitespace(c) || Character.isSpaceChar(c)) c = ' ';
      if (c != lastc) buf.append(c);
      lastc = c;
    }
    return buf.toString();
  }
  
  /** Collapse consecutive duplicated tokens */
  public static String collapse(String x) {
    return collapse(x.split(" "));
  }
  
  /** Collapse consecutive duplicated tokens */
  public static String collapse(String[] x) {
    StringBuilder sb = new StringBuilder();
    String lastToken = "";
    for (String token : x) {
      if (!lastToken.equals(token)) {
        sb.append(token).append(" ");
        lastToken = token;
      }
    }
    return sb.toString().trim();
  }
  
  /** Collapse consecutive duplicated tokens */
  public static String collapse(List<String> x) {
    StringBuilder sb = new StringBuilder();
    String lastToken = "";
    for (String token : x) {
      if (!lastToken.equals(token)) {
        sb.append(token).append(" ");
        lastToken = token;
      }
    }
    return sb.toString().trim();
  }
  
  /** Join into string */
  public static String join(String[] x) {
    StringBuilder sb = new StringBuilder();
    for (String token : x) {
      sb.append(token).append(" ");
    }
    return sb.toString().trim();
  }
  
  /** Join into string */
  public static String join(List<String> x) {
    StringBuilder sb = new StringBuilder();
    for (String token : x) {
      sb.append(token).append(" ");
    }
    return sb.toString().trim();
  }

  public static final Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9]+");
  
  public static Set<String> getBagOfWords(String string) {
    Set<String> answer = new HashSet<>();
    Matcher matcher = ALPHANUMERIC.matcher(string.replaceAll("[0-9]+", "0"));
    while (matcher.find()) {
      answer.add(matcher.group());
    }
    return answer;
  }
  
  public static final Pattern ALPHA_OR_NUMERIC = Pattern.compile("[a-z]+|[0-9]+");
  
  public static List<String> getAlphaOrNumericTokens(String string) {
    List<String> answer = new ArrayList<>();
    Matcher matcher = ALPHA_OR_NUMERIC.matcher(string.toLowerCase());
    while (matcher.find()) {
      answer.add(matcher.group());
    }
    return answer;
  }
  
  public static String whitespaceNormalize(String x) {
    return x.replaceAll("\\s+", " ").trim();
  }
  
  /**
   * Simple normalization. (Include whitespace normalization)
   */
  public static String simpleNormalize(String string) {
    // Remove diacritics
    string = Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
    // Special symbols
    string = string
        .replaceAll("‚", ",")
        .replaceAll("„", ",,")
        .replaceAll("·", ".")
        .replaceAll("…", "...")
        .replaceAll("ˆ", "^")
        .replaceAll("˜", "~")
        .replaceAll("‹", "<")
        .replaceAll("›", ">")
        .replaceAll("[‘’´`]", "'")
        .replaceAll("[“”«»]", "\"")
        .replaceAll("[•†‡]", "")
        .replaceAll("[‐‑–—]", "-")
        .replaceAll("[\\u2E00-\\uFFFF]", "");     // Remove all Han characters
    // Citation
    string = string.replaceAll("\\[(nb ?)?\\d+\\]", "");
    string = string.replaceAll("\\*+$", "");
    // Year in parentheses
    string = string.replaceAll("\\(\\d* ?-? ?\\d*\\)", "");
    // Outside Quote
    string = string.replaceAll("^\"(.*)\"$", "$1");
    // Numbering
    if (!string.matches("^[0-9.]+$"))
      string = string.replaceAll("^\\d+\\.", "");
    return string.replaceAll("\\s+", " ").trim();
  }
  
  /**
   * More aggressive normalization. (Include simple and whitespace normalization)
   */
  public static String aggressiveNormalize(String string) {
    // Dashed / Parenthesized information
    string = simpleNormalize(string);
    string = string.trim().replaceAll("\\[[^\\]]*\\]", "");
    string = string.trim().replaceAll("[\\u007F-\\uFFFF]", "");
    string = string.trim().replaceAll(" - .*$", "");
    string = string.trim().replaceAll("\\([^)]*\\)$", "");
    return string.replaceAll("\\s+", " ").trim();
  }
  
  /**
   * Normalize text depending on the level.
   * - <= 0 : no normalization
   * - 1 : strip whitespace
   * - 2 : simple
   * - >= 3 : aggressive
   */
  public static String normalize(String string, int level) {
    if (level == 1) return whitespaceNormalize(string);
    if (level == 2) return simpleNormalize(string);
    if (level >= 3) return aggressiveNormalize(string);
    return string;
  }
  
  /**
   * Find the head word of the phrase (lemmatized).
   * The algorithm is approximate, so the answer may be incorrect
   * (especially when there are proper names with prepositions)
   * 
   * Algorithm:
   * - If there is a preposition, wh-word, or "that", return the last noun preceding it.
   * - Otherwise, return the last non-number token.
   */
  public static String findHeadWord(String phrase, boolean lemmatized) {
    LingData lingData = LingData.get(phrase);
    int index = findHeadWordIndex(phrase);
    if (index >= 0)
      return lemmatized ? lingData.lemmaTokens.get(index) : lingData.tokens.get(index);
    return "";
  }
  
  public static String findHeadWord(String phrase) {
    return findHeadWord(phrase, true);
  }
  
  public static int findHeadWordIndex(String phrase) {
    phrase = hackPhrase(phrase);
    LingData lingData = LingData.get(phrase);
    int modifierIndex = -1;
    for (int i = 1; i < lingData.length; i++) {
      String posTag = lingData.posTags.get(i);
      if ("IN".equals(posTag) || "WP".equals(posTag) || "WDT".equals(posTag) || "TO".equals(posTag)) {
        modifierIndex = i;
        break;
      }
    }
    if (modifierIndex > 0 && "O".equals(lingData.nerTags.get(modifierIndex - 1))) {
      for (int i = modifierIndex - 1; i >= 0; i--) {
        if (lingData.posTags.get(i).charAt(0) == 'N') return i;
      }
      return modifierIndex - 1;
    } else {
      // Find the last non-digit
      for (int i = lingData.length - 1; i >= 0; i--) {
        if (!"CD".equals(lingData.posTags.get(i)) && !"DATE".equals(lingData.nerTags.get(i))) {
          return i;
        }
      }
      return lingData.length - 1;
    }
  }
  
  private static String hackPhrase(String phrase) {
    phrase = phrase.replaceAll(" wiki(pedia)?$", "");
    phrase = phrase.replaceAll("^(type|name) of ", "");
    phrase = phrase.replaceAll(" (episodes?) (season \\d+)$", " $1 of $2");
    return phrase;
  }
  
  /**
   * Find the last word (lemmatized).
   */
  public static String findLastWord(String phrase, boolean lemmatized) {
    LingData lingData = LingData.get(phrase);
    int index = findLastWordIndex(phrase);
    if (index >= 0)
      return lemmatized ? lingData.lemmaTokens.get(index) : lingData.tokens.get(index);
    return "";
  }
  
  public static String findLastWord(String phrase) {
    return findLastWord(phrase, true);
  }
  
  public static int findLastWordIndex(String phrase) {
    return LingData.get(phrase).length - 1;
  }
  
  /**
   * Find the last noun (lemmatized).
   */
  public static String findLastNoun(String phrase, boolean lemmatized) {
    LingData lingData = LingData.get(phrase);
    int index = findLastNounIndex(phrase);
    if (index >= 0)
      return lemmatized ? lingData.lemmaTokens.get(index) : lingData.tokens.get(index);
    return "";
  }
  
  public static String findLastNoun(String phrase) {
    return findLastNoun(phrase, true);
  }
  
  public static int findLastNounIndex(String phrase) {
    LingData lingData = LingData.get(phrase);
    for (int i = lingData.length - 1; i >= 0 ; i--) {
      if (lingData.posTags.get(i).charAt(0) == 'N')
        return i;
    }
    return -1;
  }
  
  public static void main(String[] args) {
    // Test
    LogInfo.log(simpleNormalize("This  is  a  book†[a][1]"));
    LogInfo.log(aggressiveNormalize("This  is  a  book†[a][1]"));
    LogInfo.log(simpleNormalize("Apollo 11 (1969) 「阿波罗」"));
    LogInfo.log(simpleNormalize("\"Apollo 11 (1969)\""));
    LogInfo.log(simpleNormalize("“Erdős café – ε’s delight”"));
    LogInfo.log(aggressiveNormalize("“Erdős café – ε’s delight”"));
    LogInfo.log(simpleNormalize("1. 3.14 is Pi"));
    LogInfo.log(simpleNormalize("3.14"));
    LogInfo.log(simpleNormalize("314"));
    //LogInfo.log(findHeadWord("the mentalist episodes season 2"));
  }
  
}
