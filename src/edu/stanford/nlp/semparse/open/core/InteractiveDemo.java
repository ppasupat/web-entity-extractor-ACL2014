package edu.stanford.nlp.semparse.open.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.stanford.nlp.semparse.open.core.eval.CandidateStatistics;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.LogInfo;

public class InteractiveDemo {
  
  public final OpenSemanticParser parser;
  
  public InteractiveDemo(OpenSemanticParser parser) {
    this.parser = parser;
  }

  public void run() {
    LogInfo.log("Starting interactive mode ...");
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        System.out.println("============================================================");
        System.out.print("Query: list of ");
        String phrase = in.readLine();
        if (phrase == null) {System.out.println(); break;}
        if (phrase.isEmpty()) continue;
        System.out.print("Web Page URL (blank for Google Search): ");
        String url = in.readLine();
        if (url == null) {System.out.println(); break;}
        CandidateStatistics pred = url.isEmpty() ? parser.predict(phrase) : parser.predict(phrase, url);
        LogInfo.begin_track("PRED (top scoring candidate):");
        if (pred == null) {
          LogInfo.log("Rank 1 [Unique Rank 1]: NO CANDIDATE FOUND!");
        } else {
          LogInfo.logs("Rank 1 [Unique Rank 1]: (Total Feature Score = %s)", pred.score);
          Candidate candidate = pred.candidate;
          LogInfo.logs("Extraction Predicate: %s", candidate.pattern);
          LogInfo.log(candidate.sampleEntities());
        }
        LogInfo.end_track();
      }
    } catch (IOException e) {
      LogInfo.fail(e);
    }
  }
  
}
