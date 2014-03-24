package edu.stanford.nlp.semparse.open.core;

import java.util.concurrent.Callable;

import edu.stanford.nlp.semparse.open.dataset.Dataset;

public class ParallelizedTrainer implements Callable<OpenSemanticParser> {
  Dataset dataset;
  boolean beVeryQuiet;

  public ParallelizedTrainer(Dataset dataset, boolean beVeryQuiet) {
    this.dataset = dataset;
    this.beVeryQuiet = beVeryQuiet;
  }

  @Override
  public OpenSemanticParser call() throws Exception {
    OpenSemanticParser parser = new OpenSemanticParser();
    parser.train(dataset, beVeryQuiet);
    return parser;
  }
}