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

package io.jmix.flowui.accesscontext;

import io.jmix.core.accesscontext.SpecificOperationAccessContext;

/**
 * Represents an access context for modifying configurations of generic filters in the UI.
 * This class establishes a permission boundary specifically for operations related
 * to modifying the configuration of UI components that utilize generic filters.
 */
public class UiGenericFilterModifyConfigurationContext extends SpecificOperationAccessContext {

    public static final String NAME = "ui.genericfilter.modifyConfiguration";

    public UiGenericFilterModifyConfigurationContext() {
        super(NAME);
    }
}
