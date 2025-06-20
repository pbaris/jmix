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

package io.jmix.flowui.facet.urlqueryparameters;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.AccessManager;
import io.jmix.core.MetadataTools;
import io.jmix.core.accesscontext.EntityAttributeContext;
import io.jmix.core.entity.annotation.SystemLevel;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.core.querycondition.PropertyConditionUtils;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.filter.FilterComponent;
import io.jmix.flowui.component.genericfilter.Configuration;
import io.jmix.flowui.component.genericfilter.FilterUtils;
import io.jmix.flowui.component.genericfilter.GenericFilter;
import io.jmix.flowui.component.genericfilter.configuration.RunTimeConfiguration;
import io.jmix.flowui.component.logicalfilter.LogicalFilterComponent;
import io.jmix.flowui.component.logicalfilter.LogicalFilterComponent.FilterComponentsChangeEvent;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.component.propertyfilter.SingleFilterSupport;
import io.jmix.flowui.facet.UrlQueryParametersFacet.UrlQueryParametersChangeEvent;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataLoader;
import io.jmix.flowui.model.KeyValueCollectionLoader;
import io.jmix.flowui.view.navigation.UrlParamSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static io.jmix.flowui.facet.urlqueryparameters.FilterUrlQueryParametersSupport.SEPARATOR;
import static java.util.Objects.requireNonNull;

/**
 * A binder class for managing URL query parameters related to a {@link GenericFilter} component.
 * This class facilitates the serialization and deserialization of query parameters,
 * manages component state, and ensures updates based on the URL query parameters or filter configuration changes.
 */
