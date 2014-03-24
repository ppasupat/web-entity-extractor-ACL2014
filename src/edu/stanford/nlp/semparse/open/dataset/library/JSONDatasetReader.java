package edu.stanford.nlp.semparse.open.dataset.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.dataset.CriteriaExactMatch;
import edu.stanford.nlp.semparse.open.dataset.CriteriaGeneralWeb;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.dataset.ExampleCached;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswer;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerCriteriaMatch;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerInjectiveMatch;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityNearMatch;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntitySubstring;
import edu.stanford.nlp.semparse.open.dataset.library.JSONDataset.JSONDatasetDatum;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Read a dataset from JSON file and create a Dataset instance.
 * 
 * See JSONDataset for file format.
 */
public class JSONDatasetReader {
  public static class Options {
    @Option(gloss = "Fraction of examples to use for training (from the start)") public double trainFrac = 0.8;
    @Option(gloss = "Fraction of examples to use for testing (from the end)") public double testFrac = 0.2;

    @Option public boolean fuzzyStringMatching = true;
    @Option public boolean zeroOneLoss = true;
    @Option public boolean shuffleDataset = false;
    @Option public long shuffleDatasetSeed = 42; 
  }
  public static Options opts = new Options();
  
  public final String family, name;
  
  public JSONDatasetReader(String family, String name) {
      this.family = family;
      this.name = name;
  }
  
  // ============================================================
  // Get Dataset object
  // ============================================================
  
  public Dataset getDataset() throws IOException {
    return getDataset(this.name);
  }
  
  public Dataset getDataset(String name) throws IOException {
    if (name == null)
      throw new RuntimeException("No dataset specified.");
    
    // Separate train and test data: [trainData]@[testData]
    if (name.contains("@")) {
      Dataset dataset = new Dataset();
      String[] parts = name.split("@");
      if (parts.length != 2) {
        LogInfo.fails("train@test syntax needs 2 datasets; got %d", parts.length);
      }
      dataset.addTrainFromDataset(getDataset(parts[0]));
      dataset.addTestFromDataset(getDataset(parts[1]));
      return dataset;
    }
    
    // Combine datasets: [data1]+[data2]
    if (name.contains("+")) {
      Dataset dataset = new Dataset();
      for (String subName : name.split("[+]")) {
        Dataset subDataset = getDataset(subName);
        dataset.addFromDataset(subDataset);
      }
      return dataset;
    }
    
    // Single dataset
    Path path = Paths.get("datasets", family, name + ".json");
    List<Example> examples = Lists.newArrayList();
    try (BufferedReader reader = Files.newBufferedReader(path, Charsets.UTF_8)) {
      LogInfo.begin_track("Reading dataset from %s", path);
      // Read the JSON file
      ObjectMapper mapper = new ObjectMapper();
      JSONDataset jsonDataset = mapper.readValue(reader, JSONDataset.class);
      LogInfo.log(jsonDataset.options);
      boolean firstTime = true;   // Use to log the information only once
      for (JSONDatasetDatum datum : jsonDataset.data) {
        // Create the example
        ExpectedAnswer expectedAnswer;
        if (opts.zeroOneLoss) {
          expectedAnswer = getZeroOneLossExpectedAnswer(datum, jsonDataset, firstTime);
        } else {
          expectedAnswer = getIRExpectedAnswer(datum, jsonDataset, firstTime);
        }
        // Add the created example to the example list.
        if (jsonDataset.options.useHashcode) {
          examples.add(new ExampleCached(datum.query, jsonDataset.options.cacheDirectory,
              datum.hashcode, datum.url, expectedAnswer));
        } else {
          examples.add(new Example(datum.query, expectedAnswer));
        }
        firstTime = false;
      }
      LogInfo.end_track();
    }
    
    // First trainFrac are training, last testFrac are test
    // Note that the examples can overlap and also don't have to cover all the examples.
    int trainEnd = (int)(examples.size() * opts.trainFrac);
    int testStart = (int)(examples.size() * (1 - opts.testFrac));
    
    // Create the data set
    if (opts.shuffleDataset) {
      Collections.shuffle(examples, new Random(opts.shuffleDatasetSeed));
    }
    Dataset dataset = new Dataset();
    for (int i = 0; i < examples.size(); i++) {
      Example ex = examples.get(i);
      if (i < trainEnd)
        dataset.addTrainExample(ex);
      if (i >= testStart)
        dataset.addTestExample(ex);
    }
    return dataset;
  }
  
  // ============================================================
  // Helper Methods
  // ============================================================
  
  private ExpectedAnswer getZeroOneLossExpectedAnswer(JSONDatasetDatum datum, JSONDataset jsonDataset, boolean verbose) {
    if (jsonDataset.options.detailed) {
      // Use criteria matching (0-1 loss / exact match on first, second, and last entities)
      if (verbose) {
        LogInfo.log("Using 0-1 loss (must match first, second, and last entities to get reward = 1)");
      }
      return new ExpectedAnswerCriteriaMatch(new CriteriaGeneralWeb(datum));
    } else {
      // Use exact matching (0-1 loss / exact match on all entities)
      if (verbose)
        LogInfo.log("Using 0-1 loss (must match all entities to get reward = 1)");
      List<TargetEntity> targetEntities = Lists.newArrayList();
      for (String entity : datum.entities)
        targetEntities.add(getTargetEntity(entity));
      return new ExpectedAnswerCriteriaMatch(new CriteriaExactMatch(targetEntities));
    }
  }
  
  private ExpectedAnswer getIRExpectedAnswer(JSONDatasetDatum datum, JSONDataset jsonDataset, boolean verbose) {
    // Use IR score-based matching (e.g. F1 > 80)
    if (verbose)
      LogInfo.logs("Using IR-based loss (must have %s >= %f to get positive reward)",
          ExpectedAnswerInjectiveMatch.opts.irCriterion,
          ExpectedAnswerInjectiveMatch.opts.irThreshold);
    // Convert each entity string to TargetEntity
    List<TargetEntity> targetEntities = Lists.newArrayList();
    for (String entity : datum.entities)
      targetEntities.add(getTargetEntity(entity));
    return new ExpectedAnswerInjectiveMatch(targetEntities);
  }
  
  private TargetEntity getTargetEntity(String entity) {
    return opts.fuzzyStringMatching ? new TargetEntityNearMatch(entity) : new TargetEntitySubstring(entity);
  }
}
