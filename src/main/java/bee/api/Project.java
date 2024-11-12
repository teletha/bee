/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.SourceVersion;

import org.apache.maven.model.Contributor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;

import bee.Bee;
import bee.coder.StandardHeaderStyle;
import bee.task.AnnotationValidator;
import bee.util.Ensure;
import bee.util.Inputs;
import kiss.I;
import kiss.Signal;
import kiss.XML;
import psychopath.Directory;
import psychopath.File;
import psychopath.Location;
import psychopath.Locator;

public class Project {

    /** The libraries. */
    final SortedSet<Library> libraries = new TreeSet();

    /** The excluded libraries. */
    final HashSet<Exclusion> exclusions = new HashSet();

    /** The repositories. */
    final ArrayList<RemoteRepository> repositories = new ArrayList();

    /** The project association. */
    final Map<Class, Object> associates = new ConcurrentHashMap();

    /** The project root directory. */
    private Directory root;

    /** The product group. */
    private String productGroup = "";

    /** The product name. */
    private String productName = "";

    /** The product version. */
    private String productVersion = "1.1";

    /** The product description. */
    private String description;

    /** The license. */
    private License license = License.MIT;

    /** The license related info. */
    final List<Integer> licensedFrom = new ArrayList();

    /** The license related info. */
    final List<Integer> licensedTo = new ArrayList();

    /** The license related info. */
    final List<String> licensedBy = new ArrayList();

    /** The encoding. */
    private Charset encoding = StandardCharsets.UTF_8;

    /** The requirement of Java version. */
    private SourceVersion sourceFileVersion;

    /** The requirement of class file version. */
    private SourceVersion classFileVersion;

    /** The input base directory. */
    private Directory input;

    /** The output base directory. */
    private Directory output;

    /** The version control system. */
    private VCS vcs;

    /**
     * 
     */
    protected Project() {
        Class projectClass = getClass();

        if (projectClass.isMemberClass() || projectClass.isAnonymousClass()) {
            // fabric project
            this.root = Locator.directory("").absolutize();
        } else {
            try {
                Location archive = Locator.locate(projectClass);

                if (archive.isDirectory()) {
                    // directory
                    this.root = archive.parent().parent();
                } else {
                    // some archive
                    if (archive.toString().contains("temporary")) {
                        this.root = Locator.directory("").absolutize();
                    } else {
                        this.root = archive.asDirectory();
                    }
                }
            } catch (Throwable e) {
                this.root = Locator.directory("").absolutize();
            }
        }

        setInput((Directory) null);
        setOutput((Directory) null);
    }

    /**
     * Return project root directory.
     * 
     * @return A root directory of this project.
     */
    public Directory getRoot() {
        return root;
    }

    /**
     * Return product group.
     * 
     * @return The product group.
     */
    public String getGroup() {
        return productGroup;
    }

    /**
     * Internal setter for property access.
     * 
     * @param groupName
     */
    @SuppressWarnings("unused")
    private void setGroup(String groupName) {
        this.productGroup = groupName;
    }

    /**
     * Return product name.
     * 
     * @return The product name.
     */
    public String getProduct() {
        return productName;
    }

    /**
     * Internal setter for property access.
     * 
     * @param productName
     */
    @SuppressWarnings("unused")
    private void setProduct(String productName) {
        this.productName = productName;
    }

    /**
     * Return product version.
     * 
     * @return The product version.
     */
    public String getVersion() {
        return productVersion;
    }

    /**
     * Internal setter for property access.
     * 
     * @param version
     */
    @SuppressWarnings("unused")
    private void setVersion(String version) {
        this.productVersion = version;
    }

    /**
     * Declare product package, name and version.
     * 
     * @param group A product group.
     * @param product A product name.
     * @param version A product version.
     */
    protected final void product(CharSequence group, CharSequence product, CharSequence version) {
        this.productGroup = Ensure.Alphanumeric.separator("._-+").validate(group).toString();
        this.productName = Ensure.Alphanumeric.separator("_-+").validate(product).toString();
        this.productVersion = Ensure.Alphanumeric.separator("._-+").validate(version).toString();
    }

    /**
     * Return product description.
     * 
     * @return The product description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Internal setter for property access.
     * 
     * @param description
     */
    @SuppressWarnings("unused")
    private void setDescription(String description) {
        this.description = description;
    }

