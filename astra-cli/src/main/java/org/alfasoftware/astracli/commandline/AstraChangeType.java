package org.alfasoftware.astracli.commandline;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.types.TypeReferenceRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.apache.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "changetype",
  header = "Change type refactor.",
  showDefaultValues = true,
  customSynopsis = "@|bold astra changetype|@ [@|yellow <options>|@...] [--] [@|yellow <pathspec>|@...]",
  description = "Changes all references of a given fully qualified type to a different type"
)
class AstraChangeType implements Runnable {
	
	private static final Logger log = Logger.getLogger(AstraChangeType.class);

  @CommandLine.Parameters(
    arity = "2",
    paramLabel = "<fqBeforeTypeName> <fqAfterTypeName>",
    description = "Qualified types of the before and after")
  String[] types;

  @Option(
    names = {"-d", "--dir"},
    required = true,
    description = "Set the path to the code checkout")
  File directory;

  @Option(
    names = {"-c", "--cp"},
    required = true,
    description = "Set the path to the additional jar files. At least the jar containing the 'before' type should be specified.",
    split = "[,;]")
  File[] classpath;

    @Override
    public void run() {
      log.info("Starting [changetype] refactor: [" + types[0] + "] to [" + types[1] + "]");
      AstraCore.run(directory.getAbsolutePath(), new UseCase() {

        @Override
        public Set<? extends ASTOperation> getOperations() {
          return new HashSet<>(Collections.singletonList(
            TypeReferenceRefactor.builder()
              .fromType(types[0])
              .toType(types[1])
              .build()
          ));
        }

        @Override
        public Set<String> getAdditionalClassPathEntries() {
          return Arrays.asList(classpath).stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        }
      });
    }
}