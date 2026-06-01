package org.alfasoftware.astra.core.utils;

import static org.alfasoftware.astra.core.utils.AstraUtils.makeChangesFromAST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.imports.UnusedImportRefactor;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 *  AstraCore operates on source files in an input directory, building an AST for each file, using any additional classpaths supplied. 
 *  It also builds an ASTRewriter to record changes.
 *
 *  It then visits every ASTNode in the AST, passing the nodes through a set of ASTOperations.
 *  If an operation is applicable to that node, (e.g. is this a method invocation? Is it an invocation of method Y?)
 *  it can record changes in the ASTRewriter (e.g. invoke method Z instead)
 *
 *  When all ASTNodes have been visited, any changes recorded in the ASTRewriter are written back to the source file.
 */
public class AstraCore {

  protected static final Logger log = LoggerFactory.getLogger(AstraCore.class);


  /**
   * Applies a {@link UseCase} to a specified directory.
   * This is the entry point to be used when calling the tool from outside this class.
   *
   * @param targetDirectoryPath The absolute path to the directory that the {@link UseCase} should be applied to
   * @param useCase The {@link UseCase} describing the Astra refactor to perform
   */
  public static void run(String targetDirectoryPath, UseCase useCase) {
    String[] sources = useCase.getSources();
    String[] classPath = useCase.getClassPath();
    validateSourceAndClasspath(sources, classPath);
    try {
      AstraCore main = new AstraCore();
      main.runOperations(targetDirectoryPath, useCase, sources, classPath);
    } catch (IOException e) {
      throw new RuntimeException("Astra run failed for directory [" + targetDirectoryPath + "]: " + e.getMessage(), e);
    }
  }


  protected void runOperations(String directoryPath, UseCase useCase, String[] sources, String[] classPath) throws IOException {
    log.info(System.lineSeparator() +
      "================================================" + System.lineSeparator() +
      "     ____     ____________________     ____" + System.lineSeparator() +
      "    /    \\   /  __|__    __|   _  \\   /    \\" + System.lineSeparator() +
      "   /  /\\  \\  \\  \\    |  |  |  |/  /  /  /\\  \\" + System.lineSeparator() +
      "  /  /__\\  \\__\\  \\   |  |  |  |\\  \\ /  /__\\  \\" + System.lineSeparator() +
      " /__/    \\__\\____/   |__|  |__| \\__\\__/    \\__\\" + System.lineSeparator() +
      "================================================");

    log.info("Starting Astra run for directory: " + directoryPath);
    AtomicLong currentFileIndex = new AtomicLong();
    AtomicLong currentPercentage = new AtomicLong();
    Instant startTime = Instant.now();

    // The same file-selection filter (.java extension + the UseCase path prefiltering predicate)
    // is used for both the count walk and the processing walk, so the progress denominator can
    // never drift from the set of files actually processed.
    Path sourcePath = Paths.get(directoryPath);
    Predicate<Path> fileFilter = buildFileFilter(useCase);

    // First walk: a cheap count pass that reads only directory metadata (not file contents) to
    // determine the total number of files to process, which feeds the progress percentage below.
    log.info("Counting files (this may take a few seconds)");
    long totalFiles;
    try (Stream<Path> walk = Files.walk(sourcePath)) {
      totalFiles = walk.filter(fileFilter).count();
    }
    log.info(totalFiles + " files to process after prefiltering");

    if (totalFiles == 0) {
      log.info(getPrintableDuration(Duration.between(startTime, Instant.now())));
      return;
    }

    Set<? extends ASTOperation> operations = useCase.getOperations();
    int parallelism = useCase.getParallelism();
    Predicate<String> contentPrefilteringPredicate = useCase.getContentPrefilteringPredicate();
    log.info("Processing [" + totalFiles + "] files with [" + parallelism + "] thread(s)");

    // Second walk: materialise every path that passes the file filter, then read each file's
    // content so that the content-prefiltering predicate can be applied before batch parsing.
    // Files that pass the content predicate are collected for the shared-environment batch parse;
    // files that are filtered out are tracked separately so they still count toward progress.
    //
    // Materialising paths here (rather than streaming them directly to the executor) is a
    // deliberate trade-off: it is required so all qualifying paths can be handed to
    // ASTParser.createASTs() in a single call, which amortises the cost of classpath scanning
    // (JAR index loading, type environment initialisation) across the entire run rather than
    // paying it once per file.
    List<Path> pathsToParse = new ArrayList<>();
    Map<String, String> pathToContent = new LinkedHashMap<>();
    List<Path> contentFilteredPaths = new ArrayList<>();
    Map<Path, RuntimeException> readFailures = new LinkedHashMap<>();

    try (Stream<Path> walk = Files.walk(sourcePath)) {
      walk.filter(fileFilter).forEach(path -> {
        try {
          String content = new String(Files.readAllBytes(path.toAbsolutePath()));
          if (contentPrefilteringPredicate.test(content)) {
            pathsToParse.add(path);
            pathToContent.put(path.toAbsolutePath().normalize().toString(), content);
          } else {
            log.debug("Skipping [{}] — excluded by content pre-filtering predicate", path);
            contentFilteredPaths.add(path);
          }
        } catch (IOException e) {
          readFailures.put(path, new RuntimeException(
              "Failed to read file [" + path + "]: " + e.getMessage(), e));
        }
      });
    }

    // Batch-parse all files that passed the content filter using a single shared compilation
    // environment. JDT initialises the classpath (reads JAR indices, populates the type
    // environment) exactly once for the entire batch rather than once per file.
    log.info("Batch parsing [" + pathsToParse.size() + "] file(s) with shared compilation environment");
    Map<String, CompilationUnit> parsedUnits = batchParseFiles(pathsToParse, sources, classPath);

    List<Throwable> fileErrors = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    try {
      List<Future<?>> futures = new ArrayList<>();

      // Submit work futures for files that were batch-parsed.
      for (Path path : pathsToParse) {
        String key = path.toAbsolutePath().normalize().toString();
        CompilationUnit cu = parsedUnits.get(key);
        String content = pathToContent.get(key);
        if (cu != null && content != null) {
          futures.add(executor.submit(() ->
              applyOperationsAndSaveWithPreParsedCU(path, content, cu, operations, sources, classPath)));
        } else {
          // Defensive fallback: batch parse did not return a CU (should not happen with JDT).
          log.warn("Batch parse produced no CompilationUnit for [{}]; falling back to per-file parse", path);
          futures.add(executor.submit(() ->
              applyOperationsAndSave(path, operations, sources, classPath, s -> true)));
        }
      }

      // Submit failure futures for files that could not be read (surfaces them via future.get()).
      for (Map.Entry<Path, RuntimeException> entry : readFailures.entrySet()) {
        RuntimeException ex = entry.getValue();
        futures.add(executor.submit((Runnable) () -> { throw ex; }));
      }

      // Submit no-op futures for content-filtered files so that they count toward the progress
      // denominator, preserving the same progress behaviour as the previous per-file code path.
      for (int i = 0; i < contentFilteredPaths.size(); i++) {
        futures.add(executor.submit(() -> {}));
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          log.error("Failed to process file: " + cause.getMessage(), cause);
          fileErrors.add(cause);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("File processing was interrupted", e);
        }
        long idx = currentFileIndex.incrementAndGet();
        long newPct = idx * 100 / totalFiles;
        if (currentPercentage.getAndSet(newPct) != newPct) {
          logProgress(idx, newPct, startTime, totalFiles);
        }
      }
    } finally {
      List<Runnable> notStarted = executor.shutdownNow();
      if (!notStarted.isEmpty()) {
        log.warn(notStarted.size() + " file(s) were not processed due to early termination");
      }
    }

