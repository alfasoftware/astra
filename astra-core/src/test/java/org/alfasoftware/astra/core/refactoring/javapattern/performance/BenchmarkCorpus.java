package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.alfasoftware.astra.core.refactoring.javapattern.performance.lib.LegacyCache;
import org.alfasoftware.astra.core.refactoring.javapattern.performance.lib.Money;
import org.alfasoftware.astra.core.refactoring.javapattern.performance.lib.ServiceRegistry;

/**
 * Generates a deterministic synthetic codebase for {@link TestJavaPatternPerformance}.
 *
 * <p>Every file is a class whose methods are made of everyday JDK code. That code contains near misses for the
 * benchmark patterns (invocations with the same method names, but a different shape or types), but no matches.
 * In addition:
 * <ul>
 *   <li>every 10th file uses the {@link LegacyCache} and {@link Money} APIs migrated by the "api-migration" patterns,</li>
 *   <li>every 10th file, offset by 5, uses {@link ServiceRegistry#lookup(String)}, which shares a method name with
 *       {@link LegacyCacheLookupPattern} but must not be changed by it,</li>
 *   <li>every 5th file, offset by 2, uses the idioms replaced by the "jdk-idioms" patterns.</li>
 * </ul>
 */
final class BenchmarkCorpus {

  private static final String LIB_PACKAGE = LegacyCache.class.getPackageName();
  private static final String PACKAGE = "bench.app";
  private static final int METHODS_PER_FILE = 12;
  private static final int SNIPPETS_PER_METHOD = 5;
  private static final long SEED = 20261003L;

  /**
   * Ordinary code. None of it is matched by the benchmark patterns.
   */
  private static final String[][] COMMON_SNIPPETS = {
      { "List<String> parts = new ArrayList<>();",
        "for (int i = 0; i < count; i++) {",
        "  parts.add(text + i);",
        "}",
        "names.addAll(parts);" },
      { "counts.put(key, counts.getOrDefault(key, 0) + 1);" },
      { "StringBuilder builder = new StringBuilder();",
        "builder.append(key).append(':').append(count);",
        "state = builder.toString().trim();" },
      { "if (text != null && text.startsWith(key)) {",
        "  state = text.substring(key.length());",
        "}" },
      { "Optional<String> first = names.stream().filter(name -> name.contains(key)).findFirst();",
        "state = first.orElse(\"none\");" },
      { "String upper = text.toUpperCase(Locale.ROOT);",
        "consume(upper.equals(key));" },
      { "Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2, 3));",
        "if (ids.contains(count)) {",
        "  ids.remove(count);",
        "}" },
      { "int total = 0;",
        "for (Integer value : counts.values()) {",
        "  total += value;",
        "}",
        "consume(String.valueOf(total));" },
      { "Objects.requireNonNull(text, \"text\");",
        "state = text.trim();" },
      { "List<String> copy = new ArrayList<>(names);",
        "Collections.sort(copy);",
        "state = String.join(\",\", copy);" },
      { "if (text.length() > count) {",
        "  consume(text.charAt(0));",
        "}" },
      { "Map<String, List<String>> groups = new HashMap<>();",
        "groups.computeIfAbsent(key, k -> new ArrayList<>()).add(text);",
        "consume(groups.size());" },
      { "for (Map.Entry<String, Integer> entry : counts.entrySet()) {",
        "  if (entry.getValue() > count) {",
        "    names.add(entry.getKey());",
        "  }",
        "}" },
      { "String[] tokens = text.split(\",\");",
        "consume(tokens.length == 0 ? key : tokens[0]);" },
      { "boolean known = counts.containsKey(key) && !names.isEmpty();",
        "consume(known);" },
  };

  /**
   * Matched by {@link LegacyCacheLookupPattern}, {@link MoneyConstructorPattern} and {@link LegacyCacheStorePattern}.
   */
  private static final String[][] API_MIGRATION_MATCHES = {
      { "String cached = cache.lookup(key);",
        "consume(cached);" },
      { "Money price = new Money(count, \"EUR\");",
        "consume(price);" },
      { "cache.store(key, String.valueOf(text));" },
  };

  /**
   * Uses of the legacy APIs which the "api-migration" patterns must not change.
   */
  private static final String[][] API_MIGRATION_NEAR_MISSES = {
      { "consume(cache.get(key));" },
      { "cache.put(key, text);" },
      { "cache.store(key, text);" },
      { "consume(Money.of(count, \"USD\"));" },
  };

  private static final String[][] REGISTRY_NEAR_MISSES = {
      { "consume(registry.lookup(key));" },
  };

  /**
   * Matched by {@link StringIsEmptyPattern} and {@link MapContainsKeyPattern}.
   */
  private static final String[][] JDK_IDIOM_MATCHES = {
      { "if (text.length() == 0) {",
        "  return;",
        "}" },
      { "consume(key.equals(\"\"));" },
      { "consume(counts.keySet().contains(key));" },
  };

  /** Relative path to file content, sorted by path. */
  private final Map<String, String> files;
  private final int apiMigrationFileCount;
  private final int jdkIdiomFileCount;


  private BenchmarkCorpus(Map<String, String> files, int apiMigrationFileCount, int jdkIdiomFileCount) {
    this.files = files;
    this.apiMigrationFileCount = apiMigrationFileCount;
    this.jdkIdiomFileCount = jdkIdiomFileCount;
  }


