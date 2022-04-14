/*
 * Copyright (c) 2008-2022 Haulmont.
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

package io.jmix.flowui.xml.layout.loader.component;

import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.xml.layout.support.DataLoaderSupport;

public class DatePickerLoader extends AbstractTypedFieldLoader<TypedDatePicker<?>> {

    protected DataLoaderSupport dataLoaderSupport;

    public DataLoaderSupport getDataLoaderSupport() {
        if (dataLoaderSupport == null) {
            dataLoaderSupport = applicationContext.getBean(DataLoaderSupport.class, context);
        }
        return dataLoaderSupport;
    }

    @Override
    protected TypedDatePicker<?> createComponent() {
        return factory.create(TypedDatePicker.class);
    }

    @Override
    public void loadComponent() {
        super.loadComponent();

        getDataLoaderSupport().loadData(resultComponent, element);

        loadString(element, "name", resultComponent::setName);
        loadString(element, "label", resultComponent::setLabel);
        loadBoolean(element, "opened", resultComponent::setOpened);
        loadBoolean(element, "invalid", resultComponent::setInvalid);
        loadBoolean(element, "autoOpen", resultComponent::setAutoOpen);
        loadString(element, "placeHolder", resultComponent::setPlaceholder);
        loadBoolean(element, "weekNumbersVisible", resultComponent::setWeekNumbersVisible);
        loadBoolean(element, "clearButtonVisible", resultComponent::setClearButtonVisible);
        loadResourceString("errorMessage", context.getMessageGroup(), resultComponent::setErrorMessage);
        loadBoolean(element, "requiredIndicatorVisible", resultComponent::setRequiredIndicatorVisible);

        componentLoader().loadEnabled(resultComponent, element);
        componentLoader().loadThemeName(resultComponent, element);
        componentLoader().loadClassName(resultComponent, element);
        componentLoader().loadHelperText(resultComponent, element);
        componentLoader().loadSizeAttributes(resultComponent, element);
        componentLoader().loadRequiredMessage(resultComponent, context);
        componentLoader().loadValueAndElementAttributes(resultComponent, element);
    }
}