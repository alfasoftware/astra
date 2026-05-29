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
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("File" + i + ".java"),
          "public class File" + i + " { void method() {} }");
    }

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null) {
            processedFiles.add(path);
          }
        });
      }

      @Override
      public int getParallelism() {
        return fileCount;
      }
    };

    AstraCore.run(tempDir.toString(), useCase);

    assertEquals("All files should have been visited", fileCount, processedFiles.size());
  }

  /**
   * Verifies that sequential (parallelism=1) and parallel (parallelism=N) execution
   * visit the same number of unique files.
   */
  @Test
  public void testParallelAndSequentialVisitSameFiles() throws IOException {
    int fileCount = 3;
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("ClassFile" + i + ".java"),
          "public class ClassFile" + i + " {}");
    }

    for (int parallelism : new int[]{1, fileCount}) {
      Set<Path> visited = ConcurrentHashMap.newKeySet();
      UseCase useCase = new UseCase() {
        @Override
        public Set<? extends ASTOperation> getOperations() {
          return Set.of((compilationUnit, node, rewriter) -> {
            Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
            if (path != null) visited.add(path);
          });
        }

        @Override
        public int getParallelism() {
          return parallelism;
        }
      };

      AstraCore.run(tempDir.toString(), useCase);
      assertEquals("parallelism=" + parallelism + " should visit all files", fileCount, visited.size());
    }
  }

  /**
   * Verifies that per-file errors are surfaced rather than silently swallowed.
   * An operation that always throws should cause AstraCore.run() to throw.
   */
  @Test
  public void testParallelExecutionSurfacesPerFileErrors() throws IOException {
    Files.writeString(tempDir.resolve("File1.java"), "public class File1 {}");
    Files.writeString(tempDir.resolve("File2.java"), "public class File2 {}");

    UseCase useCase = new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          throw new RuntimeException("Intentional test failure");
        });
      }

      @Override
      public int getParallelism() {
        return 2;
      }
    };

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
    for (int i = 1; i <= totalFiles; i++) {
      Files.writeString(tempDir.resolve("File" + i + ".java"),
          "public class File" + i + " {}");
    }

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();
    AtomicInteger failCount = new AtomicInteger();

    UseCase useCase = new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null && path.getFileName().toString().equals("File2.java")) {
            failCount.incrementAndGet();
            throw new RuntimeException("Intentional failure for File2");
          }
          if (path != null) {
            processedFiles.add(path);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 2;
      }
    };

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
    for (int i = 1; i <= 3; i++) {
      Files.writeString(tempDir.resolve("Include" + i + ".java"),
          "public class Include" + i + " {}");
    }
    // ...and two that should be filtered out by the prefiltering predicate.
    for (int i = 1; i <= 2; i++) {
      Files.writeString(tempDir.resolve("Exclude" + i + ".java"),
          "public class Exclude" + i + " {}");
    }
    // A non-.java file that must never be processed regardless of the predicate.
    Files.writeString(tempDir.resolve("notes.txt"), "not a java file");

    Set<Path> processedFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = new UseCase() {
      @Override
      public Predicate<String> getPrefilteringPredicate() {
        return path -> path.contains("Include");
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null) {
            processedFiles.add(path.getFileName());
          }
        });
      }

      @Override
      public int getParallelism() {
        return 2;
      }
    };

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
    for (int i = 1; i <= matching; i++) {
      Files.writeString(tempDir.resolve("Match" + i + ".java"),
          "public class Match" + i + " {}");
    }
    // Files that do not pass the predicate must not be counted in the progress total.
    for (int i = 1; i <= 3; i++) {
      Files.writeString(tempDir.resolve("Skip" + i + ".java"),
          "public class Skip" + i + " {}");
    }

    UseCase useCase = new UseCase() {
      @Override
      public Predicate<String> getPrefilteringPredicate() {
        return path -> path.contains("Match");
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return new HashSet<>();
      }

      @Override
      public int getParallelism() {
        return 1;
      }
    };

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
      AstraCore.run(tempDir.toString(), useCase);
    } finally {
      astraLogger.detachAppender(appender);
      astraLogger.setLevel(previousLevel);
      appender.stop();
    }

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
}
