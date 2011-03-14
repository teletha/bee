/*
 * Copyright (C) 2011 Nameless Production Committee.
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

import org.seasar.aptina.unit.AptinaTestCase;

/**
 * @version 2011/03/13 13:18:30
 */
public class APTTest extends AptinaTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // ソースパスを追加
        addSourcePath("src/test/java");
    }

    public void test() throws Exception {
        // テスト対象の Annotation Processor を生成して追加
        TestProcessor processor = new TestProcessor();
        addProcessor(processor);

        // コンパイル対象を追加
        addCompilationUnit(TestSource.class);

        // コンパイル実行
        compile();

        // テスト対象の Annotation Processor が生成したソースを検証
        assertEqualsGeneratedSource("package foo.bar; public class Baz {}", "foo.bar.Baz");
    }

}
