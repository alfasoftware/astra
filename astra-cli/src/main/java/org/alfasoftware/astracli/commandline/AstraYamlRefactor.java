package org.alfasoftware.astracli.commandline;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.alfasoftware.astracli.config.AstraOperationFactory;
import org.alfasoftware.astracli.config.RefactorConfig;
import org.alfasoftware.astracli.config.YamlConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * CLI subcommand {@code astra yaml} — applies refactoring operations declared in a YAML file.
 *
 * <pre>
 * astra yaml --config refactors.yaml --dir /path/to/source --cp /path/to/dep.jar
 * </pre>
 *
 * <p>Multiple operations of different types can be combined in a single YAML file.
 * See the project's {@code example-refactors.yaml} for the full schema.
 */
@CommandLine.Command(name = "yaml",
  sortOptions = false,
  headerHeading = "@|bold,underline Usage:|@%n%n",
  synopsisHeading = "%n",
  descriptionHeading = "%n@|bold,underline Description:|@%n%n",
  parameterListHeading = "%n@|bold,underline Parameters:|@%n",
  optionListHeading = "%n@|bold,underline Options:|@%n",
  header = "Apply refactors from a YAML config file.",
  description = "Reads one or more refactoring operations from a YAML configuration file and applies them to the target source directory.")
class AstraYamlRefactor implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(AstraYamlRefactor.class);

  @Option(
    names = {"-f", "--config"},
    required = true,
    paramLabel = "<config>",
    description = "Path to the YAML refactor configuration file.")
  File configFile;

  @Option(
    names = {"-d", "--dir"},
    required = true,
    description = "Set the path to the code checkout.")
  File directory;

  @Option(
    names = "--cp",
    required = true,
    description = "Set the path to the additional jar files. At least the jar containing the 'before' type should be specified.",
    split = "[,;]")
  File[] classpath;

  @Override
  public void run() {
    log.info("Starting [yaml] refactor from config: [" + configFile.getAbsolutePath() + "]");

    RefactorConfig config;
    try {
      config = new YamlConfigParser().parse(configFile);
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to parse YAML config file '" + configFile.getAbsolutePath() + "': " + e.getMessage(), e);
    }

    List<ASTOperation> operations;
    try {
      operations = new AstraOperationFactory().createOperations(config);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid refactor configuration: " + e.getMessage(), e);
    }

    log.info("Loaded " + operations.size() + " refactoring operation(s) from config");

    AstraCore.run(directory.getAbsolutePath(), new UseCase() {

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return new HashSet<>(operations);
      }

      @Override
      public Set<String> getAdditionalClassPathEntries() {
        return Arrays.asList(classpath).stream()
          .map(File::getAbsolutePath)
          .collect(Collectors.toSet());
      }
    });
  }
}
