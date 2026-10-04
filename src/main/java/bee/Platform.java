/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Properties;

import kiss.I;
import kiss.Ⅱ;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * Define platform specific default configurations.
 */
public final class Platform {

    /** OS */
    public static final String OSName = System.getProperty("os.name");

    /** OS */
    public static final String OSVersion = System.getProperty("os.version");

    /** OS */
    public static final String OSArch = System.getProperty("os.arch");

    /** The encoding. */
    public static final Charset Encoding = Charset.forName(System.getProperty("sun.jnu.encoding"));

    /** The line separator. */
    public static final String EOL = System.getProperty("line.separator");

    /** The executable file for Java. */
    public static final File Java;

    /** The root directory for Java. */
    public static final Directory JavaHome;

    /** The executable file for Bee. */
    public static final File Bee;

    /** The root directory for Bee. */
    public static final Directory BeeHome;

    /** The local repository. */
    public static final Directory BeeLocalRepository;

    /** The source which the local repository is resolved from. */
    public static final String BeeLocalRepositorySource;

    /** The user configuration file. */
    public static final File Config;

    /** The minimum Java feature version required to run this Bee. */
    private static final int RequiredJava;

    /** The platform type. */
    private static final boolean isWindows = OSName.toLowerCase().contains("win");

    /** The platform type. */
    private static final boolean isLinux = !isWindows && !(OSName.contains("Mac") || OSName.contains("Darwin"));

    /** The platform type. */
    private static final boolean isGithub = System.getenv("GITHUB_ACTIONS") != null;

    /** The platform type. */
    private static final boolean isJitPack = System.getenv("JITPACK") != null;

    /** The platform type. */
    private static boolean isEclipse;

