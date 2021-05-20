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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.imports.UnusedImportRefactor;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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

  protected static final Logger log = Logger.getLogger(AstraCore.class);


  /**
   * Applies a {@link UseCase} to a specified directory.
   * This is the entry point to be used when calling the tool from outside this class.
   *
   * @param targetDirectoryPath The absolute path to the directory that the {@link UseCase} should be applied to
   * @param useCase The {@link UseCase} describing the Astra refactor to perform
   */
  public static void run(String targetDirectoryPath, UseCase useCase) {
    validateSourceAndClasspath(useCase.getSources(), useCase.getClassPath());
    try {
      AstraCore main = new AstraCore();
      main.runOperations(targetDirectoryPath, useCase);
    } catch (IOException e) {
      log.error("ioE: " + e);
    }
  }


  protected void runOperations(String directoryPath, UseCase useCase) throws IOException {
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
    log.info("Counting files (this may take a few seconds)");
    Instant startTime = Instant.now();
    
    List<Path> javaFilesInDirectory;
    try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {
      javaFilesInDirectory = walk
          .filter(f -> f.toFile().isFile())
          .filter(f -> f.getFileName().toString().endsWith("java"))
          .collect(Collectors.toList());
    }
    log.info(javaFilesInDirectory.size() + " .java files in directory to review");

    log.info("Applying prefilters to files in directory");
    Predicate<String> prefilteringPredicate = useCase.getPrefilteringPredicate();
    List<Path> filteredJavaFiles = javaFilesInDirectory.stream()
        .filter(f -> prefilteringPredicate.test(f.toString()))
        .collect(Collectors.toList());
    log.info(filteredJavaFiles.size() + " files remain after prefiltering");

    for (Path f : filteredJavaFiles) {
      // TODO AstUtils.getClassFilesForSource(f.toString()); - attempt to get only relevant classpaths for a given source file?
      // TODO Naively we can multi-thread here (i.e. per file) but simple testing indicated that this slowed us down.
      applyOperationsAndSave(new File(f.toString()), useCase.getOperations(), useCase.getSources(), useCase.getClassPath());
      long newPercentage = currentFileIndex.incrementAndGet() * 100 / filteredJavaFiles.size();
      if (newPercentage != currentPercentage.get()) {
        currentPercentage.set(newPercentage);
        logProgress(currentFileIndex.get(), currentPercentage.get(), startTime, filteredJavaFiles.size());
      }
    }
    
    log.info(getPrintableDuration(Duration.between(startTime, Instant.now())));
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

    log.info(progressMessage);
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
   */
  protected void applyOperationsAndSave(File javaFile, Set<? extends ASTOperation> operations, String[] sources, String[] classpath) {
    try {
      String fileContentBefore = new String(Files.readAllBytes(Paths.get(javaFile.getAbsolutePath())));
      // apply the operations
      final String fileContentAfter = applyOperationsToFile(fileContentBefore, operations, sources, classpath);
      
      // If the file content has changed
      if (! fileContentAfter.equals(fileContentBefore)) {
        // save the file (over the original)
        Files.write(Paths.get(javaFile.getAbsolutePath()), fileContentAfter.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);        
      }
    } catch (IOException e) {
      log.error("ioE: " + e);
    } catch (BadLocationException e) {
      log.error("blE: " + e);
    } catch (IllegalArgumentException e){
      log.error("IAE: " + e);
    }
  }

  
  /**
   * For a given Java source file, this models the effect of applying a set of operations to the source,
   * and then removing any unused imports if a change was made.
   *
   * @param fileContentBefore Source file content before any operations are applied
   * @param operations Operations to apply
   */
  public String applyOperationsToFile(String fileContentBefore, Set<? extends ASTOperation> operations, String[] sources, String[] classpath) throws BadLocationException {

    String fileContentAfter = applyOperationsToSource(operations, sources, classpath, fileContentBefore);

    // If file hasn't changed, return as-is
    if (fileContentAfter.equals(fileContentBefore)) {
      return fileContentAfter;
    } else {
      // If we've modified the file, remove any unused imports
      return applyOperationsToSource(new HashSet<>(Arrays.asList(new UnusedImportRefactor())), sources, classpath, fileContentAfter);
    }
  }

  
  /**
   * Runs operations over the source file, and returns the result of running those operations
   */
  protected String applyOperationsToSource(Set<? extends ASTOperation> operations, String[] sources, String[] classpath, String source)
      throws BadLocationException {
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(source, sources, classpath);
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
