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

import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePathScanner;

/**
 * @version 2010/05/26 18:16:07
 */
public class ASTScanner extends TreePathScanner<Void, Object> {

    List<String> list = new ArrayList();

    /**
     * @see com.sun.source.util.TreeScanner#visitAnnotation(com.sun.source.tree.AnnotationTree,
     *      java.lang.Object)
     */
    @Override
    public Void visitAnnotation(AnnotationTree annotation, Object data) {
        list.add(annotation.getArguments().toString());
        return super.visitAnnotation(annotation, data);
    }

    /**
     * @see com.sun.source.util.TreeScanner#visitMethodInvocation(com.sun.source.tree.MethodInvocationTree,
     *      java.lang.Object)
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Object data) {
        list.add(invocation.getTypeArguments().toString());
        return super.visitMethodInvocation(invocation, data);
    }
}
