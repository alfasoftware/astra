package org.alfasoftware.astracli.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single refactoring operation entry within a {@link RefactorConfig}.
 *
 * <p>The {@code type} discriminator controls how {@code from} and {@code to} are interpreted:
 * <ul>
 *   <li>{@code typeReference} — {@code from} and {@code to} are plain strings (fully qualified type names)</li>
 *   <li>{@code methodInvocation} — {@code from} and {@code to} are objects (see {@link MethodFromConfig} / {@link MethodToConfig})</li>
 * </ul>
 */
public class RefactorEntry {

  @JsonProperty("type")
  private String type;

  /**
   * For {@code typeReference}: a plain string FQN.
   * For {@code methodInvocation}: an object with {@code qualifiedClass}, {@code method}, etc.
   */
  @JsonProperty("from")
  private JsonNode from;

  /**
   * For {@code typeReference}: a plain string FQN.
   * For {@code methodInvocation}: an object with optional {@code qualifiedClass} and/or {@code method}.
   */
  @JsonProperty("to")
  private JsonNode to;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public JsonNode getFrom() {
    return from;
  }

  public void setFrom(JsonNode from) {
    this.from = from;
  }

  public JsonNode getTo() {
    return to;
  }

  public void setTo(JsonNode to) {
    this.to = to;
  }
}