    /**
     * Declare product description.
     * 
     * @param description A product description.
     */
    protected final void describe(String description) {
        if (description == null) {
            description = "";
        }
        this.description = description.trim();
    }

    /**
     * Returns project license.
     * 
     * @return
     */
    public License license() {
        return license;
    }

    /**
     * Set product license.
     * 
     * @param license
     */
    protected final void license(License license) {
        this.license = Objects.requireNonNullElse(license, License.MIT);
    }

    /**
     * Add product licenser.
     */
    protected final void licenser(int licensedFrom, String licensedBy) {
        licenser(licensedFrom, 0, licensedBy);
    }

    /**
     * Add product licenser.
     */
    protected final void licenser(int licensedFrom, int licensedTo, String licensedBy) {
        this.licensedFrom.add(licensedFrom <= 0 ? Year.now().getValue() : licensedFrom);
        this.licensedTo.add(licensedTo <= 0 ? Year.now().getValue() : licensedTo);
        this.licensedBy.add(Objects.requireNonNullElse(licensedBy, ""));
    }

    /**
     * Returns project encoding.
     * 
     * @return
     */
    public Charset getEncoding() {
        return encoding;
    }

    /**
     * Set product encoding.
     * 
     * @param encoding
     */
    protected final void encoding(Charset encoding) {
        if (encoding == null) {
            encoding = StandardCharsets.UTF_8;
        }
        this.encoding = encoding;
    }

    /**
     * Check whether this project depends on the specified product or not.
     * 
     * @param projectName A project name to search.
     * @param productName A product name to search.
     * @return A search result.
     */
    public boolean hasDependency(String projectName, String productName) {
        return getDependency(Scope.Compile).stream()
                .filter(p -> p.group.equals(projectName) && p.name.equals(productName))
                .findFirst()
                .isPresent();
    }

    /**
     * Resolve all dependencies for the specified scope.
     * 
     * @param scopes
     * @return
     */
    public Set<Library> getDependency(Scope... scopes) {
        if (scopes == null) {
            return Collections.emptySet();
        }
        return I.make(Repository.class).collectDependency(this, scopes);
    }

    /**
     * Find {@link Library} by name.
     * 
     * @param projectName
     * @param productName
     * @return
     */
    public Set<Library> getLibrary(String projectName, String productName) {
        return getLibrary(projectName, productName, "LATEST");
    }

    /**
     * Find {@link Library} by name.
     * 
     * @param projectName
     * @param productName
     * @param version
     * @return
     */
    public Set<Library> getLibrary(String projectName, String productName, String version) {
        return I.make(Repository.class).collectDependency(new Library(projectName, productName, version), Scope.Runtime);
    }

    /**
     * Gather all classpaths needed to run this project.
     * 
     * @return
     */
    public Set<String> getClasspath() {
        return I.signal(getDependency(Scope.Runtime)).map(file -> file.getLocalJar().path()).startWith(locateJar().path()).toSet();
    }

    /**
     * Declare dependency against to Lombok library.
     */
    protected final Library requireLombok() {
        return require(Bee.Lombok.productGroup, Bee.Lombok.productName, Bee.Lombok.productVersion).atProvided();
    }

    /**
     * Declare dependency for latest version.
     * 
     * @param projectName A project name.
     * @param productName A product name.
     * @return A dependency.
     */
    protected final Library require(String projectName, String productName) {
        return require(projectName, productName, null, "LATEST");
    }

    /**
     * Declare dependency.
     * 
     * @param projectName A project name.
     * @param productName A product name.
     * @param version A product version.
     * @return A dependency.
     */
    protected final Library require(String projectName, String productName, String version) {
        return require(projectName, productName, null, version);
    }

    /**
     * Declare dependency.
     * 
     * @param projectName A project name.
     * @param productName A product name.
     * @param classifier A product classifier.
     * @param version A product version.
     * @return A dependency.
     */
    protected final Library require(String projectName, String productName, String classifier, String version) {
        Library library = new Library(projectName, productName, classifier, version);
        libraries.add(library);

        // API definition
        return library;
    }

    /**
     * Returns Java version requirement.
     * 
     * @return A Java version requirement.
     */
    public SourceVersion getJavaSourceVersion() {
        return sourceFileVersion == null ? SourceVersion.latest() : sourceFileVersion;
    }

