package org.alfasoftware.astracli.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.types.TypeReferenceRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.junit.Test;

public class TestAstraOperationFactory {

  private final YamlConfigParser parser = new YamlConfigParser();
  private final AstraOperationFactory factory = new AstraOperationFactory();

  // ─── typeReference ─────────────────────────────────────────────────────────

  @Test
  public void typeReferenceYamlProducesTypeReferenceRefactor() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from: com.example.OldType\n" +
      "    to:   com.example.NewType\n";

    List<ASTOperation> ops = factory.createOperations(parser.parse(yaml));

    assertEquals(1, ops.size());
    assertTrue(ops.get(0) instanceof TypeReferenceRefactor);

    TypeReferenceRefactor refactor = (TypeReferenceRefactor) ops.get(0);
    assertEquals("com.example.OldType", refactor.getFromType());
  }

  // ─── methodInvocation (rename only) ───────────────────────────────────────

  @Test
  public void methodInvocationRenameOnlyProducesCorrectMatcher() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "      method: oldMethod\n" +
      "    to:\n" +
      "      method: newMethod\n";

    List<ASTOperation> ops = factory.createOperations(parser.parse(yaml));

    assertEquals(1, ops.size());
    assertTrue(ops.get(0) instanceof MethodInvocationRefactor);

    MethodInvocationRefactor refactor = (MethodInvocationRefactor) ops.get(0);
    assertEquals(Optional.of("com.example.OldClass"),
      refactor.getBeforeMatcher().getFullyQualifiedDeclaringTypeExactName());
    assertEquals(Optional.of("oldMethod"),
      refactor.getBeforeMatcher().getMethodNameExactName());
    assertTrue("No parameters expected", refactor.getBeforeMatcher().getFullyQualifiedParameterNames().isEmpty());
  }

  // ─── methodInvocation (with parameters) ───────────────────────────────────

  @Test
  public void methodInvocationWithParametersParsesParameterList() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.Svc\n" +
      "      method: process\n" +
      "      parameters:\n" +
      "        - java.lang.String\n" +
      "        - int\n" +
      "    to:\n" +
      "      method: handle\n";

    List<ASTOperation> ops = factory.createOperations(parser.parse(yaml));

    MethodInvocationRefactor refactor = (MethodInvocationRefactor) ops.get(0);
    Optional<List<String>> params = refactor.getBeforeMatcher().getFullyQualifiedParameterNames();
    assertTrue(params.isPresent());
    assertEquals(List.of("java.lang.String", "int"), params.get());
  }

  // ─── methodInvocation (change type) ───────────────────────────────────────

  @Test
  public void methodInvocationChangeTypeProducesOperation() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldSvc\n" +
      "      method: doWork\n" +
      "    to:\n" +
      "      qualifiedClass: com.example.NewSvc\n";

    List<ASTOperation> ops = factory.createOperations(parser.parse(yaml));

    assertEquals(1, ops.size());
    assertTrue(ops.get(0) instanceof MethodInvocationRefactor);
  }

  // ─── multiple operations ───────────────────────────────────────────────────

  @Test
  public void multipleEntriesProduceMultipleOperations() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from: com.example.A\n" +
      "    to:   com.example.B\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.C\n" +
      "      method: foo\n" +
      "    to:\n" +
      "      method: bar\n" +
      "  - type: typeReference\n" +
      "    from: com.example.X\n" +
      "    to:   com.example.Y\n";

    List<ASTOperation> ops = factory.createOperations(parser.parse(yaml));

    assertEquals(3, ops.size());
    assertTrue(ops.get(0) instanceof TypeReferenceRefactor);
    assertTrue(ops.get(1) instanceof MethodInvocationRefactor);
    assertTrue(ops.get(2) instanceof TypeReferenceRefactor);
  }

  // ─── error: null / empty config ───────────────────────────────────────────

  @Test(expected = IllegalArgumentException.class)
  public void nullConfigThrowsIllegalArgumentException() {
    factory.createOperations(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyRefactorsListThrowsIllegalArgumentException() throws IOException {
    factory.createOperations(parser.parse("refactors: []\n"));
  }

  // ─── error: unknown type ───────────────────────────────────────────────────

  @Test
  public void unknownTypeThrowsIllegalArgumentExceptionWithUsefulMessage() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: unknownOperation\n" +
      "    from: com.example.A\n" +
      "    to:   com.example.B\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue("Message should mention the unknown type",
        e.getMessage().contains("unknownOperation"));
      assertTrue("Message should mention valid types",
        e.getMessage().contains(AstraOperationFactory.TYPE_METHOD_INVOCATION));
    }
  }

  // ─── error: missing type field ────────────────────────────────────────────

  @Test
  public void missingTypeFieldThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - from: com.example.OldType\n" +
      "    to:   com.example.NewType\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("'type' is required"));
    }
  }

  // ─── error: methodInvocation with string 'from' ───────────────────────────

  @Test
  public void methodInvocationWithStringFromThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from: com.example.OldClass\n" +
      "    to:\n" +
      "      method: newMethod\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue("Message should mention that 'from' must be an object",
        e.getMessage().contains("mapping") || e.getMessage().contains("object"));
    }
  }

  // ─── error: methodInvocation missing qualifiedClass ───────────────────────

  @Test
  public void methodInvocationMissingQualifiedClassThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      method: oldMethod\n" +
      "    to:\n" +
      "      method: newMethod\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("qualifiedClass"));
    }
  }

  // ─── error: methodInvocation missing method name ──────────────────────────

  @Test
  public void methodInvocationMissingMethodNameThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "    to:\n" +
      "      method: newMethod\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("from.method"));
    }
  }

  // ─── error: methodInvocation with empty 'to' ──────────────────────────────

  @Test
  public void methodInvocationWithEmptyToThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "      method: oldMethod\n" +
      "    to: {}\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue("Message should mention 'to.method' or 'to.qualifiedClass'",
        e.getMessage().contains("to.method") || e.getMessage().contains("to.qualifiedClass"));
    }
  }

  // ─── error: typeReference with object 'from' ──────────────────────────────

  @Test
  public void typeReferenceWithObjectFromThrowsIllegalArgumentException() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "    to: com.example.NewType\n";

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue("Message should mention 'from' must be a string",
        e.getMessage().contains("string") || e.getMessage().contains("non-empty"));
    }
  }

  // ─── error message includes entry index ───────────────────────────────────

  @Test
  public void errorMessageIncludesEntryIndex() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from: com.example.OldType\n" +
      "    to:   com.example.NewType\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.Svc\n" +
      "      method: doIt\n" +
      "    to: {}\n";          // invalid: empty to

    try {
      factory.createOperations(parser.parse(yaml));
      org.junit.Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue("Error message should mention entry index 1",
        e.getMessage().contains("entry 1"));
    }
  }
}
