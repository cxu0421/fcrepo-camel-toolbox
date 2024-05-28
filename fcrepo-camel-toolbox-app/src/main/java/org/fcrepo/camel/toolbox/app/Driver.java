package org.fcrepo.camel.toolbox.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import static org.fcrepo.camel.common.config.BasePropsConfig.FCREPO_CAMEL_CONFIG_FILE_PROPERTY;

@CommandLine.Command(name = "fcrepo-camel-toolbox",
        mixinStandardHelpOptions = true, sortOptions = false,
        versionProvider = AppVersionProvider.class)
public class Driver implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Driver.class);

    @CommandLine.Option(names = {"--config", "-c"}, required = false, order = 1,
            description = "The path to the configuration file")
    private Path configurationFilePath;

    @Override
    public Integer call() {
        if (configurationFilePath != null) {
            System.setProperty(FCREPO_CAMEL_CONFIG_FILE_PROPERTY, configurationFilePath.toFile().getAbsolutePath());
        }
        AnnotationConfigApplicationContext appContext = new AnnotationConfigApplicationContext("org.fcrepo.camel");
        appContext.registerShutdownHook();
        appContext.start();
        LOGGER.info("fcrepo-camel-toolbox started.");

        while (appContext.isRunning()) {
            Thread.onSpinWait();
        }
        return 0;
    }

    public static void main(final String[] args) {
        Driver driver = new Driver();
        CommandLine cmd = new CommandLine(driver);
        cmd.setExecutionExceptionHandler(new AppExceptionHandler(driver));
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    private static class AppExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        private final Driver driver;

        AppExceptionHandler(final Driver driver) {
            this.driver = driver;
        }

        @Override
        public int handleExecutionException(
                final Exception ex,
                final CommandLine commandLine,
                final CommandLine.ParseResult parseResult) {
            LOGGER.error("Error during command execution: ", ex);
            commandLine.getErr().println(ex.getMessage());
            ex.printStackTrace(commandLine.getErr());
            commandLine.usage(commandLine.getErr());
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }
}
