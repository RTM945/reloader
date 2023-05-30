import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HotDeployJar {

    private static final Map<String, byte[]> loaded = new HashMap<>();

    private final String jar;
    private final Instrumentation inst;

    public HotDeployJar(String jar, Instrumentation inst) {
        this.jar = jar;
        this.inst = inst;
    }

    public void exec() {
        File file = new File(jar);
        if (!file.exists()) {
            System.err.println("jar don't exist!");
            return;
        }
        Map<String, byte[]> reloadingClass = new HashMap<>();
        Map<String, byte[]> newClass = new HashMap<>();
        try {
            for (Map.Entry<String, byte[]> entry : loadFile(file).entrySet()) {
                String name = entry.getKey();
                byte[] bytes = entry.getValue();
                if (isLoaded(name, bytes)) {
                    System.out.println("skip loaded class " + name);
                    continue;
                }
                try {
                    Class<?> c = Class.forName(name, false, getClass().getClassLoader());
                    if(c == this.getClass()) {
                        System.out.println("skip class: " + c);
                        continue;
                    }
                    reloadingClass.put(name, bytes);
                } catch (ClassNotFoundException e) {
                    newClass.put(name, bytes);
                } catch (Exception e) {
                    System.err.println("reload " + jar + " fail " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }

            for (Map.Entry<String, byte[]> entry : newClass.entrySet()) {
                String name = entry.getKey();
                byte[] bytes = entry.getValue();
                try {
                    ClassLoader classLoader = getClass().getClassLoader();
                    Method method = null;
                    for (Class<?> clazz = classLoader.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
                        try {
                            method = clazz.getDeclaredMethod("defineClass",
                                    String.class, byte[].class, int.class, int.class);
                            break;
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                    method.setAccessible(true);
                    method.invoke(classLoader, name, bytes, 0, bytes.length);
                    setLoaded(name, bytes);
                    System.out.println("add new class " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("add new class " + name + " fail " + e.getMessage());
                    return;
                }
            }

            try {
                if (!reloadingClass.isEmpty()) {
                    ClassDefinition[] definitions = new ClassDefinition[reloadingClass.size()];
                    int idx = 0;
                    for (Map.Entry<String, byte[]> entry : reloadingClass.entrySet()) {
                        String name = entry.getKey();
                        byte[] bytes = entry.getValue();
                        definitions[idx++] = new ClassDefinition(Class.forName(name), bytes);
                        System.out.println("ready to reload class " + name);
                    }
                    System.out.println("reloading " + jar);
                    inst.redefineClasses(definitions);
                    System.out.println("reloading " + jar + " success");
                    for (Map.Entry<String, byte[]> entry : reloadingClass.entrySet()) {
                        String name = entry.getKey();
                        byte[] bytes = entry.getValue();
                        setLoaded(name, bytes);
                    }
                } else {
                    System.out.println("no class to reload");
                }
            } catch (Exception e) {
                System.err.println("reload " + jar + " fail " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Throwable t) {
            System.err.println("reload " + jar + " fail" + t.getMessage());
            t.printStackTrace();
        }
    }

    public Map<String, byte[]> loadFile(File file) throws Exception {
        Map<String, byte[]> classBytes = new HashMap<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                String classname = name.replace('/', '.');
                classname = classname.substring(0, classname.lastIndexOf('.'));

                InputStream in = jarFile.getInputStream(entry);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] temp = new byte[1024];
                int n;
                while ((n = in.read(temp)) != -1) {
                    bos.write(temp, 0, n);
                }

                in.close();
                bos.close();
                byte[] bs = bos.toByteArray();

                classBytes.put(classname, bs);
            }
        }
        return classBytes;
    }

    private boolean isLoaded(String name, byte[] bytes) {
        byte[] bs = loaded.get(name);
        if (bs == null) {
            return false;
        }
        return Arrays.equals(bs, bytes);
    }

    private void setLoaded(String name, byte[] bytes) {
        loaded.put(name, bytes);
    }
}
