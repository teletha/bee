/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.File;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.URLResolver;

/**
 * @version 2012/02/20 10:07:10
 */
public class IvyMain {

    public static void main(String[] args) throws Exception {
        File file = new IvyMain().resolveArtifact("asm", "asm", "3.2");
        System.out.println(file);
    }

    public File resolveArtifact(String groupId, String artifactId, String version) throws Exception {
        // creates clear ivy settings
        IvySettings settings = new IvySettings();
        // url resolver for configuration of maven repo
        URLResolver resolver = new URLResolver();
        resolver.setM2compatible(true);
        resolver.setName("central");
        resolver.addArtifactPattern("http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");
        // adding maven repo resolver
        settings.addResolver(resolver);
        // set to the default resolver
        settings.setDefaultResolver(resolver.getName());
        // creates an Ivy instance with settings
        Ivy ivy = Ivy.newInstance(settings);

        File ivyfile = File.createTempFile("ivy", ".xml");
        ivyfile.deleteOnExit();

        String[] dep = new String[] {groupId, artifactId, version};

        DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0], dep[1] + "-caller", "working"));

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
        md.addDependency(dd);

        // creates an ivy configuration file
        XmlModuleDescriptorWriter.write(md, ivyfile);

        String[] confs = new String[] {"default"};
        ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);
        resolveOptions.setLog(LogOptions.LOG_DEFAULT);

        // init resolve report
        ResolveReport report = ivy.resolve(ivyfile.toURL(), resolveOptions);

        // so you can get the jar library
        File jarArtifactFile = report.getAllArtifactsReports()[0].getLocalFile();

        return jarArtifactFile;
    }

}
