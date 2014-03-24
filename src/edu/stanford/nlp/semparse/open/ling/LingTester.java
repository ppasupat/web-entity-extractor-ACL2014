package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LingTester {

  public static void main(String args[]) throws IOException {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        System.out.println("Please enter a sentence:");
        String input = in.readLine();
        LingData data = LingData.get(input);
        System.out.println(data.tokens);
        System.out.println(data.posTags);
        System.out.println(data.nerTags);
        System.out.println(data.posTypes);
        System.out.println(data.nerValues);
        System.out.println(data.lemmaTokens);
      }
    }
  }
  
}
