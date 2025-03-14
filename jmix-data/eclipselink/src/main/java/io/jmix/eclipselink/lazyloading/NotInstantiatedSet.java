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

package io.jmix.eclipselink.lazyloading;

import io.jmix.core.entity.NoValueCollection;
import org.eclipse.persistence.indirection.IndirectSet;

/**
 * Use for kotlin non-null references to prevent eager instantiation of lazy-loaded fields.
 *
 * @see NoValueCollection
 */
public class NotInstantiatedSet<E> extends IndirectSet<E> implements NoValueCollection {
    @Override
    public boolean isInstantiated() {
        return false;
    }
}
