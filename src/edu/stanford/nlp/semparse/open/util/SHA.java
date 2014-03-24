package edu.stanford.nlp.semparse.open.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class SHA {
  public static String toSHA1(String input) {
    return Hashing.sha1().hashString(input, Charsets.UTF_8).toString();
  }
}
