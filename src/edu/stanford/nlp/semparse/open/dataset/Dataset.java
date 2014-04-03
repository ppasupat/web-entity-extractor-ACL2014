package edu.stanford.nlp.semparse.open.dataset;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityPersonName;

/**
 * A Dataset represents a data set, which has multiple Examples (data instances).
 * 
 * The examples are divided into training and test.
 */
public class Dataset {
  public final List<Example> trainExamples = Lists.newArrayList();
  public final List<Example> testExamples = Lists.newArrayList();
  
  public Dataset() {
    // Do nothing
  }
  
  public Dataset(List<Example> train, List<Example> test) {
    trainExamples.addAll(train);
    testExamples.addAll(test);
  }

  public Dataset addTrainExample(Example ex) {
    trainExamples.add(ex);
    return this;
  }
  
  public Dataset addTestExample(Example ex) {
    testExamples.add(ex);
    return this;
  }
  
  public Dataset addFromDataset(Dataset that) {
    this.trainExamples.addAll(that.trainExamples);
    this.testExamples.addAll(that.testExamples);
    return this;
  }
  
  public Dataset addTrainFromDataset(Dataset that) {
    this.trainExamples.addAll(that.trainExamples);
    this.trainExamples.addAll(that.testExamples);
    return this;
  }
  
  public Dataset addTestFromDataset(Dataset that) {
    this.testExamples.addAll(that.trainExamples);
    this.testExamples.addAll(that.testExamples);
    return this;
  }
  
  /**
   * @return a new Dataset with the Examples shuffled up.
   * The train/test ratio remain the same.
   * The original Dataset is not modified.
   */
  public Dataset getNewShuffledDataset() {
    List<Example> allExamples = Lists.newArrayList(trainExamples);
    allExamples.addAll(testExamples);
    Collections.shuffle(allExamples, new Random(42));
    List<Example> newTrain = allExamples.subList(0, trainExamples.size());
    List<Example> newTest = allExamples.subList(trainExamples.size(), allExamples.size());
    return new Dataset(newTrain, newTest);
  }
  
  /**
   * @return a new Dataset with the specified train/test ratio.
   */
  public Dataset getNewSplitDataset(double trainRatio) {
    List<Example> allExamples = Lists.newArrayList(trainExamples);
    allExamples.addAll(testExamples);
    Collections.shuffle(allExamples, new Random(42));
    int trainEndIndex = (int) (allExamples.size() * trainRatio);
    List<Example> newTrain = allExamples.subList(0, trainEndIndex);
    List<Example> newTest = allExamples.subList(trainEndIndex, allExamples.size());
    return new Dataset(newTrain, newTest);
  }
  
  // ============================================================
  // Shorthands for creating datasets.
  // ============================================================
  
  public Example E(String phrase, ExpectedAnswer expectedAnswer) {
    return E(phrase, expectedAnswer, true);
  }
  
  public Example E(String phrase, ExpectedAnswer expectedAnswer, boolean isTrain) {
    Example ex = new Example(phrase, expectedAnswer);
    if (isTrain)
      addTrainExample(ex);
    else
      addTestExample(ex);
    return ex;
  }
  
  public ExpectedAnswer L(String... items) {
    return L(false, items);
  }
  
  public ExpectedAnswer L(boolean exact, String... items) {
    return new ExpectedAnswerInjectiveMatch(items);
  }
  
  public ExpectedAnswer LN(String... items) {
    return LN(false, items);
  }
  
  public ExpectedAnswer LN(boolean exact, String... items) {
    TargetEntity[] targetEntities = new TargetEntity[items.length];
    for (int i = 0; i < items.length; i++) targetEntities[i] = N(items[i]);
    return new ExpectedAnswerInjectiveMatch(items);
  }
  
  public TargetEntityPersonName N(String full) {
    String[] parts = full.split(" ");
    if (parts.length == 2)
      return new TargetEntityPersonName(parts[0], parts[1]);
    else if (parts.length == 3)
      return new TargetEntityPersonName(parts[0], parts[1], parts[2]);
    throw new RuntimeException("N(...) requires two or three words.");
  }

}
