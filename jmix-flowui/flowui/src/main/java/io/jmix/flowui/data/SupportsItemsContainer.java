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

import io.jmix.flowui.data.items.ContainerDataProvider;
import io.jmix.flowui.model.CollectionContainer;

/**
 * Interface to be implemented by components that support handling items
 * based on a {@link CollectionContainer}.
 *
 * @param <E> the type of items managed by the container
 */
public interface SupportsItemsContainer<E> {

    /**
     * Sets items from the passed {@link CollectionContainer}.
     *
     * @param container a {@link CollectionContainer} to be used as items source
     * @see ContainerDataProvider
     */
    void setItems(CollectionContainer<E> container);
}
