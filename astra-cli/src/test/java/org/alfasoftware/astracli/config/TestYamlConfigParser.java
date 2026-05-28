package org.alfasoftware.astracli.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;

public class TestYamlConfigParser {

  private final YamlConfigParser parser = new YamlConfigParser();

  // ─── typeReference ─────────────────────────────────────────────────────────

  @Test
  public void typeReferenceEntryParsesFromAndToAsStrings() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from: com.example.OldType\n" +
      "    to:   com.example.NewType\n";

    RefactorConfig config = parser.parse(yaml);

    assertNotNull(config.getRefactors());
    assertEquals(1, config.getRefactors().size());

    RefactorEntry entry = config.getRefactors().get(0);
    assertEquals("typeReference", entry.getType());
    assertTrue("'from' should be a text node", entry.getFrom().isTextual());
    assertEquals("com.example.OldType", entry.getFrom().asText());
    assertTrue("'to' should be a text node", entry.getTo().isTextual());
    assertEquals("com.example.NewType", entry.getTo().asText());
  }

  // ─── methodInvocation (minimal) ────────────────────────────────────────────

  @Test
  public void methodInvocationEntryParsesFromAndToAsObjects() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "      method: oldMethod\n" +
      "    to:\n" +
      "      method: newMethod\n";

    RefactorConfig config = parser.parse(yaml);

    RefactorEntry entry = config.getRefactors().get(0);
    assertEquals("methodInvocation", entry.getType());
    assertTrue("'from' should be an object node", entry.getFrom().isObject());
    assertTrue("'to' should be an object node", entry.getTo().isObject());

    MethodFromConfig from = toMethodFrom(entry);
    assertEquals("com.example.OldClass", from.getQualifiedClass());
    assertEquals("oldMethod", from.getMethod());
    assertNull(from.getParameters());
    assertNull(from.getVarargs());

    MethodToConfig to = toMethodTo(entry);
    assertEquals("newMethod", to.getMethod());
    assertNull(to.getQualifiedClass());
  }

  // ─── methodInvocation (with parameters and varargs) ───────────────────────

  @Test
  public void methodInvocationWithParametersAndVarargsParsesCorrectly() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.OldClass\n" +
      "      method: oldMethod\n" +
      "      parameters:\n" +
      "        - java.lang.String\n" +
      "        - int\n" +
      "      isVarargs: true\n" +
      "    to:\n" +
      "      qualifiedClass: com.example.NewClass\n" +
      "      method: newMethod\n";

    RefactorConfig config = parser.parse(yaml);
    MethodFromConfig from = toMethodFrom(config.getRefactors().get(0));

    List<String> params = from.getParameters();
    assertNotNull(params);
    assertEquals(2, params.size());
    assertEquals("java.lang.String", params.get(0));
    assertEquals("int", params.get(1));
    assertEquals(Boolean.TRUE, from.getVarargs());

    MethodToConfig to = toMethodTo(config.getRefactors().get(0));
    assertEquals("com.example.NewClass", to.getQualifiedClass());
    assertEquals("newMethod", to.getMethod());
  }

  // ─── multiple entries ──────────────────────────────────────────────────────

  @Test
  public void multipleEntriesOfMixedTypesParse() throws IOException {
    String yaml =
      "refactors:\n" +
      "  - type: typeReference\n" +
      "    from: com.example.OldType\n" +
      "    to:   com.example.NewType\n" +
      "  - type: methodInvocation\n" +
      "    from:\n" +
      "      qualifiedClass: com.example.Svc\n" +
      "      method: doIt\n" +
      "    to:\n" +
      "      method: doItBetter\n" +
      "  - type: typeReference\n" +
      "    from: com.example.AnotherOld\n" +
      "    to:   com.example.AnotherNew\n";

    RefactorConfig config = parser.parse(yaml);

    assertEquals(3, config.getRefactors().size());
    assertEquals("typeReference",     config.getRefactors().get(0).getType());
    assertEquals("methodInvocation",  config.getRefactors().get(1).getType());
    assertEquals("typeReference",     config.getRefactors().get(2).getType());
  }

  // ─── empty / null refactors list ──────────────────────────────────────────

  @Test
  public void emptyRefactorsListParsesToEmptyList() throws IOException {
    String yaml = "refactors: []\n";

    RefactorConfig config = parser.parse(yaml);

    assertNotNull(config.getRefactors());
    assertTrue(config.getRefactors().isEmpty());
  }

  @Test
  public void missingRefactorsKeyParsesToNullList() throws IOException {
    String yaml = "{}";

    RefactorConfig config = parser.parse(yaml);

    assertNull(config.getRefactors());
  }

  // ─── invalid YAML ──────────────────────────────────────────────────────────

  @Test(expected = IOException.class)
  public void malformedYamlThrowsIOException() throws IOException {
    // Tabs in YAML are illegal
    parser.parse("refactors:\n\t- type: typeReference\n");
  }

  // ─── helpers ───────────────────────────────────────────────────────────────

  private MethodFromConfig toMethodFrom(RefactorEntry entry) {
    return new com.fasterxml.jackson.databind.ObjectMapper()
      .convertValue(entry.getFrom(), MethodFromConfig.class);
  }

  private MethodToConfig toMethodTo(RefactorEntry entry) {
    return new com.fasterxml.jackson.databind.ObjectMapper()
      .convertValue(entry.getTo(), MethodToConfig.class);
  }
}
