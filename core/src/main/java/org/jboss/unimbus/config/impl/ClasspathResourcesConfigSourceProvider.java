package org.jboss.unimbus.config.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

abstract class ClasspathResourcesConfigSourceProvider implements ConfigSourceProvider {

    ClasspathResourcesConfigSourceProvider(String path, int defaultOrdinal) {
        this.path = path;
        this.defaultOrdinal = defaultOrdinal;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        List<ConfigSource> sources = new ArrayList<>();

        try {
            sources.addAll(process(classLoader));
            if (Thread.currentThread().getContextClassLoader() != classLoader) {
                sources.addAll(process(Thread.currentThread().getContextClassLoader()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sources;
    }

    List<ConfigSource> process(ClassLoader classLoader) throws IOException {
        List<ConfigSource> sources = new ArrayList<>();
        Enumeration<URL> resources = classLoader.getResources(this.path);
        while (resources.hasMoreElements()) {
            ConfigSource source = process(resources.nextElement());
            if (source != null) {
                sources.add(source);
            }
        }
        return sources;
    }

    ConfigSource process(URL url) throws IOException {
        if (url.getPath().endsWith(".properties")) {
            return PropertiesConfigSource.of(url, this.defaultOrdinal);
        } else if (url.getPath().endsWith(".yaml") || url.getPath().endsWith(".yml")) {
            return YamlConfigSource.of(url, this.defaultOrdinal);
        }
        return null;
    }

    private final String path;

    private final int defaultOrdinal;
}