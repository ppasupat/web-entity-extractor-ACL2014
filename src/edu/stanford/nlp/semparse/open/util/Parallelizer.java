package edu.stanford.nlp.semparse.open.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fig.basic.LogInfo;
import fig.basic.Option;

public class Parallelizer {
  public static class Options {
    @Option(gloss = "Number of threads for execution")
    public int numThreads = 1;
  }
  public static Options opts = new Options();
  
  public static int getNumThreads() {
    int numThreads = Runtime.getRuntime().availableProcessors();
    if (opts.numThreads > 0 && numThreads > opts.numThreads)
      numThreads = opts.numThreads;
    return numThreads;
  }
  
  public static void run(List<Runnable> tasks) {
    LogInfo.begin_threads();
    ExecutorService service = Executors.newFixedThreadPool(getNumThreads());
    try {
      for (Runnable task : tasks) {
        service.submit(task);
      }
      service.shutdown();
      service.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      LogInfo.fail(e);
    }
    LogInfo.end_threads();
  }
  
  public static <T, S extends Callable<T>> List<Future<T>> runAndReturnStuff(List<S> tasks) {
    LogInfo.begin_threads();
    List<Future<T>> results = null;
    ExecutorService service = Executors.newFixedThreadPool(getNumThreads());
    try {
      // Invoke all trainers
      results = service.invokeAll(tasks);
      service.shutdown();
      service.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      LogInfo.fail(e);
    }
    LogInfo.end_threads();
    return results;
  }
  
}