    /**
     * Internal setter for property access.
     * 
     * @param version
     */
    @SuppressWarnings("unused")
    private void setJavaSourceVersion(SourceVersion version) {
        this.sourceFileVersion = version;
    }

    /**
     * Returns Java version requirement.
     * 
     * @return A Java version requirement.
     */
    public SourceVersion getJavaClassVersion() {
        return classFileVersion == null ? SourceVersion.latest() : classFileVersion;
    }

    /**
     * Internal setter for property access.
     * 
     * @param version
     */
    @SuppressWarnings("unused")
    private void setJavaClassVersion(SourceVersion version) {
        this.classFileVersion = version;
    }

    /**
     * Declare Java version requirement.
     * 
     * @param version A Java version to require.
     */
    protected final void require(SourceVersion version) {
        require(version, version);
    }

    /**
     * Declare Java version requirement.
     * 
     * @param sourceVersion A Java source version to require.
     * @param targetVersion A Java target version to require.
     */
    protected final void require(SourceVersion sourceVersion, SourceVersion targetVersion) {
        this.sourceFileVersion = sourceVersion;
        this.classFileVersion = targetVersion;
    }

    /**
     * Exclude the specified library from transitive dependency resolution.
     * 
     * @param projectName A project name.
     * @param productName A product name.
     */
    protected final void unrequire(String projectName, String productName) {
        exclusions.add(new Exclusion(projectName, productName, "", "jar"));
    }

