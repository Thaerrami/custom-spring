package framework.core;

import framework.annotations.Component;
import framework.annotations.Inject;
import framework.annotations.PostConstruct;
import framework.annotations.PreDestroy;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The IoC (Inversion of Control) container — the heart of this framework.
 *
 * <p><b>Spring equivalent:</b> {@code ApplicationContext} / {@code DefaultListableBeanFactory}.
 *
 * <p><b>Startup pipeline</b> (same idea as Spring context refresh):
 * <pre>
 *   1. scanPackage()     — find classes on the classpath
 *   2. registerBean()    — instantiate @Component / @Service / @Repository classes
 *   3. injectDependencies() — wire @Inject fields via reflection
 *   4. @PostConstruct    — run lifecycle hooks (DB connect, schema, etc.)
 * </pre>
 *
 * <p><b>Read next:</b> {@code com.example.demo.Application} shows how to bootstrap the container.
 *
 * @see framework.annotations.Component
 * @see framework.annotations.Inject
 */
public class Container {

    /**
     * Live bean instances keyed by class or interface type.
     * Spring's version: {@code DefaultSingletonBeanRegistry} with {@code BeanDefinition} metadata.
     */
    private final Map<Class<?>, Object> beanRegistry = new HashMap<>();

    /** Cached classloader — avoids repeated lookups during classpath scanning. */
    private ClassLoader classLoader;

    /**
     * Bootstraps the container: scan, create, inject, initialize.
     *
     * @param basePackages one or more root packages to scan (e.g. "framework", "com.example.demo")
     */
    public void init(String... basePackages) throws Exception {
        System.out.println("⚡ CustomSpring Container starting...");

        // --- Phase 1: DISCOVERY — walk the classpath and collect all .class files ---
        Set<Class<?>> candidates = new HashSet<>();
        for (String basePackage : basePackages) {
            candidates.addAll(scanPackage(basePackage));
        }

        // --- Phase 2: REGISTRATION — create instances for stereotype-annotated classes ---
        for (Class<?> clazz : candidates) {
            if (isBeanCandidate(clazz)) {
                registerBean(clazz);
            }
        }

        // --- Phase 3: INJECTION — all beans exist; now wire @Inject fields ---
        injectDependencies();

        // --- Phase 4: INITIALIZATION — @PostConstruct after every field is populated ---
        invokeLifecycle(PostConstruct.class);

        System.out.println("✅ Container initialized (" + beanRegistry.size() + " registrations).");
    }

    /** Graceful shutdown — runs @PreDestroy methods (close DB, release resources). */
    public void shutdown() throws Exception {
        invokeLifecycle(PreDestroy.class);
    }

    /**
     * Service-locator style bean retrieval.
     * Spring usually injects beans instead of calling getBean(), but both work.
     */
    public <T> T getBean(Class<T> clazz) {
        Object bean = beanRegistry.get(clazz);
        if (bean == null) {
            throw new IllegalStateException("No bean registered for type: " + clazz.getName());
        }
        return clazz.cast(bean);
    }

    /**
     * Creates a bean via reflection and registers it by concrete class AND implemented interfaces.
     *
     * <p>Interface registration lets you {@code getBean(UserRepository.class)} even when
     * the concrete class is {@code UserRepositoryImpl}. Spring does the same for interfaces.
     */
    private void registerBean(Class<?> clazz) throws Exception {
        // Requires a public no-arg constructor — Spring also supports constructor injection
        Object instance = clazz.getDeclaredConstructor().newInstance();
        beanRegistry.put(clazz, instance);

        // Register under each interface so @Inject UserRepository finds the impl
        for (Class<?> iface : clazz.getInterfaces()) {
            beanRegistry.putIfAbsent(iface, instance);
        }

        System.out.println("  + Bean: " + clazz.getSimpleName());
    }

