package edu.stanford.nlp.semparse.open.util;

public class EditDistance {
  
  private static int min3(int x1, int x2, int x3) {
    return Math.min(Math.min(x1, x2), x3);
  }

  /**
   * Return true if |a| and |b| are within Levenshtein edit distance |limit|.
   */
  public static boolean withinEditDistance(String a, String b, int limit) {
    if (a == null || b == null || Math.abs(a.length() - b.length()) > limit) return false;
    //  memory array:     0      limit-1 limit limit+1   2*limit
    // actual column: (i-limit) .. (i-1)   i   (i+1) .. (i+limit)
    int[] memory = new int[2 * limit + 1];
    int INFINITY = limit + 1;
    // First row
    for (int j = 0; j < limit; j++) memory[j] = INFINITY;
    for (int j = 0; j <= limit; j++) memory[limit + j] = j;
    // Consequent rows
    for (int i = 0; i < a.length(); i++) {
      int[] newMemory = new int[2 * limit + 1];
      for (int j = 0; j <= 2 * limit; j++) {
        int actualJ = i + j - limit;
        if (actualJ < 0 || actualJ >= b.length()) {
          newMemory[j] = INFINITY;
        } else {
          newMemory[j] = min3(
              (j-1 < 0       ? INFINITY : newMemory[j-1] + 1),
              (j+1 > 2*limit ? INFINITY : memory[j+1] + 1), 
              memory[j] + (a.charAt(i) == b.charAt(actualJ) ? 0 : 1));
        }
      }
      memory = newMemory;
    }
    return memory[limit + (b.length() - a.length())] <= limit;
  }
  
  public static void main(String args[]) {
    System.out.println(withinEditDistance("this", "this", 0));
    System.out.println(withinEditDistance("this", "this", 3));
    System.out.println(withinEditDistance("this", "that", 2));
    System.out.println(withinEditDistance("this", "these", 2));
    System.out.println(withinEditDistance("why?", "who!", 1));
    System.out.println(withinEditDistance("This is a book", "That is a Book", 3));
    System.out.println(withinEditDistance("This is a book", "That is a Books", 4));
    System.out.println(withinEditDistance("hello", "HELLO", 3));
    System.out.println(withinEditDistance("hello", "\"hello\"", 2));
    System.out.println(withinEditDistance("cafe", "Café", 2));
  }
  
  
}