  static BenchmarkCorpus generate(int fileCount) {
    Random random = new Random(SEED);
    Map<String, String> files = new TreeMap<>();
    int apiMigrationFileCount = 0;
    int jdkIdiomFileCount = 0;
    for (int index = 0; index < fileCount; index++) {
      boolean usesLegacyApi = index % 10 == 0;
      boolean usesRegistry = index % 10 == 5;
      boolean usesJdkIdioms = index % 5 == 2;

      List<List<String[]>> methods = new ArrayList<>();
      for (int m = 0; m < METHODS_PER_FILE; m++) {
        List<String[]> snippets = new ArrayList<>();
        for (int s = 0; s < SNIPPETS_PER_METHOD; s++) {
          snippets.add(COMMON_SNIPPETS[random.nextInt(COMMON_SNIPPETS.length)]);
        }
        methods.add(snippets);
      }
      if (usesLegacyApi) {
        insertRandomly(methods, API_MIGRATION_MATCHES, 2, random);
        insertRandomly(methods, API_MIGRATION_NEAR_MISSES, 1, random);
        apiMigrationFileCount++;
      }
      if (usesRegistry) {
        insertRandomly(methods, REGISTRY_NEAR_MISSES, 2, random);
      }
      if (usesJdkIdioms) {
        insertRandomly(methods, JDK_IDIOM_MATCHES, 2, random);
        jdkIdiomFileCount++;
      }

      String className = String.format("Component%04d", index);
      files.put(PACKAGE.replace('.', '/') + "/" + className + ".java",
          renderClass(className, methods, usesLegacyApi, usesRegistry));
    }
    return new BenchmarkCorpus(files, apiMigrationFileCount, jdkIdiomFileCount);
  }


  private static void insertRandomly(List<List<String[]>> methods, String[][] snippets, int times, Random random) {
    for (int i = 0; i < times; i++) {
      for (String[] snippet : snippets) {
        List<String[]> method = methods.get(random.nextInt(methods.size()));
        method.add(random.nextInt(method.size() + 1), snippet);
      }
    }
  }


  private static String renderClass(String className, List<List<String[]>> methods, boolean usesLegacyApi, boolean usesRegistry) {
    StringBuilder source = new StringBuilder();
    source.append("package ").append(PACKAGE).append(";\n\n");
    for (String type : Arrays.asList("ArrayList", "Arrays", "Collections", "HashMap", "HashSet", "List", "Locale", "Map",
        "Objects", "Optional", "Set")) {
      source.append("import java.util.").append(type).append(";\n");
    }
    source.append("\n");
    if (usesLegacyApi) {
      source.append("import ").append(LIB_PACKAGE).append(".LegacyCache;\n");
      source.append("import ").append(LIB_PACKAGE).append(".Money;\n");
    }
    if (usesRegistry) {
      source.append("import ").append(LIB_PACKAGE).append(".ServiceRegistry;\n");
    }
    source.append("\n");
    source.append("public class ").append(className).append(" {\n\n");
    source.append("  private final List<String> names = new ArrayList<>();\n");
    source.append("  private final Map<String, Integer> counts = new HashMap<>();\n");
    if (usesLegacyApi) {
      source.append("  private final LegacyCache<String, String> cache = new LegacyCache<>();\n");
    }
    if (usesRegistry) {
      source.append("  private final ServiceRegistry registry = new ServiceRegistry();\n");
    }
    source.append("  private String state = \"\";\n");

    for (int m = 0; m < methods.size(); m++) {
      source.append("\n  public void process").append(m).append("(String text, String key, int count) {\n");
      for (String[] snippet : methods.get(m)) {
        // each snippet gets its own block, so that the local variables of snippets used more than once don't clash
        source.append("    {\n");
        for (String line : snippet) {
          source.append("      ").append(line).append("\n");
        }
        source.append("    }\n");
      }
      source.append("  }\n");
    }

    source.append("\n  private static void consume(Object value) {\n");
    source.append("    // only the shape of the code matters to the benchmark\n");
    source.append("  }\n");
    source.append("}\n");
    return source.toString();
  }


  /**
   * Writes (or re-writes) every file of the corpus into the given directory.
   */
  void writeTo(Path directory) throws IOException {
    for (Map.Entry<String, String> file : files.entrySet()) {
      Path path = directory.resolve(file.getKey());
      Files.createDirectories(path.getParent());
      Files.writeString(path, file.getValue(), StandardCharsets.UTF_8);
    }
  }


  /**
   * @return the current content of the corpus files in the given directory, keyed by relative path
   */
  Map<String, String> readFrom(Path directory) throws IOException {
    Map<String, String> content = new TreeMap<>();
    for (String relativePath : files.keySet()) {
      content.put(relativePath, Files.readString(directory.resolve(relativePath), StandardCharsets.UTF_8));
    }
    return content;
  }


  Map<String, String> getFiles() {
    return files;
  }


  int getFileCount() {
    return files.size();
  }


  /**
   * @return the number of files which the "api-migration" patterns should change
   */
  int getApiMigrationFileCount() {
    return apiMigrationFileCount;
  }


  /**
   * @return the number of files which the "jdk-idioms" patterns should change
   */
  int getJdkIdiomFileCount() {
    return jdkIdiomFileCount;
  }


  /**
   * @return a SHA-256 digest over the given files' paths and content, to compare the output of different runs
   */
  static String digest(Map<String, String> content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (Map.Entry<String, String> file : new TreeMap<>(content).entrySet()) {
        digest.update(file.getKey().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(file.getValue().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
