package org.alfasoftware.astra;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs an ASTRA UseCase refactor over the given module.
 */
@Mojo(name = "refactor", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class RefactorMojo extends AbstractMojo {

  /**
   * The Maven project, containing runtime values based on the model.
   */
  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  protected MavenProject project;
  
  /**
   * The usecase fully-qualified class name
   */
  @Parameter(property = "astra.usecase")
  private String usecase;

  /**
   * The usecase fully-qualified class name
   */
  @Parameter(property = "astra.skip", defaultValue = "false")
  private String skip;

  /**
   * The source directory to be processed
   */
  @Parameter(property = "sourceDirectory", defaultValue = "${project.basedir}")
  private File sourceDirectory;

  /**
   * The target directory for the project.
   * This enables us to remove items from the classpath
   */
  @Parameter(property = "targetDirectory", readonly = true, defaultValue = "${project.build.directory}")
  private String targetDirectory;


  @Override
  public void execute() throws MojoExecutionException {

    List<String> testClasspathElements;
    try {
	    testClasspathElements = project.getTestClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Unable to resolve test class path for the project", e);
    }

    // remove anything within this projects target directory as this will invalidate it
    testClasspathElements.removeIf(s -> s.startsWith(targetDirectory));
  
    UseCase useCaseInstance = getUseCaseInstance();
    AstraCore.run(sourceDirectory.getAbsolutePath(), new UseCase() {

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return useCaseInstance.getOperations();
      }

      @Override
      public Predicate<String> getPrefilteringPredicate() {
        return useCaseInstance.getPrefilteringPredicate();
      }

      @Override
      public Set<String> getAdditionalClassPathEntries() {
        return Set.of();
      }
    });

  }


  private UseCase getUseCaseInstance() throws MojoExecutionException {
    try {
      Class<?> useCaseClazz = Class.forName(usecase);
      if (!UseCase.class.isAssignableFrom(useCaseClazz)) {
        throw new MojoExecutionException(String.format("Class [%s] must be of type org.alfasoftware.astra.core.refactoring.UseCase", usecase));
      }
      return (UseCase)useCaseClazz.getDeclaredConstructors()[0].newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | SecurityException | ClassNotFoundException e) {
      throw new MojoExecutionException(String.format("Unable to instantiate usecase [%s]", usecase), e);
    }
  }

}
