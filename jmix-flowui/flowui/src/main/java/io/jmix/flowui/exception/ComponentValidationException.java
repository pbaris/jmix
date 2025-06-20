/*
 * Copyright 2022 Haulmont.
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

package io.jmix.flowui.exception;

import com.vaadin.flow.component.Component;

/**
 * Represents a validation exception that is specifically related to a UI {@link Component}.
 * <p>
 * This exception is intended to be used in cases where a validation failure needs to be directly
 * associated with a particular {@link Component} in the UI. It provides a reference to
 * the associated component to facilitate handling or error reporting.
 */
public class ComponentValidationException extends ValidationException
        implements ValidationException.HasRelatedComponent {

    private final Component component;

    public ComponentValidationException(String message, Component component) {
        super(message);

        this.component = component;
    }

    public ComponentValidationException(String message, Component component, Exception cause) {
        super(message, cause);

        this.component = component;
    }

    @Override
    public Component getComponent() {
        return component;
    }
}