    // initialization
    static {
        File beeExe = null;

        // Search the Bee launcher and the Eclipse embedded JDK from the environment path.
        root: for (Entry<String, String> entry : System.getenv().entrySet()) {
            // On UNIX systems the alphabetic case of name is typically significant, while on
            // Microsoft Windows systems it is typically not.
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String value : entry.getValue().split(java.io.File.pathSeparator)) {
                    if (value.toLowerCase().contains("plugins/org.eclipse.justj.openjdk")) {
                        isEclipse = true;
                        continue;
                    }

                    Directory directory = Locator.directory(value);
                    if (directory.file(isWindows ? "javac.exe" : "javac").isPresent()) {
                        beeExe = directory.file(isWindows ? "bee.bat" : "bee");
                        break root;
                    }
                }
            }
        }

        BeeHome = locateBeeHome();
        Config = BeeHome.file("config.properties");
        RequiredJava = requiredJava();

        // Resolve the JDK: the configured one (BEE_JAVA or config.properties) takes precedence,
        // then the one on the environment path, and finally the JDK which is running this process.
        Directory javaHome = configuredJava();
        if (javaHome == null) {
            javaHome = searchJavaHome();
        }
        if (javaHome == null) {
            Directory running = Locator.directory(System.getProperty("java.home"));
            if (isJdk(running)) {
                javaHome = running;
            }
        }
        if (javaHome == null) {
            throw new Error("Java SDK is not found in your environment path.");
        }

        JavaHome = javaHome;
        Java = javaHome.file(isWindows ? "bin/javac.exe" : "bin/javac");
        Bee = beeExe != null ? beeExe : javaHome.file(isWindows ? "bin/bee.bat" : "bin/bee");

        Ⅱ<Directory, String> repository = searchLocalRepository();
        BeeLocalRepository = repository.ⅰ;
        BeeLocalRepositorySource = repository.ⅱ;
    }

    /**
     * Locate the Bee home directory. It is independent from the JDK installation so that Bee can
     * install its executor and caches without writing into the JDK directory. The location can be
     * overridden by the {@code BEE_HOME} environment variable.
     * 
     * @return The Bee home directory.
     */
    private static Directory locateBeeHome() {
        String home = I.env("BEE_HOME");
        if (home != null && !home.isBlank()) {
            return Locator.directory(home).absolutize();
        }
        return Locator.directory(System.getProperty("user.home")).directory(".bee");
    }

    /**
     * Resolve the JDK selected by the user. The {@code BEE_JAVA} environment variable has the
     * highest
     * priority, then the {@code java} entry in the Bee configuration file. The value can be either
     * a
     * path to a JDK or a version (e.g. {@code 21}) of an installed JDK.
     * 
     * @return The selected JDK, or <code>null</code> when nothing is configured.
     */
    public static Directory configuredJava() {
        String value = I.env("BEE_JAVA");
        if (value == null || value.isBlank()) {
            value = config("java");
        }
        if (value == null || value.isBlank()) {
            return null;
        }

        Directory dir = Locator.directory(value);
        if (isJdk(dir) && canRun(dir)) {
            return dir;
        }

        // Treat the value as a version and search the installed JDKs.
        for (Directory candidate : BeeHome.directory("jdk").walkDirectory("*").toList()) {
            if (candidate.name().contains(value) && isJdk(candidate) && canRun(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Test whether the specified JDK can run this Bee. An old JDK can not load the Bee classes, so
     * it
     * is ignored when resolving the selected JDK.
     * 
     * @param jdk A JDK home directory.
     * @return A result.
     */
    public static boolean canRun(Directory jdk) {
        int feature = feature(jdk);
        return feature == -1 || RequiredJava == 0 || feature >= RequiredJava;
    }

    /**
     * Detect the minimum Java feature version required to run this Bee from its own class file.
     * 
     * @return A required Java feature version, or 0 when it can not be detected.
     */
    private static int requiredJava() {
        try (InputStream in = Bee.class.getResourceAsStream("Bee.class")) {
            byte[] header = in.readNBytes(8);
            int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
            return major - 44;
        } catch (Throwable e) {
            return 0;
        }
    }

    /**
     * Detect the feature version of the specified JDK from its release file.
     * 
     * @param jdk A JDK home directory.
     * @return A feature version, or <code>-1</code> when it can not be detected.
     */
    public static int feature(Directory jdk) {
        File release = jdk.file("release");

        if (release.isPresent()) {
            for (String line : release.lines().toList()) {
                if (line.startsWith("JAVA_VERSION=")) {
                    String version = line.substring(13).replace("\"", "").trim();
                    int dot = version.indexOf('.');

                    try {
                        return Integer.parseInt(dot == -1 ? version : version.substring(0, dot));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Search the JDK on the environment path.
     * 
     * @return The JDK home, or <code>null</code> when it is not found.
     */
    private static Directory searchJavaHome() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String value : entry.getValue().split(java.io.File.pathSeparator)) {
                    Directory bin = Locator.directory(value);
                    if (bin.file(isWindows ? "javac.exe" : "javac").isPresent()) {
                        return bin.parent();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Test whether the specified directory is a JDK.
     * 
     * @param dir A candidate directory.
     * @return A result.
     */
    private static boolean isJdk(Directory dir) {
        return dir != null && dir.file(isWindows ? "bin/javac.exe" : "bin/javac").isPresent();
    }

    /**
     * Read the user configuration.
     * 
     * @param key A configuration key.
     * @return A configured value, or <code>null</code>.
     */
    public static String config(String key) {
        if (Config != null && Config.isPresent()) {
            try (InputStream in = Config.newInputStream()) {
                Properties properties = new Properties();
                properties.load(in);
                return properties.getProperty(key);
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Update the user configuration.
     * 
     * @param key A configuration key.
     * @param value A configuration value. <code>null</code> removes the entry.
     */
    public static void config(String key, String value) {
        Properties properties = new Properties();

        if (Config.isPresent()) {
            try (InputStream in = Config.newInputStream()) {
                properties.load(in);
            } catch (IOException e) {
                // ignore
            }
        }

        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }

        Config.parent().create();
        try (OutputStream out = Config.newOutputStream()) {
            properties.store(out, "Bee user configuration");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Search the Maven local repository and resolve which source it is determined from. The
     * returned pair is the repository and the description of its source, which is one of the
     * following.
     * <ul>
     * <li><code>from Maven [&lt;settings.xml&gt;]</code> - the localRepository in the Maven
     * settings.xml</li>
     * <li><code>from Maven [default]</code> - the default Maven local repository
     * (.m2/repository)</li>
     * <li><code>from Bee [default]</code> - the Bee local repository (.bee/repository)</li>
     * <li><code>from JitPack [default]</code> - the Maven local repository on JitPack</li>
     * </ul>
     * 
     * @return The local repository and the source it is determined from.
     */
    private static Ⅱ<Directory, String> searchLocalRepository() {
        if (isJitPack) {
            return I.pair(Locator.directory(System.getenv("HOME")).directory(".m2/repository"), "from JitPack [default]");
        }

        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(java.io.File.pathSeparator)) {
                    File mvn = Locator.directory(path).file("mvn");
                    if (mvn.isPresent()) {
                        // maven is here
                        Directory home = mvn.parent().parent();
                        File conf = home.file("conf/settings.xml");

                        if (conf.isPresent()) {
                            String location = I.xml(conf.asJavaPath()).find("localRepository").text();
                            if (location.length() != 0) {
                                return I.pair(Locator.directory(location), "from Maven [" + conf + "]");
                            }
                        }
                    }
                }
            }
        }
        // Prefer the standard Maven local repository when it exists so that artifacts are shared
        // with Maven, otherwise use Bee's own repository under the Bee home.
        Directory maven = Locator.directory(System.getProperty("user.home")).directory(".m2/repository");
        if (maven.isPresent()) {
            return I.pair(maven, "from Maven [default]");
        }
        return I.pair(BeeHome.directory("repository"), "from Bee [default]");
    }

    /**
     * Hide constructor.
     */
    private Platform() {
    }

    /**
     * Check whether the current platform is Windows OS or not.
     * 
     * @return Result
     */
    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * Check whether the current platform is Linux like OS or not.
     * 
     * @return Result
     */
    public static boolean isLinux() {
        return isLinux;
    }

    /**
     * Check whether the current platform is Linux like OS or not.
     * 
     * @return Result
     */
    public static boolean isMac() {
        return OSName.contains("Mac") || OSName.contains("Darwin");
    }

    /**
     * Check whether the current platform is Github Action or not.
     * 
     * @return Result
     */
    public static boolean isGithub() {
        return isGithub;
    }

    /**
     * Check whether the current platform is JitPack or not.
     * 
     * @return Result
     */
    public static boolean isJitPack() {
        return isJitPack;
    }

    /**
     * Check whether the current platform is Eclipse or not.
     * 
     * @return Result
     */
    public static boolean isEclipse() {
        return isEclipse;
    }
}
