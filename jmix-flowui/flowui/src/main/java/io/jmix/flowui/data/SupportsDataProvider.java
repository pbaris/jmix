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

package io.jmix.flowui.data;

import com.vaadin.flow.data.provider.DataProvider;
import org.springframework.lang.Nullable;

/**
 * Defines a contract for components that support the use of a {@link DataProvider} to manage and provide their data.
 *
 * @param <V> the type of item provided by the {@link DataProvider}
 */
public interface SupportsDataProvider<V> {

    /**
     * Returns the {@link DataProvider} associated with this component.
     *
     * @return the {@link DataProvider} instance used for managing and providing data,
     * or {@code null} if no data provider is set
     */
    @Nullable
    DataProvider<V, ?> getDataProvider();
}
