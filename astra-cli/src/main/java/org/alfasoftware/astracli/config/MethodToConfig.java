package org.alfasoftware.astracli.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code to} block for a {@code methodInvocation} refactor entry,
 * describing the desired state after the refactor. At least one field must be set.
 *
 * <pre>
 * to:
 *   qualifiedClass: com.example.NewClass  # optional — changes the owning type
 *   method: newMethod                      # optional — renames the method
 * </pre>
 */
public class MethodToConfig {

  @JsonProperty("qualifiedClass")
  private String qualifiedClass;

  @JsonProperty("method")
  private String method;

  public String getQualifiedClass() {
    return qualifiedClass;
  }

  public void setQualifiedClass(String qualifiedClass) {
    this.qualifiedClass = qualifiedClass;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }
}
