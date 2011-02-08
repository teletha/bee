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
package bee.structure;

/**
 * @version 2011/02/08 16:29:46
 */
public interface Collection<T> {

    /**
     * <p>
     * Ensures that this collection contains the specified element (optional operation). Returns
     * true if this collection changed as a result of the call. (Returns false if this collection
     * does not permit duplicates and already contains the specified element.)
     * </p>
     * 
     * @param item
     * @return
     */
    boolean add(T item);
}
