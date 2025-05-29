package dst.ass2.aop.impl;

import dst.ass2.aop.IPluginExecutable;
import dst.ass2.aop.IPluginExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginExecutor implements IPluginExecutor {
    private final Set<File> directories = ConcurrentHashMap.newKeySet();
    private final Map<WatchKey, File> watchFileMap = new ConcurrentHashMap<>();
    private final Map<File, WatchKey> watchKeyMap = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private WatchService watchService;
    private Thread watchThread;
    private final Set<File> recentlyProcessed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean running = false;

    public PluginExecutor() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void monitor(File dir) {
        directories.add(dir);
        if (running) {
            registerDirectory(dir);
        }
    }

    @Override
    public void stopMonitoring(File dir) {
        directories.remove(dir);
        WatchKey key = watchKeyMap.remove(dir);
        if (key != null) {
            watchFileMap.remove(key);
            key.cancel();
        }
    }

    @Override
    public void start() {
        running = true;

        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (File dir : directories) {
                registerDirectory(dir);
                scanDirectory(dir);
            }

            watchThread = new Thread(this::processWatchEvents, "PluginWatcherThread");
            watchThread.start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {}

        if (watchThread != null) watchThread.interrupt();

        threadPool.shutdownNow();
    }

    private void registerDirectory(File dir) {
        try {
            WatchKey key = dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            watchKeyMap.put(dir, key);
            watchFileMap.put(key, dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register directory: " + dir.getAbsolutePath(), e);
        }
    }

    private void scanDirectory(File dir) {
        File[] jars = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                threadPool.execute(() -> {
                    try {
                        loadAndRunPlugin(jar);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void processWatchEvents () {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take(); // blocking
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            File dir = watchFileMap.get(key);
            if (dir == null) continue;

            for (WatchEvent<?> event : key.pollEvents()) {
                File file = new File(dir, event.context().toString());
                if (!file.getName().endsWith(".jar")) continue;

                //avoid duplicate fires
                if (recentlyProcessed.contains(file)) continue;
                recentlyProcessed.add(file);

                threadPool.execute(() -> {
                    try {
                        loadAndRunPlugin(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            key.reset();
        }
    }

    private void loadAndRunPlugin(File file) throws Exception {
        URL[] urls = new URL[] { file.toURI().toURL() };
        URLClassLoader loader = URLClassLoader.newInstance(urls);
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith("class"))
                    continue;

                String className = entry.getName().replace("/", ".").replace(".class", "");
                Class<?> clazz = loader.loadClass(className);

                if (IPluginExecutable.class.isAssignableFrom(clazz)) {
                    IPluginExecutable plugin = (IPluginExecutable) clazz.getDeclaredConstructor().newInstance();
                    threadPool.execute(plugin::execute);
                }
            }
        } finally {
            try {
                loader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}