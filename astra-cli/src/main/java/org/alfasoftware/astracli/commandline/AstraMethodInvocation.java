package org.alfasoftware.astracli.commandline;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;

import picocli.CommandLine;

@CommandLine.Command(name = "method",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Refactor method invocations.",
        description = "Finds method invocations matching given criteria, and performs a refactoring.")
class AstraMethodInvocation implements Runnable {

    static Logger log = Logger.getLogger(AstraMethodInvocation.class.getName());

    @CommandLine.Option(
      names = {"-t", "--fqType"},
      required = true,
      paramLabel = "<fqType>",
      description = "Fully qualified type of the method to match.")
    String fqType;

    @CommandLine.Option(
      names = {"-n", "--name"},
      required = true,
      paramLabel = "<name>",
      description = "Name of the method to match.")
    String name;

    @CommandLine.Option(
      names = {"-p", "--parameters"},
      required = true,
      paramLabel = "<parameters>",
      description = "Parameters of the method to match.")
    List<String> parameters = new ArrayList<>();

    @CommandLine.Option(
      names = {"-v", "--isVarargs"},
      paramLabel = "<isVarargs>",
      description = "Whether the method to match is varargs or not.")
    Boolean isVarargs;


    @CommandLine.Option(
      names = {"-nn", "--newname"},
      paramLabel = "<newname>",
      description = "New name for the method.")
    String newName;

    @CommandLine.Option(
      names = {"-nt", "--newfqtype"},
      paramLabel = "<newfqtype>",
      description = "New fully qualified type for the method.")
    String newFQType;


    @CommandLine.Option(
      names = {"-d", "--dir"},
      required = true,
      description = "Set the path to the code checkout")
    File directory;

    @CommandLine.Option(
      names = "--cp",
      required = true,
      description = "Set the path to the additional jar files. At least the jar containing the 'before' type should be specified.")
    File[] classpath;


    @Override
    public void run() {
      MethodMatcher.Builder methodbuilder = MethodMatcher.builder()
          .withFullyQualifiedDeclaringType(fqType)
          .withMethodName(name)
          .withFullyQualifiedParameters(parameters);

      if (isVarargs != null) {
        methodbuilder = methodbuilder.isVarargs(isVarargs);
      }

      log.debug("Starting [method] refactor: [" + methodbuilder.build() + "]");

      MethodInvocationRefactor.Changes changes = new MethodInvocationRefactor.Changes();

      if (newName != null) {
        changes = changes.toNewMethodName(newName);
      }

      if (newFQType != null) {
        changes = changes.toNewType(newFQType);
      }

      performMethodRefactor(methodbuilder, changes);
    }


    private void performMethodRefactor(MethodMatcher.Builder methodbuilder, MethodInvocationRefactor.Changes changes) {
      AstraCore.run(directory.getAbsolutePath(), new UseCase() {

        @Override
        public Set<? extends ASTOperation> getOperations() {
          return new HashSet<>(Collections.singletonList(
            MethodInvocationRefactor
            .from(methodbuilder.build())
            .to(changes)));
        }

        @Override
        public Set<String> getAdditionalClassPathEntries() {
          return Arrays.asList(classpath).stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        }
      });
    }
}