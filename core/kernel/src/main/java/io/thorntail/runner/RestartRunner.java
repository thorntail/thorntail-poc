package io.thorntail.runner;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.thorntail.DevMode;
import io.thorntail.impl.KernelMessages;

/**
 * Created by bob on 4/3/18.
 */
public class RestartRunner extends AbstractForkedRunner {

    private static final String JAVA_CLASS_PATH_PROPERTY_NAME = "java.class.path";

    @Override
    public void run() throws Exception {
        KernelMessages.MESSAGES.usingDevMode(DevMode.RESTART);
        KernelMessages.MESSAGES.restartEnabled();

        ProcessBuilder builder = new ProcessBuilder();

        builder.environment().remove(DevMode.ENVIRONMENT_VAR_NAME);
        builder.command(command());
        builder.inheritIO();
        startWatchdog(builder);
    }

    private void startWatchdog(ProcessBuilder builder) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        boolean sensitiveAvailable = false;
        try {
            Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
            sensitiveAvailable = true;
            KernelMessages.MESSAGES.highSensitiveFileWatching();
        } catch (ClassNotFoundException e) {
            // ignore
        }

        boolean finalSensitiveAvailable = sensitiveAvailable;

        pathsToWatch().forEach(p -> {
            try {
                if (finalSensitiveAvailable) {
                    watch(p, (path) -> {
                        path.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);
                    });
                } else {
                    watch(p, (path) -> {
                        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    });
                }
                KernelMessages.MESSAGES.watchingDirectory(p.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        new Thread(() -> {
            long lastChange = 0;
            Process process = null;
            while (true) {
                try {
                    if (process == null) {
                        KernelMessages.MESSAGES.launchingChildProcess();
                        process = builder.start();
                    }
                    WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        if (lastChange == 0) {
                            continue;
                        }
                        if (System.currentTimeMillis() - lastChange > 500) {
                            KernelMessages.MESSAGES.destroyingChildProcess();
                            process.destroy();
                            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                                KernelMessages.MESSAGES.destroyingChildProcessForcibly();
                                process.destroyForcibly();
                            }
                            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                                KernelMessages.MESSAGES.childProcessDidNotExit();
                                break;
                            }
                            process = null;
                            lastChange = 0;
                        }
                        continue;
                    }
                    lastChange = System.currentTimeMillis();
                    KernelMessages.MESSAGES.changeDetected(key.watchable().toString());
                    int totalEvents = 0;
                    for (WatchEvent<?> watchEvent : key.pollEvents()) {
                        totalEvents += watchEvent.count();
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    static interface PathConsumer {
        void accept(Path p) throws IOException;

    }

    void watch(Path root, PathConsumer consumer) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.err.println("watch: " + dir);
                consumer.accept(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    private static List<Path> pathsToWatch() {
        return Arrays.asList(System.getProperty(JAVA_CLASS_PATH_PROPERTY_NAME).split(File.pathSeparator)).stream()
                .map(e -> Paths.get(e))
                .map(e -> Files.isDirectory(e) ? e : e.getParent())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> extraJvmArgs() {
        return Collections.singletonList(debug());
    }

}
