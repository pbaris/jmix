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

package io.jmix.flowui.xml.layout.loader.html;

import com.vaadin.flow.component.html.FieldSet;

public class FieldSetLoader extends AbstractHtmlContainerLoader<FieldSet> {

    @Override
    protected FieldSet createComponent() {
        return factory.create(FieldSet.class);
    }

    @Override
    public void loadComponent() {
        super.loadComponent();

        componentLoader().loadAriaLabel(resultComponent, element);
        loadString(element, "legendText", resultComponent::setLegendText);
    }
}
