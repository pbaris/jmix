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

package io.jmix.flowui.kit.component.valuepicker;

import com.google.common.base.Preconditions;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.shared.SlotUtils;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.*;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Represents a button component that supports integration with actions, handling of
 * shortcut keys, and customizable visual appearance, including an icon.
 * <p>
 * This component is primarily designed to be used with {@link ValuePicker} component.
 */
@Tag("jmix-value-picker-button")
@JsModule("./src/value-picker-button/jmix-value-picker-button.js")
public class ValuePickerButton extends Component
        implements ClickNotifier<ValuePickerButton>, Focusable<ValuePickerButton>,
        HasStyle, HasTheme, HasTitle, HasAction, HasShortcutCombination {


    protected ValuePickerButtonActionSupport actionSupport;

    protected Component iconComponent;
    protected ShortcutRegistration shortcutRegistration;
    protected KeyCombination shortcutCombination;

    @Override
    public void setAction(@Nullable Action action, boolean overrideComponentProperties) {
        getActionSupport().setAction(action, overrideComponentProperties);
    }

    @Nullable
    @Override
    public Action getAction() {
        return getActionSupport().getAction();
    }

    protected ValuePickerButtonActionSupport getActionSupport() {
        if (actionSupport == null) {
            actionSupport = createActionSupport();
        }

        return actionSupport;
    }

    protected ValuePickerButtonActionSupport createActionSupport() {
        return new ValuePickerButtonActionSupport(this);
    }

    /**
     * Returns the icon component associated with this instance.
     *
     * @return the icon component
     * @throws IllegalStateException if no icon component is set
     */
    public Component getIcon() {
        Preconditions.checkState(iconComponent != null, "No icon set");

        return iconComponent;
    }

    /**
     * Sets the icon for the component. The provided icon must be a valid
     * component and cannot be a text node. If an icon is already set, it will
     * be replaced with the new one.
     *
     * @param icon the component to be used as an icon; must not be null
     * @throws NullPointerException     if the icon is null
     * @throws IllegalArgumentException if the icon is a text node
     */
    public void setIcon(Component icon) {
        Preconditions.checkNotNull(icon, "Icon cannot be null");

        if (icon.getElement().isTextNode()) {
            throw new IllegalArgumentException("Text node can't be used as an icon.");
        }

        if (iconComponent != null) {
            remove(iconComponent);
        }

        iconComponent = icon;

        add(icon);
    }

    protected void add(Component component) {
        SlotUtils.addToSlot(this, "icon", component);
    }

    protected void remove(Component component) {
        if (getElement().equals(component.getElement().getParent())) {
            component.getElement().removeAttribute("slot");
            getElement().removeChild(component.getElement());
        } else {
            throw new IllegalArgumentException("The given component ("
                    + component + ") is not a child of this component");
        }
    }

    @Nullable
    @Override
    public KeyCombination getShortcutCombination() {
        return shortcutCombination;
    }

    @Override
    public void setShortcutCombination(@Nullable KeyCombination shortcutCombination) {
        KeyCombination oldValue = getShortcutCombination();
        if (!Objects.equals(oldValue, shortcutCombination)) {
            this.shortcutCombination = shortcutCombination;

            if (shortcutRegistration != null) {
                shortcutRegistration.remove();
                shortcutRegistration = null;
            }

            if (shortcutCombination != null) {
                shortcutRegistration = ComponentUtils.addClickShortcut(this, shortcutCombination);
            }
        }
    }
}