public class GenericFilterUrlQueryParametersBinder extends AbstractUrlQueryParametersBinder
        implements HasInitialState {

    private static final Logger log = LoggerFactory.getLogger(GenericFilterUrlQueryParametersBinder.class);

    public static final String NAME = "genericFilter";

    public static final String PROPERTY_CONDITION_PREFIX = "property:";
    public static final String DEFAULT_CONFIGURATION_PARAM = "genericFilterConfiguration";
    public static final String DEFAULT_CONDITION_PARAM = "genericFilterCondition";

    protected GenericFilter filter;

    protected String configurationParam;
    protected String conditionParam;

    protected ApplicationContext applicationContext;
    protected UrlParamSerializer urlParamSerializer;
    protected UiComponents uiComponents;
    protected SingleFilterSupport singleFilterSupport;
    protected MetadataTools metadataTools;
    protected FilterUrlQueryParametersSupport filterUrlQueryParametersSupport;
    protected AccessManager accessManager;

    protected Registration filterComponentsChangeRegistration;

    protected InitialState initialState;

    public GenericFilterUrlQueryParametersBinder(GenericFilter filter,
                                                 UrlParamSerializer urlParamSerializer,
                                                 ApplicationContext applicationContext) {
        this.filter = filter;
        this.urlParamSerializer = urlParamSerializer;
        this.applicationContext = applicationContext;

        autowireDependencies();
        initComponent(filter);
    }

    protected void autowireDependencies() {
        uiComponents = applicationContext.getBean(UiComponents.class);
        filterUrlQueryParametersSupport = applicationContext.getBean(FilterUrlQueryParametersSupport.class);
        accessManager = applicationContext.getBean(AccessManager.class);
    }

    protected void initComponent(GenericFilter filter) {
        filter.addConfigurationChangeListener(this::onConfigurationChanged);
        bindFilterComponentsChangeListener(filter);
        bindDataLoaderListener(filter);
    }

    @Override
    public void saveInitialState() {
        initialState = new InitialState(filter.getCurrentConfiguration());
    }

    protected void bindDataLoaderListener(GenericFilter filter) {
        DataLoader dataLoader = filter.getDataLoader();
        if (dataLoader instanceof CollectionLoader) {
            ((CollectionLoader<?>) dataLoader).addPostLoadListener(this::onPostLoad);
        } else if (dataLoader instanceof KeyValueCollectionLoader) {
            ((KeyValueCollectionLoader) dataLoader).addPostLoadListener(this::onPostLoad);
        }
    }

    protected void onPostLoad(EventObject event) {
        updateQueryParameters();
    }

    protected void onConfigurationChanged(GenericFilter.ConfigurationChangeEvent event) {
        unbindFilterComponentsChange();
        bindFilterComponentsChangeListener(event.getSource());

        updateQueryParameters();
    }

    protected void onFilterComponentsChanged(FilterComponentsChangeEvent<?> event) {
        updateQueryParameters();
    }

    protected void updateQueryParameters() {
        ImmutableMap<String, List<String>> parametersMap = serializeQueryParameters();

        QueryParameters queryParameters = new QueryParameters(parametersMap);
        fireQueryParametersChanged(new UrlQueryParametersChangeEvent(this, queryParameters));
    }

    /**
     * Serializes query parameters into an immutable map. The method processes the current
     * configuration and its associated conditions to generate query parameters.
     *
     * @return an immutable map containing serialized query parameters. The keys represent
     * parameter types (e.g., configuration and condition parameters), while the
     * values are lists of their respective serialized representations.
     */
    public ImmutableMap<String, List<String>> serializeQueryParameters() {
        Configuration currentConfiguration = filter.getCurrentConfiguration();
        LogicalCondition queryCondition = currentConfiguration.getQueryCondition();

        List<Condition> conditions = queryCondition.getConditions();
        List<String> conditionParams = new ArrayList<>(conditions.size());
        List<String> configurationParam;

        if (currentConfiguration != filter.getEmptyConfiguration()) {
            configurationParam = Collections.singletonList(serializeConfigurationId(currentConfiguration));
        } else {
            configurationParam = Collections.emptyList();
        }

        for (Condition condition : conditions) {
            if (condition instanceof PropertyCondition) {
                conditionParams.add(serializePropertyCondition(((PropertyCondition) condition)));
            }
        }

        return ImmutableMap.of(
                getConfigurationParam(), configurationParam,
                getConditionParam(), conditionParams
        );
    }

    protected String serializeConfigurationId(Configuration configuration) {
        return urlParamSerializer.serialize(configuration.getId());
    }

    protected String serializePropertyCondition(PropertyCondition condition) {
        String property = urlParamSerializer.serialize(
                filterUrlQueryParametersSupport.replaceSeparatorValue(condition.getProperty()));
        String operation = urlParamSerializer.serialize(
                filterUrlQueryParametersSupport.replaceSeparatorValue(condition.getOperation()));
        Object parameterValue = urlParamSerializer.serialize(
                filterUrlQueryParametersSupport.getSerializableValue(condition.getParameterValue()));

        return PROPERTY_CONDITION_PREFIX +
                property + SEPARATOR +
                operation + SEPARATOR +
                parameterValue;
    }

    @Override
    public void applyInitialState() {
        if (initialState.configuration instanceof RunTimeConfiguration runTimeConfiguration) {
            runTimeConfiguration.getRootLogicalFilterComponent().removeAll();
        }

        filter.setCurrentConfiguration(initialState.configuration);
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        Map<String, List<String>> parameters = queryParameters.getParameters();

        if (parameters.containsKey(getConfigurationParam())) {
            List<String> configurationParam = parameters.get(getConfigurationParam());
            String configurationId = deserializeConfigurationId(configurationParam.get(0));
            Optional<Configuration> currentConfiguration = filter.getConfigurations().stream()
                    .filter(configuration -> configurationId.equals(configuration.getId()))
                    .findAny();

            currentConfiguration.ifPresent(configuration -> {
                if (parameters.containsKey(getConditionParam())) {
                    List<String> conditionParams = parameters.get(getConditionParam());
                    updateConfigurationConditions(configuration, conditionParams);
                }

                FilterUtils.setCurrentConfiguration(filter, configuration, true);
            });
        } else if (parameters.containsKey(getConditionParam())) {
            List<String> conditionParams = parameters.get(getConditionParam());

            // For cases where there is a default design-time configuration.
            // The design-time configuration can't be modified, so an empty configuration is entered
            // to be able to add conditions from the URL query parameters.
            Configuration currentConfiguration = filter.getCurrentConfiguration() instanceof RunTimeConfiguration
                    ? filter.getCurrentConfiguration()
                    : filter.getEmptyConfiguration();

            LogicalFilterComponent<?> rootLogicalFilterComponent = currentConfiguration.getRootLogicalFilterComponent();

            List<FilterComponent> conditions = deserializeConditions(conditionParams,
                    rootLogicalFilterComponent.getDataLoader());
            conditions.forEach(filterComponent -> {
                rootLogicalFilterComponent.add(filterComponent);
                currentConfiguration.setFilterComponentModified(filterComponent, true);
            });

            FilterUtils.setCurrentConfiguration(filter, currentConfiguration, true);
        }
    }

    protected String deserializeConfigurationId(String configurationParam) {
        return urlParamSerializer.deserialize(String.class, configurationParam);
    }

    protected List<FilterComponent> deserializeConditions(List<String> conditionParams, DataLoader dataLoader) {
        List<FilterComponent> conditions = new ArrayList<>(conditionParams.size());
        for (String conditionString : conditionParams) {
            FilterComponent filterComponent = parseCondition(conditionString, dataLoader);
            if (isPermitted(dataLoader, filterComponent)) {
                conditions.add(filterComponent);
            }
        }

        return conditions;
    }

    protected boolean isPermitted(DataLoader dataLoader, FilterComponent filterComponent) {
        if (filterComponent instanceof PropertyFilter<?> propertyFilter && propertyFilter.getProperty() != null) {
            MetaClass entityMetaClass = dataLoader.getContainer().getEntityMetaClass();
            MetaPropertyPath propertyPath = getMetadataTools().resolveMetaPropertyPathOrNull(entityMetaClass, propertyFilter.getProperty());

            Predicate<MetaPropertyPath> propertyFiltersPredicate = filter.getPropertyFiltersPredicate();
            if (propertyFiltersPredicate != null && !propertyFiltersPredicate.test(propertyPath)) {
                return false;
            }

            EntityAttributeContext context = new EntityAttributeContext(propertyPath);
            accessManager.applyRegisteredConstraints(context);
            if (!context.canView()) {
                return false;
            }

            return propertyPath == null ||
                    !propertyPath.getMetaProperty().getAnnotatedElement().isAnnotationPresent(SystemLevel.class);
        }
        return true;
    }

    protected void updateConfigurationConditions(Configuration currentConfiguration, List<String> conditionParams) {
        LogicalFilterComponent<?> rootLogicalFilterComponent = currentConfiguration.getRootLogicalFilterComponent();

        List<FilterComponent> conditions = deserializeConditions(conditionParams,
                rootLogicalFilterComponent.getDataLoader());
        List<FilterComponent> configurationComponents = rootLogicalFilterComponent.getFilterComponents();

        for (FilterComponent filterComponent : conditions) {
            FilterComponent usedFilterComponent = null;

            for (int i = 0; i < configurationComponents.size() && usedFilterComponent == null; ++i) {
                FilterComponent configurationComponent = configurationComponents.get(i);

                usedFilterComponent = updateFilterComponent(configurationComponent, filterComponent);
            }

            if (usedFilterComponent != null) {
                configurationComponents.remove(usedFilterComponent);
            }

            if (currentConfiguration instanceof RunTimeConfiguration && usedFilterComponent == null) {
                currentConfiguration.setFilterComponentModified(filterComponent, true);
                rootLogicalFilterComponent.add(filterComponent);
            } else {
                log.debug("Can't add filterComponent to Design-Time Configuration");
            }
        }
    }

    @Nullable
    protected FilterComponent updateFilterComponent(FilterComponent configurationComponent,
                                                    FilterComponent filterComponent) {
        if (configurationComponent instanceof PropertyFilter<?> configurationPropertyFilter
                && filterComponent instanceof PropertyFilter<?> propertyFilter) {
            return updatePropertyCondition(configurationPropertyFilter, propertyFilter);
        }

        /* else if (configurationComponent instanceof JpqlFilter<?> configurationJpqlFilter
                && filterComponent instanceof JpqlFilter<?> jpqlFilter) {
            //TODO: kremnevda, implement 02.06.2023
        } else if (configurationComponent instanceof GroupFilter configurationGroupFilter
                && filterComponent instanceof GroupFilter groupFilter) {
            //TODO: kremnevda, implement 02.06.2023
        }
        */

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    protected FilterComponent updatePropertyCondition(PropertyFilter configurationComponent,
                                                      PropertyFilter filterComponent) {
        if (isPropertyMatched(configurationComponent, filterComponent)) {
            if (configurationComponent.isOperationEditable()) {
                configurationComponent.setOperation(filterComponent.getOperation());
            }

            if (isOperationMatched(configurationComponent, filterComponent)) {
                if (filterComponent.getValue() != null) {
                    UiComponentUtils.setValue(configurationComponent, filterComponent.getValue());
                }

                return configurationComponent;
            }
        }

        return null;
    }

    protected FilterComponent parseCondition(String conditionString, DataLoader dataLoader) {
        if (conditionString.startsWith(PROPERTY_CONDITION_PREFIX)) {
            String propertyConditionString = conditionString.substring(PROPERTY_CONDITION_PREFIX.length());
            return parsePropertyCondition(propertyConditionString, dataLoader);
        }

        throw new IllegalStateException("Unknown condition type: " + conditionString);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected PropertyFilter<?> parsePropertyCondition(String conditionString, DataLoader dataLoader) {
        int separatorIndex = conditionString.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            throw new IllegalStateException("Can't parse property condition: " + conditionString);
        }

        String propertyString = conditionString.substring(0, separatorIndex);
        String property = urlParamSerializer.deserialize(String.class,
                filterUrlQueryParametersSupport.restoreSeparatorValue(propertyString));

        conditionString = conditionString.substring(separatorIndex + 1);
        separatorIndex = conditionString.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            throw new IllegalStateException("Can't parse property condition: " + conditionString);
        }

        String operationString = conditionString.substring(0, separatorIndex);
        PropertyFilter.Operation operation = urlParamSerializer
                .deserialize(PropertyFilter.Operation.class,
                        filterUrlQueryParametersSupport.restoreSeparatorValue(operationString));

        PropertyFilter propertyFilter = uiComponents.create(PropertyFilter.class);
        propertyFilter.setProperty(property);
        propertyFilter.setOperation(operation);
        // TODO: gg, change when configurations and custom conditions will be implemented
        propertyFilter.setOperationEditable(true);

        propertyFilter.setParameterName(PropertyConditionUtils.generateParameterName(property));
        propertyFilter.setDataLoader(dataLoader);

        propertyFilter.setValueComponent(generatePropertyFilterValueComponent(propertyFilter));

        String valueString = conditionString.substring(separatorIndex + 1);
        if (!Strings.isNullOrEmpty(valueString)) {
            try {
                Object parsedValue = filterUrlQueryParametersSupport
                        .parseValue(dataLoader.getContainer().getEntityMetaClass(),
                                property, operation.getType(), valueString);
                propertyFilter.setValue(parsedValue);
            } catch (Exception e) {
                log.info("Cannot parse URL parameter. {}", e.toString());
                propertyFilter.setValue(null);
            }
        }

        return propertyFilter;
    }

    protected HasValueAndElement<?, ?> generatePropertyFilterValueComponent(PropertyFilter<?> propertyFilter) {
        MetaClass metaClass = propertyFilter.getDataLoader().getContainer().getEntityMetaClass();
        return getSingleFilterSupport().generateValueComponent(metaClass,
                requireNonNull(propertyFilter.getProperty()), propertyFilter.getOperation());
    }

    protected void bindFilterComponentsChangeListener(GenericFilter filter) {
        LogicalFilterComponent<?> rootLogicalFilterComponent = filter.getCurrentConfiguration()
                .getRootLogicalFilterComponent();
        filterComponentsChangeRegistration = rootLogicalFilterComponent
                .addFilterComponentsChangeListener(this::onFilterComponentsChanged);
    }

    protected void unbindFilterComponentsChange() {
        if (filterComponentsChangeRegistration != null) {
            filterComponentsChangeRegistration.remove();
            filterComponentsChangeRegistration = null;
        }
    }

    /**
     * Returns the current configuration parameter name for the URL. If the parameter
     * is null or empty, a default name is returned.
     *
     * @return the configuration parameter name if set, otherwise the default configuration parameter name
     */
    public String getConfigurationParam() {
        return Strings.isNullOrEmpty(configurationParam) ? DEFAULT_CONFIGURATION_PARAM : configurationParam;
    }

    /**
     * Sets the condition parameter name for the URL.
     *
     * @param conditionParam the condition parameter name to set
     */
    public void setConfigurationParam(@Nullable String conditionParam) {
        this.conditionParam = conditionParam;
    }

    /**
     * Returns the current condition parameter name for the URL. If the condition parameter
     * is null or empty, a default name is returned.
     *
     * @return the condition parameter name if set, otherwise the default condition parameter name
     */
    public String getConditionParam() {
        return Strings.isNullOrEmpty(conditionParam) ? DEFAULT_CONDITION_PARAM : conditionParam;
    }

    /**
     * Sets the condition parameter name for the URL.
     *
     * @param conditionParam the condition parameter name to set
     */
    public void setConditionParam(@Nullable String conditionParam) {
        this.conditionParam = conditionParam;
    }

    protected boolean isPropertyMatched(PropertyFilter<?> propertyFilter, PropertyFilter<?> anotherPropertyFilter) {
        return Objects.equals(propertyFilter.getProperty(), anotherPropertyFilter.getProperty());
    }

    protected boolean isOperationMatched(PropertyFilter<?> propertyFilter, PropertyFilter<?> anotherPropertyFilter) {
        return Objects.equals(propertyFilter.getOperation(), anotherPropertyFilter.getOperation());
    }

    protected MetadataTools getMetadataTools() {
        if (metadataTools == null) {
            metadataTools = applicationContext.getBean(MetadataTools.class);
        }
        return metadataTools;
    }

    protected SingleFilterSupport getSingleFilterSupport() {
        if (singleFilterSupport == null) {
            singleFilterSupport = applicationContext.getBean(SingleFilterSupport.class);
        }
        return singleFilterSupport;
    }

    @Nullable
    @Override
    public Component getComponent() {
        return filter;
    }

    /**
     * A POJO class for storing configuration of the {@link GenericFilter}'s initial state.
     *
     * @param configuration the value of {@code configuration} at initialization
     */
    protected record InitialState(Configuration configuration) {
    }
}