    log.info(getPrintableDuration(Duration.between(startTime, Instant.now())));

    if (!fileErrors.isEmpty()) {
      IOException summary = new IOException(
          fileErrors.size() + " file(s) failed during processing; see suppressed exceptions for details");
      fileErrors.forEach(summary::addSuppressed);
      throw summary;
    }
  }


  /**
   * Batch-parses all of the supplied source files using a single shared JDT compilation
   * environment.
   *
   * <p>A single {@link ASTParser} is configured with the supplied classpath and source paths,
   * and {@link ASTParser#createASTs} is called with all file paths in one shot.  JDT
   * initialises its internal {@code LookupEnvironment} — which involves scanning every JAR
   * and source root on the classpath — exactly once for the whole batch rather than once per
   * file, which is the primary cost saving over the per-file {@code createAST()} API.
   *
   * <p><strong>Thread safety:</strong> {@code createASTs()} processes files sequentially on
   * the calling thread, calling back into {@code acceptAST()} for each one.  Bindings are
   * resolved eagerly during parsing; by the time this method returns the returned
   * {@link CompilationUnit} objects are fully resolved and can be read safely from multiple
   * worker threads in the subsequent parallel operation-application phase — provided those
   * threads do not themselves trigger new binding lookups that write to the shared
   * {@code LookupEnvironment}.  In practice, all Astra operations only <em>read</em>
   * already-resolved bindings, so concurrent operation application is safe.
   *
   * @return a map from normalised absolute path string to {@link CompilationUnit}; the path
   *         strings are exactly those returned by
   *         {@link Path#toAbsolutePath()}{@code .normalize().toString()} for each input path.
   */
  private static Map<String, CompilationUnit> batchParseFiles(
      List<Path> paths, String[] sources, String[] classPath) {

    Map<String, CompilationUnit> result = new HashMap<>(paths.size() * 2);

    if (paths.isEmpty()) {
      return result;
    }

    ASTParser parser = AstraUtils.createBatchParser(sources, classPath);

    String[] absolutePaths = paths.stream()
        .map(p -> p.toAbsolutePath().normalize().toString())
        .toArray(String[]::new);
    String[] fileEncodings = new String[absolutePaths.length];
    Arrays.fill(fileEncodings, "UTF-8");

    parser.createASTs(absolutePaths, fileEncodings, new String[0],
        new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            // sourceFilePath is exactly what we passed in (absolute + normalised).
            ast.setProperty(CompilationUnitProperty.ABSOLUTE_PATH,
                Paths.get(sourceFilePath).toAbsolutePath());
            ast.recordModifications();
            result.put(sourceFilePath, ast);
          }
        }, null);

    return result;
  }


  /**
   * Applies {@code operations} to a file whose {@link CompilationUnit} was already produced by
   * the batch parse, then runs import cleanup and writes the file back if content changed.
   *
   * <p>This mirrors the logic in {@link #applyOperationsAndSave} but skips the per-file
   * {@link ASTParser} / {@link AstraUtils#readAsCompilationUnit} call that would otherwise
   * re-initialise the JDT classpath environment for this individual file.
   *
   * <p>This method is designed to be called from multiple worker threads in parallel; each
   * invocation operates exclusively on its own {@link CompilationUnit} and {@link ASTRewrite},
   * so there is no shared mutable state between threads.
   */
  private void applyOperationsAndSaveWithPreParsedCU(
      Path javaFile,
      String fileContentBefore,
      CompilationUnit preParseUnit,
      Set<? extends ASTOperation> operations,
      String[] sources,
      String[] classpath) {
    try {
      ASTRewrite rewriter = runOperations(operations, preParseUnit);
      String fileContentAfter = makeChangesFromAST(fileContentBefore, rewriter);

      if (fileContentAfter.equals(fileContentBefore)) {
        return;
      }

      // File was changed: run import cleanup and write back.
      fileContentAfter = applyOperationsToSource(
          new HashSet<>(Arrays.asList(new UnusedImportRefactor())),
          sources, classpath, javaFile, fileContentAfter);

      if (!fileContentAfter.equals(fileContentBefore)) {
        Files.write(javaFile.toAbsolutePath(), fileContentAfter.getBytes(),
            StandardOpenOption.TRUNCATE_EXISTING);
      }
    } catch (IOException | BadLocationException | IllegalArgumentException e) {
      throw new RuntimeException("Failed to process file [" + javaFile + "]: " + e.getMessage(), e);
    }
  }


  /**
   * Builds the single, shared file-selection filter applied to every {@link Path} encountered when
   * walking the source directory. This combines the {@code .java} file check with the path-level
   * prefiltering predicate from the {@link UseCase} ({@link UseCase#getPrefilteringPredicate()}).
   *
   * <p>The same {@link Predicate} instance is used for both the count walk (which determines the
   * total used for progress reporting) and the processing walk, ensuring the two can never diverge.
   */
  private Predicate<Path> buildFileFilter(UseCase useCase) {
    Predicate<String> prefilteringPredicate = useCase.getPrefilteringPredicate();
    return f -> f.toFile().isFile()
        && f.getFileName().toString().endsWith("java")
        && prefilteringPredicate.test(f.toString());
  }


  private void logProgress(long currentFileIndex, long currentPercentage, Instant startTime, long totalNumberOfFiles) {
    Duration elapsedDuration = Duration.between(startTime, Instant.now());
    Duration estimatedDuration = elapsedDuration.multipliedBy(totalNumberOfFiles).dividedBy(currentFileIndex);
    Duration remainingDuration = estimatedDuration.minus(elapsedDuration);

    StringBuilder progressMessage = new StringBuilder()
        .append("[" + currentPercentage + "%] complete")
        .append(", [" + currentFileIndex + "] of [" + totalNumberOfFiles + "] files reviewed");

    if (elapsedDuration.toMinutes() > 0) {
      progressMessage.append(", [" + elapsedDuration.toMinutes() + "] minute");
      if (elapsedDuration.toMinutes() != 1) {
        progressMessage.append("s");
      }
      progressMessage.append(" elapsed, estimated [" + remainingDuration.toMinutes() + "] minutes remaining");
    }

    log.info(progressMessage.toString());
  }


  private String getPrintableDuration(Duration duration) {
    long minutes = duration.toMinutes();
    long seconds = TimeUnit.MILLISECONDS.toSeconds(duration.minusMinutes(minutes).toMillis());
    StringBuilder builder = new StringBuilder("Run complete in ");
    if (minutes > 0) {
      builder.append("[" + minutes + "] minute");
      if (minutes != 1) {
        builder.append("s");
      }
      builder.append(", ");
    }
    builder.append("[" + seconds + "] second");
    if (seconds != 1) {
      builder.append("s");
    }
    return builder.append(".").toString();
  }


  /**
   * Validates that the provided source and classpath entries exist
   */
  protected static void validateSourceAndClasspath(String[] sources, String[] classPath) {
    Predicate<String> isNotFound = s -> ! s.isEmpty() && ! new File(s).exists();
    Stream.of(sources)
    .filter(isNotFound)
    .peek(s -> log.error("Source: [" + s + "] does not exist"))
    .findAny()
    .ifPresent(s -> { throw new RuntimeException("Supplied source does not exist: " + s);});

    Stream.of(classPath)
    .filter(isNotFound)
    .peek(c -> log.error("Classpath: [" + c + "] does not exist"))
    .findAny()
    .ifPresent(s -> { throw new RuntimeException("Supplied classpath does not exist: " + s);});
  }


  /**
   * Applies the operations to a source file and then overwrites that file with the result.
   * Operations that result in an empty file, will cause the file to be deleted.
   */
  protected void applyOperationsAndSave(Path javaFile, Set<? extends ASTOperation> operations, String[] sources, String[] classpath) {
    applyOperationsAndSave(javaFile, operations, sources, classpath, content -> true);
  }

  /**
   * Applies the operations to a source file and then overwrites that file with the result.
   * Files whose content does not satisfy {@code contentPrefilteringPredicate} are skipped entirely,
   * avoiding the cost of AST construction.
   * Operations that result in an empty file, will cause the file to be deleted.
   */
  protected void applyOperationsAndSave(Path javaFile, Set<? extends ASTOperation> operations, String[] sources, String[] classpath, Predicate<String> contentPrefilteringPredicate) {
    try {
      String fileContentBefore = new String(Files.readAllBytes(javaFile.toAbsolutePath()));

      // Apply the content predicate before parsing — skip files that cannot be affected
      if (!contentPrefilteringPredicate.test(fileContentBefore)) {
        log.debug("Skipping [{}] — excluded by content pre-filtering predicate", javaFile);
        return;
      }

      // apply the operations
      final String fileContentAfter = applyOperationsToFile(javaFile, fileContentBefore, operations, sources, classpath);

      // If the file content has changed
      if (! fileContentAfter.equals(fileContentBefore)) {
        // save the file (over the original)
        Files.write(javaFile.toAbsolutePath(), fileContentAfter.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
      }
    } catch (IOException | BadLocationException | IllegalArgumentException e) {
      throw new RuntimeException("Failed to process file [" + javaFile + "]: " + e.getMessage(), e);
    }
  }


  /**
   * For a given Java source file, this models the effect of applying a set of operations to the source,
   * and then removing any unused imports if a change was made.
   *
   * @param fileContentBefore Source file content before any operations are applied
   * @param operations Operations to apply
   */
  public String applyOperationsToFile(Path file, String fileContentBefore, Set<? extends ASTOperation> operations, String[] sources, String[] classpath) throws BadLocationException {

    String fileContentAfter = applyOperationsToSource(operations, sources, classpath, file, fileContentBefore);

    // If file hasn't changed, return as-is
    if (fileContentAfter.equals(fileContentBefore)) {
      return fileContentAfter;
    } else {
      // If we've modified the file, remove any unused imports
      return applyOperationsToSource(new HashSet<>(Arrays.asList(new UnusedImportRefactor())), sources, classpath, file, fileContentAfter);
    }
  }


  /**
   * Runs operations over the source file, and returns the result of running those operations
   */
  private String applyOperationsToSource(Set<? extends ASTOperation> operations, String[] sources, String[] classpath, Path file, String source)
      throws BadLocationException {
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(file, source, sources, classpath);
    ASTRewrite rewriter = runOperations(operations, compilationUnit);
    return makeChangesFromAST(source, rewriter);
  }


  /**
   * Run the provided operations over the ASTNodes in the compilation unit,
   * recording any changes to be made to that compilation unit in the ASTRewrite.
   *
   * @param operations Operations to apply to ASTNodes in the compilation unit
   * @param compilationUnit The compilation unit - expected to be a whole Java source file
   * @return ASTRewrite, a collection of changes to make to the source file
   */
  private static ASTRewrite runOperations(Set<? extends ASTOperation> operations, final CompilationUnit compilationUnit) {

    // Create the re-writer for modifying the code
    final ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());

    final ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);

    for (ASTOperation operation : operations) {

      // For every ASTNode we've visited
      visitor.getVisitedNodes()
      .forEach(node -> {
        try {
          // Pass them to the operation
          operation.run(compilationUnit, node, rewriter);
        } catch (MalformedTreeException | IOException | BadLocationException e) {
          throw new RuntimeException(e);
        }
      });
    }
    return rewriter;
  }
}