    /**
     * Determines if a scanned class should become a bean.
     *
     * <p>Checks direct {@code @Component} OR meta-annotations (@Service, @Repository carry @Component).
     */
    private boolean isBeanCandidate(Class<?> clazz) {
        if (clazz.isInterface() || clazz.isAnnotation()) {
            return false;
        }
        if (clazz.isAnnotationPresent(Component.class)) {
            return true;
        }
        // Meta-annotation check: @Repository and @Service are annotated with @Component
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Field injection via reflection — the core DI mechanism in this framework.
     *
     * <p><b>How it works:</b>
     * <ol>
     *   <li>Iterate every bean's declared fields</li>
     *   <li>If field has {@code @Inject}, look up matching type in registry</li>
     *   <li>{@code field.setAccessible(true)} — bypass private modifier (frameworks need this)</li>
     *   <li>{@code field.set(bean, dependency)} — write the reference onto the heap object</li>
     * </ol>
     *
     * <p><b>Spring equivalent:</b> {@code AutowiredAnnotationBeanPostProcessor}
     */
    private void injectDependencies() throws IllegalAccessException {
        // HashSet copy avoids ConcurrentModification if injection triggers new registrations
        for (Object bean : new HashSet<>(beanRegistry.values())) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }

                Object dependency = beanRegistry.get(field.getType());
                if (dependency == null) {
                    throw new RuntimeException(
                            "Missing dependency " + field.getType().getSimpleName()
                                    + " for " + bean.getClass().getSimpleName() + "." + field.getName());
                }

                field.setAccessible(true);  // LEARN: opens private fields to reflection (Java 9+ module caveats)
                field.set(bean, dependency);
                System.out.println("  → Injected " + field.getType().getSimpleName()
                        + " → " + bean.getClass().getSimpleName());
            }
        }
    }

    /** Invokes all methods annotated with the given lifecycle annotation (@PostConstruct / @PreDestroy). */
    private void invokeLifecycle(Class<? extends Annotation> annotationType) throws Exception {
        for (Object bean : new HashSet<>(beanRegistry.values())) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationType)) {
                    method.setAccessible(true);
                    method.invoke(bean);
                }
            }
        }
    }

    /**
     * Classpath scanner — finds all classes under a package.
     *
     * <p><b>Spring equivalent:</b> {@code ClassPathBeanDefinitionScanner} (uses ASM for speed;
     * we load full Class objects for clarity).
     *
     * <p>Handles two deployment modes:
     * <ul>
     *   <li>{@code file:} protocol — exploded classes in IDE / {@code out/classes}</li>
     *   <li>{@code jar:} protocol — packaged fat JAR (Spring Boot style)</li>
     * </ul>
     */
    private List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');  // "com.example" → "com/example"

        if (classLoader == null) {
            // Context classloader: lets code in one loader find classes in another (app servers, Boot JARs)
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = Container.class.getClassLoader();
            }
        }

        // getResources (plural) — same package can exist in multiple JARs
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File directory = new File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                    scanDirectory(directory, packageName, classes);
                }
            } else if ("jar".equals(resource.getProtocol()) || "zip".equals(resource.getProtocol())) {
                scanJarFile(resource, path, classes);
            }
        }

        return classes;
    }

    /** Recursively walks a directory tree and loads every .class file found. */
    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    // false = load class metadata WITHOUT running static initializers yet
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException e) {
                    System.err.println("Warning: could not load " + className);
                }
            }
        }
    }

    /** Scans inside a JAR file for classes in the target package (production deployment). */
    private void scanJarFile(URL jarUrl, String path, List<Class<?>> classes) throws Exception {
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
        }
        int separatorIndex = jarPath.indexOf('!');
        if (separatorIndex != -1) {
            jarPath = jarPath.substring(0, separatorIndex);
        }

        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
            String packagePath = path + "/";
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Skip inner classes (UserService$Helper.class) — they aren't standalone beans
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entryName.contains("$")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    try {
                        classes.add(Class.forName(className, false, classLoader));
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        System.err.println("Warning: could not load " + className);
                    }
                }
            }
        }
    }
}
