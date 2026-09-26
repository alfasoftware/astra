package org.alfasoftware.astracli.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code from} block for a {@code methodInvocation} refactor entry,
 * describing the method invocation to match.
 *
 * <pre>
 * from:
 *   qualifiedClass: com.example.OldClass
 *   method: oldMethod
 *   parameters:         # optional; omit to match any parameter list
 *     - java.lang.String
 *     - int
 *   isVarargs: false    # optional; omit to match regardless of varargs
 * </pre>
 */
public class MethodFromConfig {

  @JsonProperty("qualifiedClass")
  private String qualifiedClass;

  @JsonProperty("method")
  private String method;

  @JsonProperty("parameters")
  private List<String> parameters;

  /** When {@code null} the varargs constraint is not applied to the match. */
  @JsonProperty("isVarargs")
  private Boolean varargs;

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

  public List<String> getParameters() {
    return parameters;
  }

  public void setParameters(List<String> parameters) {
    this.parameters = parameters;
  }

  public Boolean getVarargs() {
    return varargs;
  }

  public void setVarargs(Boolean varargs) {
    this.varargs = varargs;
  }
}
