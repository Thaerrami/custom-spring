package src.framework.core;

import src.framework.annotations.Component;
import src.framework.annotations.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class Container {
    // The "Registry" holds all the live objects (Beans)
    private final Map<Class<?>, Object> beanRegistry = new HashMap<>();

    public void init(String basePackage) throws Exception {
        System.out.println("⚡ NextGenFW Starting...");

        // Step 1: Scan & Instantiate
        List<Class<?>> classes = scanPackage(basePackage);
        for (Class<?> clazz : classes) {
            // Only create instances for classes marked with @Component
            if (clazz.isAnnotationPresent(Component.class)) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                beanRegistry.put(clazz, instance);
                System.out.println("Created Bean: " + clazz.getSimpleName());
            }
        }

        // Step 2: Inject Dependencies
        injectDependencies();

        System.out.println("✅ Framework Initialized.");
    }

    private void injectDependencies() throws IllegalAccessException {
        // Loop through every bean we created
        for (Object bean : beanRegistry.values()) {
            // Look at every field in that bean
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    // It's a dependency! Find the matching bean in our registry.
                    Object dependency = beanRegistry.get(field.getType());

                    if (dependency != null) {
                        // Allow modifying private fields
                        field.setAccessible(true);
                        // Inject the dependency
                        field.set(bean, dependency);
                        System.out.println("   --> Injected " + field.getType().getSimpleName() +
                                " into " + bean.getClass().getSimpleName());
                    } else {
                        throw new RuntimeException("Could not find dependency for field: " + field.getName());
                    }
                }
            }
        }
    }

    // Retrieves a bean for the user
    public <T> T getBean(Class<T> clazz) {
        return clazz.cast(beanRegistry.get(clazz));
    }

    // --- Helper: Scans file system for .class files ---
    // OPTIMIZED: Cache the classloader to avoid repeated lookups
    private ClassLoader classLoader;
    
    private List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        
        // OPTIMIZATION 1: Use cached classloader or get it once
        // Why Thread.currentThread().getContextClassLoader()?
        // - In Java, each thread has a "context classloader" that can be set by frameworks
        // - This allows code running in one classloader context to access classes from another
        // - It's a way to break the parent-first classloading model when needed
        // - Spring Boot uses this pattern for compatibility across different deployment scenarios
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            // Fallback: Use the classloader that loaded this Container class
            if (classLoader == null) {
                classLoader = Container.class.getClassLoader();
            }
        }
        
        // OPTIMIZATION 2: Use getResources() to handle multiple classpath entries (like JARs)
        // This is more robust than getResource() which only returns the first match
        Enumeration<URL> resources = classLoader.getResources(path);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            
            // OPTIMIZATION 3: Handle both file system and JAR file resources
            // Spring Boot needs to work with JAR files, not just directories
            if ("file".equals(resource.getProtocol())) {
                // File system (development/IDE)
                File directory = new File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                    scanDirectory(directory, packageName, classes);
                }
            } else if ("jar".equals(resource.getProtocol()) || "zip".equals(resource.getProtocol())) {
                // JAR file (production)
                scanJarFile(resource, path, packageName, classes);
            }
        }
        
        return classes;
    }
    
    // OPTIMIZATION 4: Extract directory scanning to separate method for clarity
    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded (might be inner classes, etc.)
                    System.err.println("Warning: Could not load class " + className + ": " + e.getMessage());
                }
            }
        }
    }
    
    // OPTIMIZATION 5: Add JAR file scanning support (Spring Boot requirement)
    private void scanJarFile(URL jarUrl, String path, String packageName, List<Class<?>> classes) throws Exception {
        // Extract JAR file path from URL (format: jar:file:/path/to.jar!/package/path)
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
        }
        int separatorIndex = jarPath.indexOf('!');
        if (separatorIndex != -1) {
            jarPath = jarPath.substring(0, separatorIndex);
        }
        
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
            Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            String packagePath = path + "/";
            
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Check if entry is in the target package and is a class file
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    // Convert path to class name
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    // Skip inner classes (they have $ in the name)
                    if (!className.contains("$")) {
                        try {
                            classes.add(Class.forName(className, false, classLoader));
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            // Skip classes that can't be loaded
                            System.err.println("Warning: Could not load class " + className + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}