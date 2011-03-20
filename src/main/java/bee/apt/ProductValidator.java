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
package bee.apt;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import bee.Product;
import bee.Project;
import bee.compiler.JavaCompiler;

/**
 * @version 2010/06/10 15:18:26
 */
public class ProductValidator implements TypeAnnotationValidator<Product> {

    /**
     * @see bee.apt.TypeAnnotationValidator#validate(java.lang.Class,
     *      java.lang.annotation.Annotation)
     */
    @Override
    public void validate(Class type, Product annotation) throws InvalidValue {
        File project = new File("src/project/Project.java");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = format.format(new Date(project.lastModified()));
        System.out.println(time);

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(project.getParentFile().toPath());

        ClassLoader loader = compiler.compile();

        try {
            Class<? extends Project> projectClass = (Class<? extends Project>) loader.loadClass("Project");

            Project projectinstance = projectClass.newInstance();

            for (Method method : projectClass.getMethods()) {
                Product product = method.getAnnotation(Product.class);

                if (product != null) {

                    method.invoke(projectinstance);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidValue(e.getMessage());
        }
    }
}
