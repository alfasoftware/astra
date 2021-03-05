package org.alfasoftware.astracli.commandline;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * This example shows how astra could be implemented using picocli.
 * <p>
 * Invoking the {@code astra} command without a subcommand shows the usage help.
 * </p>
 */
@Command(name = "astra",
        mixinStandardHelpOptions = true,
        versionProvider = AstraCli.ManifestVersionProvider.class,
        description = "Astra is a tool for refactoring and analysis of source code.",
        commandListHeading = "%nCommands:%n%nThe most commonly used astra commands are:%n",
        footer = "%nSee 'astra help <command>' to read about a specific subcommand or concept.",
        subcommands = {
                AstraMethodInvocation.class,
                AstraChangeType.class,
                CommandLine.HelpCommand.class
        })
public class AstraCli implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // if the command was invoked without subcommand, show the usage help
        spec.commandLine().usage(System.err);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new AstraCli()).execute(args));
    }

    /**
     * {@link IVersionProvider} implementation that returns version information from the astra-cli.jar file's {@code /META-INF/MANIFEST.MF} file.
     */
    static class ManifestVersionProvider implements IVersionProvider {
      @Override
      public String[] getVersion() throws Exception {
        Enumeration<URL> resources = CommandLine.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
          URL url = resources.nextElement();
          try {
            Manifest manifest = new Manifest(url.openStream());
            if (isApplicableManifest(manifest)) {
              Attributes attr = manifest.getMainAttributes();
              return new String[] { "Astra CLI version " + get(attr, "Implementation-Version")};
            }
          } catch (IOException ex) {
            return new String[] { "Unable to read from " + url + ": " + ex };
          }
        }
        return new String[0];
      }

      private boolean isApplicableManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        return "astra-cli".equals(get(attributes, "Implementation-Title"));
      }

      private static Object get(Attributes attributes, String key) {
        return attributes.get(new Attributes.Name(key));
      }
    }
}
