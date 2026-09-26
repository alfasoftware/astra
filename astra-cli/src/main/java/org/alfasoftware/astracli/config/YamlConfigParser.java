package org.alfasoftware.astracli.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Parses an astra YAML refactor configuration file into a {@link RefactorConfig}.
 *
 * <p>Accepts a {@link File}, a raw YAML {@link String}, or an {@link InputStream}.
 * Throws {@link IOException} on malformed YAML or if the file cannot be read.
 */
public class YamlConfigParser {

  private final ObjectMapper mapper;

  public YamlConfigParser() {
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  public RefactorConfig parse(File configFile) throws IOException {
    return mapper.readValue(configFile, RefactorConfig.class);
  }

  public RefactorConfig parse(String yaml) throws IOException {
    return mapper.readValue(yaml, RefactorConfig.class);
  }

  public RefactorConfig parse(InputStream stream) throws IOException {
    return mapper.readValue(stream, RefactorConfig.class);
  }
}
