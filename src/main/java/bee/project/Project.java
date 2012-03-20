/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this Path except in compliance with the License.
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
package bee.project;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2010/04/02 3:47:44
 */
@Manageable(lifestyle = Singleton.class)
public class Project {

    /** The products. */
    private final List<Product> products = new ArrayList();

    /**
     * 
     */
    protected Project() {
    }

    /**
     * Initialize project definition.
     */
    private void build() {
        try {
            for (Field field : getClass().getDeclaredFields()) {
                if (Product.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);

                    // Product Definition
                    Product product = (Product) field.get(this);

                    if (product != null) {
                        products.add(product);
                    }
                }
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Launch project build process.
     * </p>
     * 
     * @param definition
     */
    protected static final void launch(Class<? extends Project> definition) {
        Project project = I.make(definition);
        project.build();
    }
}
