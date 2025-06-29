/*
 * Copyright 2024 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.flowui.kit.component.stub;

import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.kit.meta.StudioIgnore;

import java.util.function.Consumer;

/**
 * For Studio use only.
 */
interface DropdownButtonStubSeparator extends DropdownButtonItem {

    @StudioIgnore
    @Override
    Registration addClickListener(Consumer<ClickEvent> listener);
}
