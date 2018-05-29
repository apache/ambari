/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import org.apache.ambari.server.state.StackId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * Auto-generated superclass of {@link ResolvedComponent.Builder}, derived from the API of {@link
 * ResolvedComponent}.
 */
abstract class ResolvedComponent_Builder implements ResolvedComponent {

  /** Creates a new builder using {@code value} as a template. */
  public static ResolvedComponent.Builder from(ResolvedComponent value) {
    return new ResolvedComponent.Builder().mergeFrom(value);
  }

  private static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

  private enum Property {
    STACK_ID("stackId"),
    SERVICE_TYPE("serviceType"),
    COMPONENT_NAME("componentName"),
    COMPONENT("component"),
    ;

    private final String name;

    private Property(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private StackId stackId;
  // Store a nullable object instead of an Optional. Escape analysis then
  // allows the JVM to optimize away the Optional objects created by and
  // passed to our API.
  private String serviceGroupName = null;
  private String serviceType;
  // Store a nullable object instead of an Optional. Escape analysis then
  // allows the JVM to optimize away the Optional objects created by and
  // passed to our API.
  private String serviceName = null;
  private String componentName;
  private boolean masterComponent;
  private Component component;
  private final EnumSet<Property> _unsetProperties =
      EnumSet.allOf(ResolvedComponent_Builder.Property.class);

  /**
   * Sets the value to be returned by {@link ResolvedComponent#stackId()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code stackId} is null
   */
  public ResolvedComponent.Builder stackId(StackId stackId) {
    this.stackId = Preconditions.checkNotNull(stackId);
    _unsetProperties.remove(ResolvedComponent_Builder.Property.STACK_ID);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link ResolvedComponent#stackId()} by applying {@code
   * mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public ResolvedComponent.Builder mapStackId(UnaryOperator<StackId> mapper) {
    Preconditions.checkNotNull(mapper);
    return stackId(mapper.apply(stackId()));
  }

  /**
   * Returns the value that will be returned by {@link ResolvedComponent#stackId()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public StackId stackId() {
    Preconditions.checkState(
        !_unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID), "stackId not set");
    return stackId;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceGroupName()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code serviceGroupName} is null
   */
  public ResolvedComponent.Builder serviceGroupName(String serviceGroupName) {
    this.serviceGroupName = Preconditions.checkNotNull(serviceGroupName);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceGroupName()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder serviceGroupName(Optional<? extends String> serviceGroupName) {
    if (serviceGroupName.isPresent()) {
      return serviceGroupName(serviceGroupName.get());
    } else {
      return clearServiceGroupName();
    }
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceGroupName()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder nullableServiceGroupName(@Nullable String serviceGroupName) {
    if (serviceGroupName != null) {
      return serviceGroupName(serviceGroupName);
    } else {
      return clearServiceGroupName();
    }
  }

  /**
   * If the value to be returned by {@link ResolvedComponent#serviceGroupName()} is present,
   * replaces it by applying {@code mapper} to it and using the result.
   *
   * <p>If the result is null, clears the value.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null
   */
  public ResolvedComponent.Builder mapServiceGroupName(UnaryOperator<String> mapper) {
    return serviceGroupName(serviceGroupName().map(mapper));
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceGroupName()} to {@link
   * Optional#empty() Optional.empty()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder clearServiceGroupName() {
    serviceGroupName = null;
    return (ResolvedComponent.Builder) this;
  }

  /** Returns the value that will be returned by {@link ResolvedComponent#serviceGroupName()}. */
  public Optional<String> serviceGroupName() {
    return Optional.ofNullable(serviceGroupName);
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceType()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code serviceType} is null
   */
  public ResolvedComponent.Builder serviceType(String serviceType) {
    this.serviceType = Preconditions.checkNotNull(serviceType);
    _unsetProperties.remove(ResolvedComponent_Builder.Property.SERVICE_TYPE);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link ResolvedComponent#serviceType()} by applying {@code
   * mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public ResolvedComponent.Builder mapServiceType(UnaryOperator<String> mapper) {
    Preconditions.checkNotNull(mapper);
    return serviceType(mapper.apply(serviceType()));
  }

  /**
   * Returns the value that will be returned by {@link ResolvedComponent#serviceType()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String serviceType() {
    Preconditions.checkState(
        !_unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE),
        "serviceType not set");
    return serviceType;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceName()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code serviceName} is null
   */
  public ResolvedComponent.Builder serviceName(String serviceName) {
    this.serviceName = Preconditions.checkNotNull(serviceName);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceName()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder serviceName(Optional<? extends String> serviceName) {
    if (serviceName.isPresent()) {
      return serviceName(serviceName.get());
    } else {
      return clearServiceName();
    }
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceName()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder nullableServiceName(@Nullable String serviceName) {
    if (serviceName != null) {
      return serviceName(serviceName);
    } else {
      return clearServiceName();
    }
  }

  /**
   * If the value to be returned by {@link ResolvedComponent#serviceName()} is present, replaces it
   * by applying {@code mapper} to it and using the result.
   *
   * <p>If the result is null, clears the value.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null
   */
  public ResolvedComponent.Builder mapServiceName(UnaryOperator<String> mapper) {
    return serviceName(serviceName().map(mapper));
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#serviceName()} to {@link
   * Optional#empty() Optional.empty()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder clearServiceName() {
    serviceName = null;
    return (ResolvedComponent.Builder) this;
  }

  /** Returns the value that will be returned by {@link ResolvedComponent#serviceName()}. */
  public Optional<String> serviceName() {
    return Optional.ofNullable(serviceName);
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#componentName()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code componentName} is null
   */
  public ResolvedComponent.Builder componentName(String componentName) {
    this.componentName = Preconditions.checkNotNull(componentName);
    _unsetProperties.remove(ResolvedComponent_Builder.Property.COMPONENT_NAME);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link ResolvedComponent#componentName()} by applying
   * {@code mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public ResolvedComponent.Builder mapComponentName(UnaryOperator<String> mapper) {
    Preconditions.checkNotNull(mapper);
    return componentName(mapper.apply(componentName()));
  }

  /**
   * Returns the value that will be returned by {@link ResolvedComponent#componentName()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String componentName() {
    Preconditions.checkState(
        !_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME),
        "componentName not set");
    return componentName;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#masterComponent()}.
   *
   * @return this {@code Builder} object
   */
  public ResolvedComponent.Builder masterComponent(boolean masterComponent) {
    this.masterComponent = masterComponent;
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link ResolvedComponent#masterComponent()} by applying
   * {@code mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   */
  public ResolvedComponent.Builder mapMasterComponent(UnaryOperator<Boolean> mapper) {
    return masterComponent(mapper.apply(masterComponent()));
  }

  /** Returns the value that will be returned by {@link ResolvedComponent#masterComponent()}. */
  public boolean masterComponent() {
    return masterComponent;
  }

  /**
   * Sets the value to be returned by {@link ResolvedComponent#component()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code component} is null
   */
  public ResolvedComponent.Builder component(Component component) {
    this.component = Preconditions.checkNotNull(component);
    _unsetProperties.remove(ResolvedComponent_Builder.Property.COMPONENT);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link ResolvedComponent#component()} by applying {@code
   * mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public ResolvedComponent.Builder mapComponent(UnaryOperator<Component> mapper) {
    Preconditions.checkNotNull(mapper);
    return component(mapper.apply(component()));
  }

  /**
   * Returns the value that will be returned by {@link ResolvedComponent#component()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public Component component() {
    Preconditions.checkState(
        !_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT),
        "component not set");
    return component;
  }

  /** Sets all property values using the given {@code ResolvedComponent} as a template. */
  public ResolvedComponent.Builder mergeFrom(ResolvedComponent value) {
    ResolvedComponent_Builder _defaults = new ResolvedComponent.Builder();
    if (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID)
        || !Objects.equals(value.stackId(), _defaults.stackId())) {
      stackId(value.stackId());
    }
    value.serviceGroupName().ifPresent(this::serviceGroupName);
    if (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE)
        || !Objects.equals(value.serviceType(), _defaults.serviceType())) {
      serviceType(value.serviceType());
    }
    value.serviceName().ifPresent(this::serviceName);
    if (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME)
        || !Objects.equals(value.componentName(), _defaults.componentName())) {
      componentName(value.componentName());
    }
    if (!Objects.equals(value.masterComponent(), _defaults.masterComponent())) {
      masterComponent(value.masterComponent());
    }
    if (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT)
        || !Objects.equals(value.component(), _defaults.component())) {
      component(value.component());
    }
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public ResolvedComponent.Builder mergeFrom(ResolvedComponent.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    ResolvedComponent_Builder base = template;
    ResolvedComponent_Builder _defaults = new ResolvedComponent.Builder();
    if (!base._unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID)
        && (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID)
            || !Objects.equals(template.stackId(), _defaults.stackId()))) {
      stackId(template.stackId());
    }
    template.serviceGroupName().ifPresent(this::serviceGroupName);
    if (!base._unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE)
        && (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE)
            || !Objects.equals(template.serviceType(), _defaults.serviceType()))) {
      serviceType(template.serviceType());
    }
    template.serviceName().ifPresent(this::serviceName);
    if (!base._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME)
        && (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME)
            || !Objects.equals(template.componentName(), _defaults.componentName()))) {
      componentName(template.componentName());
    }
    if (!Objects.equals(template.masterComponent(), _defaults.masterComponent())) {
      masterComponent(template.masterComponent());
    }
    if (!base._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT)
        && (_defaults._unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT)
            || !Objects.equals(template.component(), _defaults.component()))) {
      component(template.component());
    }
    return (ResolvedComponent.Builder) this;
  }

  /** Resets the state of this builder. */
  public ResolvedComponent.Builder clear() {
    ResolvedComponent_Builder _defaults = new ResolvedComponent.Builder();
    stackId = _defaults.stackId;
    serviceGroupName = _defaults.serviceGroupName;
    serviceType = _defaults.serviceType;
    serviceName = _defaults.serviceName;
    componentName = _defaults.componentName;
    masterComponent = _defaults.masterComponent;
    component = _defaults.component;
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (ResolvedComponent.Builder) this;
  }

  /**
   * Returns a newly-created {@link ResolvedComponent} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public ResolvedComponent build() {
    Preconditions.checkState(_unsetProperties.isEmpty(), "Not set: %s", _unsetProperties);
    return new ResolvedComponent_Builder.Value(this);
  }

  /**
   * Returns a newly-created partial {@link ResolvedComponent} for use in unit tests. State checking
   * will not be performed. Unset properties will throw an {@link UnsupportedOperationException}
   * when accessed via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  @VisibleForTesting()
  public ResolvedComponent buildPartial() {
    return new ResolvedComponent_Builder.Partial(this);
  }

  private static final class Value implements ResolvedComponent {
    private final StackId stackId;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final String serviceGroupName;
    private final String serviceType;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final String serviceName;
    private final String componentName;
    private final boolean masterComponent;
    private final Component component;

    private Value(ResolvedComponent_Builder builder) {
      this.stackId = builder.stackId;
      this.serviceGroupName = builder.serviceGroupName;
      this.serviceType = builder.serviceType;
      this.serviceName = builder.serviceName;
      this.componentName = builder.componentName;
      this.masterComponent = builder.masterComponent;
      this.component = builder.component;
    }

    @Override
    public StackId stackId() {
      return stackId;
    }

    @Override
    public Optional<String> serviceGroupName() {
      return Optional.ofNullable(serviceGroupName);
    }

    @Override
    public String serviceType() {
      return serviceType;
    }

    @Override
    public Optional<String> serviceName() {
      return Optional.ofNullable(serviceName);
    }

    @Override
    public String componentName() {
      return componentName;
    }

    @Override
    public boolean masterComponent() {
      return masterComponent;
    }

    @Override
    public Component component() {
      return component;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ResolvedComponent_Builder.Value)) {
        return false;
      }
      ResolvedComponent_Builder.Value other = (ResolvedComponent_Builder.Value) obj;
      return Objects.equals(stackId, other.stackId)
          && Objects.equals(serviceGroupName, other.serviceGroupName)
          && Objects.equals(serviceType, other.serviceType)
          && Objects.equals(serviceName, other.serviceName)
          && Objects.equals(componentName, other.componentName)
          && Objects.equals(masterComponent, other.masterComponent)
          && Objects.equals(component, other.component);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          stackId,
          serviceGroupName,
          serviceType,
          serviceName,
          componentName,
          masterComponent,
          component);
    }

    @Override
    public String toString() {
      return "ResolvedComponent{"
          + COMMA_JOINER.join(
              "stackId=" + stackId,
              (serviceGroupName != null ? "serviceGroupName=" + serviceGroupName : null),
              "serviceType=" + serviceType,
              (serviceName != null ? "serviceName=" + serviceName : null),
              "componentName=" + componentName,
              "masterComponent=" + masterComponent,
              "component=" + component)
          + "}";
    }
  }

  private static final class Partial implements ResolvedComponent {
    private final StackId stackId;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final String serviceGroupName;
    private final String serviceType;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final String serviceName;
    private final String componentName;
    private final boolean masterComponent;
    private final Component component;
    private final EnumSet<ResolvedComponent_Builder.Property> _unsetProperties;

    Partial(ResolvedComponent_Builder builder) {
      this.stackId = builder.stackId;
      this.serviceGroupName = builder.serviceGroupName;
      this.serviceType = builder.serviceType;
      this.serviceName = builder.serviceName;
      this.componentName = builder.componentName;
      this.masterComponent = builder.masterComponent;
      this.component = builder.component;
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public StackId stackId() {
      if (_unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID)) {
        throw new UnsupportedOperationException("stackId not set");
      }
      return stackId;
    }

    @Override
    public Optional<String> serviceGroupName() {
      return Optional.ofNullable(serviceGroupName);
    }

    @Override
    public String serviceType() {
      if (_unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE)) {
        throw new UnsupportedOperationException("serviceType not set");
      }
      return serviceType;
    }

    @Override
    public Optional<String> serviceName() {
      return Optional.ofNullable(serviceName);
    }

    @Override
    public String componentName() {
      if (_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME)) {
        throw new UnsupportedOperationException("componentName not set");
      }
      return componentName;
    }

    @Override
    public boolean masterComponent() {
      return masterComponent;
    }

    @Override
    public Component component() {
      if (_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT)) {
        throw new UnsupportedOperationException("component not set");
      }
      return component;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ResolvedComponent_Builder.Partial)) {
        return false;
      }
      ResolvedComponent_Builder.Partial other = (ResolvedComponent_Builder.Partial) obj;
      return Objects.equals(stackId, other.stackId)
          && Objects.equals(serviceGroupName, other.serviceGroupName)
          && Objects.equals(serviceType, other.serviceType)
          && Objects.equals(serviceName, other.serviceName)
          && Objects.equals(componentName, other.componentName)
          && Objects.equals(masterComponent, other.masterComponent)
          && Objects.equals(component, other.component)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          stackId,
          serviceGroupName,
          serviceType,
          serviceName,
          componentName,
          masterComponent,
          component,
          _unsetProperties);
    }

    @Override
    public String toString() {
      return "partial ResolvedComponent{"
          + COMMA_JOINER.join(
              (!_unsetProperties.contains(ResolvedComponent_Builder.Property.STACK_ID)
                  ? "stackId=" + stackId
                  : null),
              (serviceGroupName != null ? "serviceGroupName=" + serviceGroupName : null),
              (!_unsetProperties.contains(ResolvedComponent_Builder.Property.SERVICE_TYPE)
                  ? "serviceType=" + serviceType
                  : null),
              (serviceName != null ? "serviceName=" + serviceName : null),
              (!_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT_NAME)
                  ? "componentName=" + componentName
                  : null),
              "masterComponent=" + masterComponent,
              (!_unsetProperties.contains(ResolvedComponent_Builder.Property.COMPONENT)
                  ? "component=" + component
                  : null))
          + "}";
    }
  }
}
