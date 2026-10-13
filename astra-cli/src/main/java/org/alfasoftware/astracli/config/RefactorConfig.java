package org.alfasoftware.astracli.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level model for an astra YAML refactor configuration file.
 *
 * <pre>
 * refactors:
 *   - type: typeReference
 *     from: com.example.OldType
 *     to:   com.example.NewType
 *   - type: methodInvocation
 *     from:
 *       qualifiedClass: com.example.OldClass
 *       method: oldMethod
 *     to:
 *       method: newMethod
 * </pre>
 */
public class RefactorConfig {

  @JsonProperty("refactors")
  private List<RefactorEntry> refactors;

  public List<RefactorEntry> getRefactors() {
    return refactors;
  }

  public void setRefactors(List<RefactorEntry> refactors) {
    this.refactors = refactors;
  }
}
