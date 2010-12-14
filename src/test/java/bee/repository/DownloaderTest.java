/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import ezunit.CleanRoom;

/**
 * @version 2010/09/19 13:08:00
 */
public class DownloaderTest {

    @Rule
    public static final CleanRoom room = new CleanRoom();

    private static final List<Artifact> single = new ArrayList<Artifact>();

    private static final List<Repository> repositories = new ArrayList<Repository>();

    /** The previous user home. */
    private static File home;

    static {
        single.add(new Artifact("org.slf4j", "slf4j-api", "1.4.3"));

        repositories.add(new Repository("http://repo1.maven.org/maven2/"));
    }

    @BeforeClass
    public static void setup() {
        home = Repository.getLocation();

        Repository.setLocation(room.locateDirectory(""));
    }

    @AfterClass
    public static void cleanup() {
        Repository.setLocation(home);
    }

    @Test
    public void donwloadSingleArtifact() throws Exception {
        System.out.println(Repository.getLocation());
        Downloader.donwload(single, repositories);

        Artifact artifact = single.get(0);
        assertTrue(artifact.locate().exists());
        assertTrue(artifact.locateProjectDescriptor().exists());
    }
}
