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
package bee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * @version 2010/05/11 20:15:01
 */
public class JavaRuntimeTest {

    @Test
    public void run() throws Exception {
        try {
            ProcessBuilder builder = new ProcessBuilder("java", "-cp", "e:\\trix\\bee\\target\\classes", "-Duser.dir=e:\\trix\\bee\\target\\test-classes", "bee.Launcher");
            builder.directory(new File(".").getAbsoluteFile());
            builder.redirectErrorStream();
            Process process = builder.start();

            InputStream stream = process.getInputStream();
            while (true) {
                int c = stream.read();
                if (c == -1) {
                    stream.close();
                    break;
                }
                System.out.print((char) c);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