    /**
     * Add new repository by URI.
     * 
     * @param uri
     */
    protected final void repository(String uri) {
        if (uri != null && !uri.isEmpty()) {
            try {
                Builder builder = new Builder(new URI(uri).getHost(), "default", uri);
                repositories.add(builder.build());
            } catch (URISyntaxException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * Get the bese directory of input.
     * 
     * @return The base input directory.
     */
    public Directory getInput() {
        return input;
    }

    /**
     * Set base directory of input. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * 
     * @param input The base input directory to set.
     */
    protected final void setInput(Directory input) {
        if (input == null) {
            input = getRoot().directory("src");
        }

        if (!input.isAbsolute()) {
            input = getRoot().directory(input);
        }
        this.input = input;
    }

    /**
     * Set base directory of input. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * 
     * @param input The base input directory to set.
     */
    protected final void setInput(String input) {
        if (input == null) {
            input = "src";
        }
        setInput(getRoot().directory(input));
    }

    /**
     * Get the bese directory of output.
     * 
     * @return The base output directory.
     */
    public Directory getOutput() {
        return output;
    }

    /**
     * Set base directory of output. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * 
     * @param output The base output directory to set.
     */
    protected final void setOutput(Directory output) {
        if (output == null) {
            output = getRoot().directory("target");
        }

        if (!output.isAbsolute()) {
            output = getRoot().directory(output);
        }
        this.output = output;
    }

    /**
     * Set base directory of output. <code>null</code> will set default directory. A relative path
     * will be resolved from project root directory.
     * 
     * @param output The base output directory to set.
     */
    protected final void setOutput(String output) {
        if (output == null) {
            output = "target";
        }
        setOutput(getRoot().directory(output));
    }

    /**
     * Returns source directories.
     * 
     * @return
     */
    public Directory getSources() {
        return input.directory("main");
    }

    /**
     * Returns source directories.
     * 
     * @return
     */
    public Signal<Directory> getSourceSet() {
        return getSources().walkDirectory("*");
    }

    /**
     * Returns class directory.
     * 
     * @return
     */
    public Directory getClasses() {
        return output.directory("classes");
    }

    /**
     * Returns test source directories.
     * 
     * @return
     */
    public Directory getTestSources() {
        return input.directory("test");
    }

    /**
     * Returns test source directories.
     * 
     * @return
     */
    public Signal<Directory> getTestSourceSet() {
        return getTestSources().walkDirectory("*");
    }

    /**
     * Returns test class directory.
     * 
     * @return
     */
    public Directory getTestClasses() {
        return output.directory("test-classes");
    }

    /**
     * Returns project source directories.
     * 
     * @return
     */
    public Directory getProjectSources() {
        return input.directory("project");
    }

    /**
     * Returns project source directories.
     * 
     * @return
     */
    public Signal<Directory> getProjectSourceSet() {
        return getProjectSources().walkDirectory("*");
    }

    /**
     * Returns project class directory.
     * 
     * @return
     */
    public Directory getProjectClasses() {
        return output.directory("project-classes");
    }

    /**
     * Returns project source file.
     * 
     * @return
     */
    public File getProjectDefinition() {
        File file = input.file("project/java/Project.java");

        if (file.isAbsent()) {
            file = input.directory("project/java").walkFile("**/Project.java").first().to().or(file);
        }
        return file;
    }

    /**
     * Returns project source file.
     * 
     * @return
     */
    public File getProjectDefintionClass() {
        return getProjectClasses().file(input.directory("project/java").relativize(getProjectDefinition())).extension("class");
    }

    /**
     * Resolve all annotation processor for this project.
     * 
     * @return
     */
    public Set<Location> getAnnotationProcessors() {
        // search javax.annotation.processing.Processor file in libraries
        Set<Location> libraries = new HashSet();

        for (Library library : getDependency(Scope.Annotation)) {
            libraries.add(library.getLocalJar());
        }

        // search AnnotationValidator in classpath
        List<AnnotationValidator> validators = I.find(AnnotationValidator.class);

        if (!validators.isEmpty()) {
            libraries.add(Locator.locate(Bee.class));
        }

        return libraries;
    }

    /**
     * Locate product jar file.
     * 
     * @return
     */
    public File locateJar() {
        return getOutput().file(getProduct() + "-" + getVersion() + ".jar");
    }

    /**
     * Locate product source jar file.
     * 
     * @return
     */
    public File locateSourceJar() {
        return getOutput().file(getProduct() + "-" + getVersion() + "-sources.jar");
    }

    /**
     * Locate product javadoc jar file.
     * 
     * @return
     */
    public File locateJavadocJar() {
        return getOutput().file(getProduct() + "-" + getVersion() + "-javadoc.jar");
    }

    /**
     * Get the VCS.
     * 
     * @return A uri of version control system.
     */
    public VCS getVersionControlSystem() {
        return vcs != null ? vcs : VCS.detect(getRoot());
    }

    /**
     * Internal setter for property access.
     * 
     * @param groupName
     */
    @SuppressWarnings("unused")
    private void setVersionControlSystem(VCS vcs) {
        this.vcs = vcs;
    }

    /**
     * Locate VCS.
     * 
     * @param uri A uri of version control system.
     */
    protected final void versionControlSystem(String uri) {
        this.vcs = VCS.of(uri);
    }

    /**
     * Convert this project to {@link Library}.
     * 
     * @return
     */
    public final Library asLibrary() {
        return new Library(getGroup(), getProduct(), getVersion());
    }

    /**
     * Retrieve the project related object of the specified type.
     * 
     * @param <A>
     * @param type
     * @return
     */
    public <A> A associate(Class<A> type) {
        return (A) associates.computeIfAbsent(type, key -> I.make(type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((productGroup == null) ? 0 : productGroup.toLowerCase().hashCode());
        result = prime * result + ((productName == null) ? 0 : productName.toLowerCase().hashCode());
        result = prime * result + ((productVersion == null) ? 0 : productVersion.toLowerCase().hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Project) {
            Project project = (Project) obj;

            return productGroup.equalsIgnoreCase(project.productGroup) && productName
                    .equalsIgnoreCase(project.productName) && productVersion.equalsIgnoreCase(project.productVersion);
        }
        return false;
    }

    /**
     * Returns literal project definition.
     * 
     * @return
     */
    public List<String> toBeeDefinition() {
        List<String> code = Inputs.templates("""
                {=${ }=}
                import static bee.api.License.*;

                public class Project extends bee.api.Project {
                    {
                        product("${group}", "${product}", "${version}");
                        license(${license.name});
                        ${#versionControlSystem}
                        versionControlSystem("${versionControlSystem}");
                        ${/versionControlSystem}
                    }
                }
                """, this);

        return StandardHeaderStyle.SlashStar.convert(code, license);
    }

    /**
     * Returns literal of POM file.
     * 
     * @return
     */
    public String toMavenDefinition() {
        XML pom = I.xml("project");
        pom.child("modelVersion").text("4.0.0");
        pom.child("groupId").text(getGroup());
        pom.child("artifactId").text(getProduct());
        pom.child("version").text(getVersion());
        pom.child("packaging").text("jar");
        pom.child("name").text(getProduct());
        pom.child("description").text(getDescription());

        XML dependencies = pom.child("dependencies");

        for (Library library : libraries) {
            XML dependency = dependencies.child("dependency");
            dependency.child("groupId").text(library.group);
            dependency.child("artifactId").text(library.name);
            dependency.child("version")
                    .text(library.version.equals("LATEST") ? I.make(Repository.class).resolveLatestVersion(library) : library.version);
            dependency.child("scope").text(library.scope == Scope.Annotation ? "provided" : library.scope.toString());

            if (library.isJavaTools()) {
                dependency.child("systemPath").text("${java.home}/../lib/tools.jar");
            }

            XML exclusions = dependency.child("exclusions");

            for (Exclusion e : this.exclusions) {
                XML exclusion = exclusions.child("exclusion");
                exclusion.child("groupId").text(e.getGroupId());
                exclusion.child("artifactId").text(e.getArtifactId());
            }
        }

        if (this.license != null) {
            XML license = pom.child("licenses").child("license");
            license.child("name").text(this.license.full);
            license.child("url").text(this.license.uri);
        }

        List<RemoteRepository> repos = new ArrayList();
        repos.addAll(this.repositories);
        repos.addAll(Repository.builtinRepositories);
        XML repositories = pom.child("repositories");

        for (RemoteRepository repo : repos) {
            XML repository = repositories.child("repository");
            repository.child("id").text(repo.getId());
            repository.child("name").text(repo.getHost());
            repository.child("url").text(repo.getUrl());
        }

        if (vcs != null) {
            pom.child("url").text(vcs.uri());

            XML scm = pom.child("scm");
            scm.child("url").text(vcs.uri());
            scm.child("connection").text(vcs.uriForRead());
            scm.child("developerConnection").text(vcs.uriForWrite());

            XML issue = pom.child("issueManagement");
            issue.child("system").text(vcs.name());
            issue.child("url").text(vcs.issue());

            XML contributors = pom.child("developers");

            for (Contributor contributor : vcs.contributors()) {
                if (contributor.getName() != null) {
                    XML xml = contributors.child("developer");
                    xml.child("name").text(contributor.getName());
                    xml.child("email").text(contributor.getEmail());
                    xml.child("url").text(contributor.getUrl());
                }
            }
        }

        // ==============================================================
        // Maven Plugins
        // ==============================================================
        XML plugins = pom.child("build").child("plugins");

        // compiler-plugin
        XML plugin = plugins.child("plugin");
        plugin.child("artifactId").text("maven-compiler-plugin");
        plugin.child("version").text("3.13.0");
        XML conf = plugin.child("configuration");
        conf.child("source").text(Inputs.normalize(getJavaSourceVersion()));
        conf.child("target").text(Inputs.normalize(getJavaClassVersion()));
        conf.child("encoding").text(getEncoding().displayName());
        XML args = conf.child("compilerArgs");
        if (getClasses().file("META-INF/services/javax.annotation.processing.Processor").isPresent()) args.child("arg").text("-proc:none");

        // surefire-plugin
        plugins.append("""
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.5.2</version>
                    <dependencies>
                        <dependency>
                            <groupId>me.fabriciorby</groupId>
                            <artifactId>maven-surefire-junit5-tree-reporter</artifactId>
                            <version>0.1.0</version>
                        </dependency>
                    </dependencies>
                    <configuration>
                        <argLine>-Dfile.encoding=UTF-8</argLine>
                        <reportFormat>plain</reportFormat>
                        <consoleOutputReporter>
                            <disable>true</disable>
                        </consoleOutputReporter>
                        <statelessTestsetInfoReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoTreeReporter"/>
                    </configuration>
                </plugin>
                """);

        // write as pom
        return pom.toString();
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A path to the target text file.
     * @return A file contents.
     */
    protected static CharSequence ref(String file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    protected static CharSequence ref(Path file) {
        return ref(Locator.file(file));
    }

    /**
     * Return the reference of the specified file's text.
     * 
     * @param file A target text file.
     * @return A file contents.
     */
    protected static CharSequence ref(File file) {
        return Inputs.ref(file);
    }
}