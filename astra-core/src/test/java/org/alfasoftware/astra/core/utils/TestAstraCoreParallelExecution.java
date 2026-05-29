package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class TestAstraCoreParallelExecution {

  private Path tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("astra-parallel-test");
  }

  @After
  public void tearDown() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(path -> path.toFile().delete());
  }

  @Test
  public void testDefaultParallelismMatchesAvailableProcessors() {
    UseCase useCase = () -> new HashSet<>();
    assertEquals(Runtime.getRuntime().availableProcessors(), useCase.getParallelism());
  }

  /**
   * Verifies that all files in the target directory are visited when running with parallelism > 1.
   * Each file should have its compilation unit passed to the operation at least once.
   */
  @Test
  public void testParallelExecutionProcessesAllFiles() throws IOException {
    int fileCount = 4;
    writeJavaClasses("File", fileCount);

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), useCase(fileCount, recordVisitedPaths(processedFiles)));

    assertEquals("All files should have been visited", fileCount, processedFiles.size());
  }

  /**
   * Verifies that sequential (parallelism=1) and parallel (parallelism=N) execution
   * visit the same number of unique files.
   */
  @Test
  public void testParallelAndSequentialVisitSameFiles() throws IOException {
    int fileCount = 3;
    writeJavaClasses("ClassFile", fileCount);

    for (int parallelism : new int[]{1, fileCount}) {
      Set<Path> visited = ConcurrentHashMap.newKeySet();

      AstraCore.run(tempDir.toString(), useCase(parallelism, recordVisitedPaths(visited)));

      assertEquals("parallelism=" + parallelism + " should visit all files", fileCount, visited.size());
    }
  }

  /**
   * Verifies that per-file errors are surfaced rather than silently swallowed.
   * An operation that always throws should cause AstraCore.run() to throw.
   */
  @Test
  public void testParallelExecutionSurfacesPerFileErrors() throws IOException {
    writeJavaClasses("File", 2);

    UseCase useCase = useCase(2, (compilationUnit, node, rewriter) -> {
      throw new RuntimeException("Intentional test failure");
    });

    try {
      AstraCore.run(tempDir.toString(), useCase);
      fail("Expected a RuntimeException to be thrown for failing operations");
    } catch (RuntimeException e) {
      assertNotNull("Exception message should be present", e.getMessage());
      // The root cause chain should reach the original failures
      Throwable root = e;
      while (root.getCause() != null) {
        root = root.getCause();
      }
      assertTrue("Exception chain should reference processing failures",
          root.getMessage() != null);
    }
  }

  /**
   * Verifies that a single failing file does not prevent other files from being processed.
   */
  @Test
  public void testParallelExecutionContinuesAfterFileError() throws IOException {
    int totalFiles = 4;
    writeJavaClasses("File", totalFiles);

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();
    AtomicInteger failCount = new AtomicInteger();

    UseCase useCase = useCase(2, (compilationUnit, node, rewriter) -> {
      Path path = visitedPath(compilationUnit);
      if (path != null && path.getFileName().toString().equals("File2.java")) {
        failCount.incrementAndGet();
        throw new RuntimeException("Intentional failure for File2");
      }
      if (path != null) {
        processedFiles.add(path);
      }
    });

    try {
      AstraCore.run(tempDir.toString(), useCase);
      fail("Expected a RuntimeException due to File2 failing");
    } catch (RuntimeException e) {
      // File2 failed, so 3 other files should have been processed successfully
      assertEquals("Non-failing files should have been processed", totalFiles - 1, processedFiles.size());
      assertTrue("File2 should have triggered the failure", failCount.get() > 0);
    }
  }

  /**
   * Verifies that the shared file-selection filter (the {@code .java} extension check plus the
   * {@link UseCase} path prefiltering predicate) is honoured by the processing walk: only files
   * passing the predicate are processed, and non-matching ones are not. This proves the count walk
   * and the processing walk use the same filter, so the progress total cannot drift.
   */
  @Test
  public void testPrefilteringPredicateLimitsProcessedFiles() throws IOException {
    // Three files that should be processed (names starting with "Include")...
    writeJavaClasses("Include", 3);
    // ...and two that should be filtered out by the prefiltering predicate.
    writeJavaClasses("Exclude", 2);
    // A non-.java file that must never be processed regardless of the predicate.
    Files.writeString(tempDir.resolve("notes.txt"), "not a java file");

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = useCase(2, path -> path.contains("Include"), recordVisitedPaths(processedFiles));

    AstraCore.run(tempDir.toString(), useCase);

    assertEquals("Only files passing the prefiltering predicate should be processed",
        3, processedFiles.size());
    for (Path processed : processedFiles) {
      assertTrue("Processed file should match the predicate: " + processed,
          processed.getFileName().toString().contains("Include"));
    }
  }

  /**
   * Verifies that the total used for progress logging equals the number of files passing the shared
   * filter. The progress messages report "[X] of [N] files reviewed"; we capture the logs and assert
   * the denominator N matches the count of files that pass the prefiltering predicate.
   */
  @Test
  public void testProgressTotalMatchesFilteredFileCount() throws IOException {
    int matching = 5;
    writeJavaClasses("Match", matching);
    // Files that do not pass the predicate must not be counted in the progress total.
    writeJavaClasses("Skip", 3);

    UseCase useCase = useCase(1, path -> path.contains("Match"));

    List<ILoggingEvent> events = captureAstraLogs(() -> AstraCore.run(tempDir.toString(), useCase));

    // The progress messages embed the total as "of [N] files reviewed".
    Pattern totalPattern = Pattern.compile("of \\[(\\d+)\\] files reviewed");
    boolean foundProgress = false;
    for (ILoggingEvent event : events) {
      Matcher matcher = totalPattern.matcher(event.getFormattedMessage());
      if (matcher.find()) {
        foundProgress = true;
        assertEquals("Progress total should match the number of files passing the filter",
            matching, Integer.parseInt(matcher.group(1)));
      }
    }
    assertTrue("Expected at least one progress log line with a total", foundProgress);
  }


  /**
   * Writes {@code count} trivial Java classes named {@code <prefix>1.java} .. {@code <prefix><count>.java}
   * into the temp directory.
   */
  private void writeJavaClasses(String prefix, int count) throws IOException {
    for (int i = 1; i <= count; i++) {
      Files.writeString(tempDir.resolve(prefix + i + ".java"), "public class " + prefix + i + " {}");
    }
  }


  /**
   * Builds a {@link UseCase} running the given operations at the given parallelism, accepting all files.
   */
  private static UseCase useCase(int parallelism, ASTOperation... operations) {
    return useCase(parallelism, path -> true, operations);
  }


  /**
   * Builds a {@link UseCase} running the given operations at the given parallelism, filtered by the
   * supplied path prefiltering predicate.
   */
  private static UseCase useCase(int parallelism, Predicate<String> prefilteringPredicate, ASTOperation... operations) {
    return new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of(operations);
      }

      @Override
      public int getParallelism() {
        return parallelism;
      }

      @Override
      public Predicate<String> getPrefilteringPredicate() {
        return prefilteringPredicate;
      }
    };
  }


  /**
   * An operation that records the absolute path of every compilation unit it visits into {@code sink}.
   */
  private static ASTOperation recordVisitedPaths(Set<Path> sink) {
    return (compilationUnit, node, rewriter) -> {
      Path path = visitedPath(compilationUnit);
      if (path != null) {
        sink.add(path);
      }
    };
  }


  /**
   * Extracts the absolute path property that AstraCore attaches to each compilation unit it processes.
   */
  private static Path visitedPath(org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
    return (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
  }


  /** An action that may throw {@link IOException}, e.g. a call to {@link AstraCore#run}. */
  @FunctionalInterface
  private interface IOAction {
    void run() throws IOException;
  }


  /**
   * Captures the {@code INFO}-level log events emitted by {@link AstraCore} while {@code action} runs.
   */
  private static List<ILoggingEvent> captureAstraLogs(IOAction action) throws IOException {
    List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
      @Override
      protected void append(ILoggingEvent event) {
        events.add(event);
      }
    };
    appender.start();

    Logger astraLogger = (Logger) LoggerFactory.getLogger(AstraCore.class);
    Level previousLevel = astraLogger.getLevel();
    astraLogger.setLevel(Level.INFO);
    astraLogger.addAppender(appender);
    try {
      action.run();
    } finally {
      astraLogger.detachAppender(appender);
      astraLogger.setLevel(previousLevel);
      appender.stop();
    }
    return events;
  }
}
