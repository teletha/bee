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
package bee.ui;

/**
 * @version 2010/09/05 12:47:25
 */
public interface UserInterface {

    String ask(String message);

    <T> T ask(String message, T defaultAnswer);

    <T> T ask(String message, Validator<T> validator);

    <T> T ask(String message, T defaultAnswer, Validator<T> validator);

    void talk(String message, Object... params);
}
