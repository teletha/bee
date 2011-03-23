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

/**
 * <p>
 * Interactive user interface.
 * </p>
 * 
 * @version 2011/03/23 16:30:28
 */
public interface UserInterface extends UserNotifier {

    /**
     * <p>
     * Ask user about your question and return his/her answer.
     * </p>
     * 
     * @param question Your question message.
     * @return An answer.
     */
    String ask(String question);

    /**
     * <p>
     * Ask user about your question and return his/her answer.
     * </p>
     * <p>
     * UserInterface can display a default answer and user can use it with simple action. If the
     * returned answer is incompatible with the default anwser type, default answer will be
     * returned.
     * </p>
     * 
     * @param <T> Anwser type.
     * @param question Your question message.
     * @param defaultAnswer A default anwser.
     * @return An answer.
     */
    <T> T ask(String question, T defaultAnswer);

    /**
     * <p>
     * Ask user about your question and return his/her answer.
     * </p>
     * <p>
     * Validator validates a his/her awnser. If it is invalid, UserInterface will display reason of
     * invalidity and continue to ask user.
     * </p>
     * 
     * @param <T> Anwser type.
     * @param question Your question message.
     * @param validator A validator for anwser.
     * @return An answer.
     */
    <T> T ask(String question, Validator<T> validator);

    /**
     * <p>
     * Ask user about your question and return his/her answer.
     * </p>
     * <p>
     * UserInterface can display a default answer and user can use it with simple action. Validator
     * validates a his/her awnser. If it is invalid, UserInterface will display reason of invalidity
     * and continue to ask user.
     * </p>
     * 
     * @param <T> Anwser type.
     * @param question Your question message.
     * @param defaultAnswer A default anwser.
     * @param validator A validator for anwser.
     * @return An answer.
     */
    <T> T ask(String question, T defaultAnswer, Validator<T> validator);
}
