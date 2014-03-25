package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import fig.basic.LogInfo;
import fig.basic.Option;

public class WordVectorTable {
  public static class Options {
    @Option public String wordVectorFilename = null;
    
    @Option(gloss = "vector to use for UNKNOWN words (-1 = don't use any vector)")
    public int wordVectorUNKindex = 0;
  }
  public static Options opts = new Options();
  
  public static Map<String, Integer> wordToIndex;
  public static double[][] wordVectors;
  public static int numWords, numDimensions;
  
  public static void initModels() {
    if (wordVectors != null || opts.wordVectorFilename == null || opts.wordVectorFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.wordVectorFilename);
    LogInfo.logs("Reading word vectors from %s", dataPath);
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charsets.UTF_8)) {
      String[] headerTokens = in.readLine().split(" ");
      numWords = Integer.parseInt(headerTokens[0]);
      numDimensions = Integer.parseInt(headerTokens[1]);
      wordToIndex = Maps.newHashMap();
      wordVectors = new double[numWords][numDimensions];
      for (int i = 0; i < numWords; i++) {
        String[] tokens = in.readLine().split(" ");
        wordToIndex.put(tokens[0], i);
        for (int j = 0; j < numDimensions; j++) {
          wordVectors[i][j] = Double.parseDouble(tokens[j+1]);
        }
      }
      LogInfo.logs("Neural network vectors: %s words; %s dimensions per word", numWords, numDimensions);
    } catch (IOException e) {
      LogInfo.fails("Cannot load neural network vectors from %s", dataPath);
    }
  }
  
  public static double[] getVector(String word) {
    initModels();
    Integer index = wordToIndex.get(word);
    if (index == null) {
      index = opts.wordVectorUNKindex;
      if (index < 0) return null;
    }
    return wordVectors[index];
  }

}
