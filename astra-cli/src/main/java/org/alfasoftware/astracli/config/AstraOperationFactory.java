package org.alfasoftware.astracli.config;

import java.util.ArrayList;
import java.util.List;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.types.TypeReferenceRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts a {@link RefactorConfig} (parsed from YAML) into a list of {@link ASTOperation} instances
 * ready to be passed to {@code AstraCore.run()}.
 *
 * <p>Supported {@code type} values:
 * <ul>
 *   <li>{@code typeReference} — produces a {@link TypeReferenceRefactor}</li>
 *   <li>{@code methodInvocation} — produces a {@link MethodInvocationRefactor}</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} for missing required fields or unknown types.
 */
public class AstraOperationFactory {

  static final String TYPE_METHOD_INVOCATION = "methodInvocation";
  static final String TYPE_TYPE_REFERENCE = "typeReference";

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Creates one {@link ASTOperation} for each entry in the config.
   *
   * @throws IllegalArgumentException if the config is null/empty or any entry is invalid
   */
  public List<ASTOperation> createOperations(RefactorConfig config) {
    if (config == null || config.getRefactors() == null || config.getRefactors().isEmpty()) {
      throw new IllegalArgumentException("Config must contain at least one refactor entry under 'refactors'");
    }

    List<ASTOperation> operations = new ArrayList<>();
    List<RefactorEntry> refactors = config.getRefactors();
    for (int i = 0; i < refactors.size(); i++) {
      try {
        operations.add(createOperation(refactors.get(i)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Error in refactor entry " + i + ": " + e.getMessage(), e);
      }
    }
    return operations;
  }

  private ASTOperation createOperation(RefactorEntry entry) {
    if (entry.getType() == null || entry.getType().isBlank()) {
      throw new IllegalArgumentException("'type' is required");
    }

    switch (entry.getType()) {
      case TYPE_METHOD_INVOCATION:
        return createMethodInvocationRefactor(entry);
      case TYPE_TYPE_REFERENCE:
        return createTypeReferenceRefactor(entry);
      default:
        throw new IllegalArgumentException(
          "Unknown refactor type: '" + entry.getType() + "'. Must be one of: "
            + TYPE_METHOD_INVOCATION + ", " + TYPE_TYPE_REFERENCE);
    }
  }

  private MethodInvocationRefactor createMethodInvocationRefactor(RefactorEntry entry) {
    JsonNode fromNode = entry.getFrom();
    if (fromNode == null || !fromNode.isObject()) {
      throw new IllegalArgumentException(
        "'from' must be a YAML mapping (object) for type '" + TYPE_METHOD_INVOCATION + "'");
    }

    MethodFromConfig fromConfig = mapper.convertValue(fromNode, MethodFromConfig.class);

    if (isNullOrBlank(fromConfig.getQualifiedClass())) {
      throw new IllegalArgumentException("'from.qualifiedClass' is required for type '" + TYPE_METHOD_INVOCATION + "'");
    }
    if (isNullOrBlank(fromConfig.getMethod())) {
      throw new IllegalArgumentException("'from.method' is required for type '" + TYPE_METHOD_INVOCATION + "'");
    }

    MethodMatcher.Builder matcherBuilder = MethodMatcher.builder()
      .withFullyQualifiedDeclaringType(fromConfig.getQualifiedClass())
      .withMethodName(fromConfig.getMethod());

    if (fromConfig.getParameters() != null) {
      matcherBuilder = matcherBuilder.withFullyQualifiedParameters(fromConfig.getParameters());
    }

    if (Boolean.TRUE.equals(fromConfig.getVarargs())) {
      matcherBuilder = matcherBuilder.isVarargs(true);
    } else if (Boolean.FALSE.equals(fromConfig.getVarargs())) {
      matcherBuilder = matcherBuilder.isVarargs(false);
    }

    JsonNode toNode = entry.getTo();
    if (toNode == null || !toNode.isObject()) {
      throw new IllegalArgumentException(
        "'to' must be a YAML mapping (object) for type '" + TYPE_METHOD_INVOCATION + "'");
    }

    MethodToConfig toConfig = mapper.convertValue(toNode, MethodToConfig.class);

    if (isNullOrBlank(toConfig.getMethod()) && isNullOrBlank(toConfig.getQualifiedClass())) {
      throw new IllegalArgumentException(
        "At least one of 'to.method' or 'to.qualifiedClass' must be specified for type '" + TYPE_METHOD_INVOCATION + "'");
    }

    MethodInvocationRefactor.Changes changes = new MethodInvocationRefactor.Changes();
    if (!isNullOrBlank(toConfig.getMethod())) {
      changes = changes.toNewMethodName(toConfig.getMethod());
    }
    if (!isNullOrBlank(toConfig.getQualifiedClass())) {
      changes = changes.toNewType(toConfig.getQualifiedClass());
    }

    return MethodInvocationRefactor.from(matcherBuilder.build()).to(changes);
  }

  private TypeReferenceRefactor createTypeReferenceRefactor(RefactorEntry entry) {
    JsonNode fromNode = entry.getFrom();
    JsonNode toNode = entry.getTo();

    if (fromNode == null || !fromNode.isTextual() || fromNode.asText().isBlank()) {
      throw new IllegalArgumentException(
        "'from' must be a non-empty string (fully qualified type name) for type '" + TYPE_TYPE_REFERENCE + "'");
    }
    if (toNode == null || !toNode.isTextual() || toNode.asText().isBlank()) {
      throw new IllegalArgumentException(
        "'to' must be a non-empty string (fully qualified type name) for type '" + TYPE_TYPE_REFERENCE + "'");
    }

    return TypeReferenceRefactor.builder()
      .fromType(fromNode.asText())
      .toType(toNode.asText())
      .build();
  }

  private static boolean isNullOrBlank(String s) {
    return s == null || s.isBlank();
  }
}
