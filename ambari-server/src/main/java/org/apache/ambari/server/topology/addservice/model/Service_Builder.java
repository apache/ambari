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
package org.apache.ambari.server.topology.addservice.model;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

/**
 * Auto-generated superclass of {@link Service.Builder}, derived from the API of {@link Service}.
 */
abstract class Service_Builder {

  /** Creates a new builder using {@code value} as a template. */
  public static Service.Builder from(Service value) {
    return new Service.Builder().mergeFrom(value);
  }

  private enum Property {
    NAME("name"),
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

  private String name;
  // Store a nullable object instead of an Optional. Escape analysis then
  // allows the JVM to optimize away the Optional objects created by and
  // passed to our API.
  private Boolean credentialStoreEnabled = null;
  private final EnumSet<Service_Builder.Property> _unsetProperties =
      EnumSet.allOf(Service_Builder.Property.class);

  /**
   * Sets the value to be returned by {@link Service#name()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code name} is null
   */
  public Service.Builder name(String name) {
    this.name = Objects.requireNonNull(name);
    _unsetProperties.remove(Service_Builder.Property.NAME);
    return (Service.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link Service#name()} by applying {@code mapper} to it
   * and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public Service.Builder mapName(UnaryOperator<String> mapper) {
    Objects.requireNonNull(mapper);
    return name(mapper.apply(name()));
  }

  /**
   * Returns the value that will be returned by {@link Service#name()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String name() {
    if (_unsetProperties.contains(Service_Builder.Property.NAME)) {
      throw new IllegalStateException("name not set");
    }
    return name;
  }

  /**
   * Sets the value to be returned by {@link Service#credentialStoreEnabled()}.
   *
   * @return this {@code Builder} object
   */
  public Service.Builder credentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
    return (Service.Builder) this;
  }

  /**
   * Sets the value to be returned by {@link Service#credentialStoreEnabled()}.
   *
   * @return this {@code Builder} object
   */
  public Service.Builder credentialStoreEnabled(
      Optional<? extends Boolean> credentialStoreEnabled) {
    if (credentialStoreEnabled.isPresent()) {
      return credentialStoreEnabled(credentialStoreEnabled.get());
    } else {
      return clearCredentialStoreEnabled();
    }
  }

  /**
   * Sets the value to be returned by {@link Service#credentialStoreEnabled()}.
   *
   * @return this {@code Builder} object
   */
  public Service.Builder nullableCredentialStoreEnabled(@Nullable Boolean credentialStoreEnabled) {
    if (credentialStoreEnabled != null) {
      return credentialStoreEnabled(credentialStoreEnabled);
    } else {
      return clearCredentialStoreEnabled();
    }
  }

  /**
   * If the value to be returned by {@link Service#credentialStoreEnabled()} is present, replaces it
   * by applying {@code mapper} to it and using the result.
   *
   * <p>If the result is null, clears the value.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null
   */
  public Service.Builder mapCredentialStoreEnabled(UnaryOperator<Boolean> mapper) {
    return credentialStoreEnabled(credentialStoreEnabled().map(mapper));
  }

  /**
   * Sets the value to be returned by {@link Service#credentialStoreEnabled()} to {@link
   * Optional#empty() Optional.empty()}.
   *
   * @return this {@code Builder} object
   */
  public Service.Builder clearCredentialStoreEnabled() {
    credentialStoreEnabled = null;
    return (Service.Builder) this;
  }

  /** Returns the value that will be returned by {@link Service#credentialStoreEnabled()}. */
  public Optional<Boolean> credentialStoreEnabled() {
    return Optional.ofNullable(credentialStoreEnabled);
  }

  /** Sets all property values using the given {@code Service} as a template. */
  public Service.Builder mergeFrom(Service value) {
    Service_Builder _defaults = new Service.Builder();
    if (_defaults._unsetProperties.contains(Service_Builder.Property.NAME)
        || !Objects.equals(value.name(), _defaults.name())) {
      name(value.name());
    }
    value.credentialStoreEnabled().ifPresent(this::credentialStoreEnabled);
    return (Service.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public Service.Builder mergeFrom(Service.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    Service_Builder base = template;
    Service_Builder _defaults = new Service.Builder();
    if (!base._unsetProperties.contains(Service_Builder.Property.NAME)
        && (_defaults._unsetProperties.contains(Service_Builder.Property.NAME)
            || !Objects.equals(template.name(), _defaults.name()))) {
      name(template.name());
    }
    template.credentialStoreEnabled().ifPresent(this::credentialStoreEnabled);
    return (Service.Builder) this;
  }

  /** Resets the state of this builder. */
  public Service.Builder clear() {
    Service_Builder _defaults = new Service.Builder();
    name = _defaults.name;
    credentialStoreEnabled = _defaults.credentialStoreEnabled;
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (Service.Builder) this;
  }

  /**
   * Returns a newly-created {@link Service} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public Service build() {
    if (!_unsetProperties.isEmpty()) {
      throw new IllegalStateException("Not set: " + _unsetProperties);
    }
    return new Service_Builder.Value(this);
  }

  /**
   * Returns a newly-created partial {@link Service} for use in unit tests. State checking will not
   * be performed. Unset properties will throw an {@link UnsupportedOperationException} when
   * accessed via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  public Service buildPartial() {
    return new Service_Builder.Partial(this);
  }

  private static final class Value implements Service {
    private final String name;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final Boolean credentialStoreEnabled;

    private Value(Service_Builder builder) {
      this.name = builder.name;
      this.credentialStoreEnabled = builder.credentialStoreEnabled;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Optional<Boolean> credentialStoreEnabled() {
      return Optional.ofNullable(credentialStoreEnabled);
    }

    @Override
    public Service.Builder toBuilder() {
      return new Service.Builder().mergeFrom(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Service_Builder.Value)) {
        return false;
      }
      Service_Builder.Value other = (Service_Builder.Value) obj;
      return Objects.equals(name, other.name)
          && Objects.equals(credentialStoreEnabled, other.credentialStoreEnabled);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, credentialStoreEnabled);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder("Service{");
      String separator = "";
      result.append("name=").append(name);
      separator = ", ";
      if (credentialStoreEnabled != null) {
        result.append(separator);
        result.append("credentialStoreEnabled=").append(credentialStoreEnabled);
      }
      result.append("}");
      return result.toString();
    }
  }

  private static final class Partial implements Service {
    private final String name;
    // Store a nullable object instead of an Optional. Escape analysis then
    // allows the JVM to optimize away the Optional objects created by our
    // getter method.
    private final Boolean credentialStoreEnabled;
    private final EnumSet<Service_Builder.Property> _unsetProperties;

    Partial(Service_Builder builder) {
      this.name = builder.name;
      this.credentialStoreEnabled = builder.credentialStoreEnabled;
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public String name() {
      if (_unsetProperties.contains(Service_Builder.Property.NAME)) {
        throw new UnsupportedOperationException("name not set");
      }
      return name;
    }

    @Override
    public Optional<Boolean> credentialStoreEnabled() {
      return Optional.ofNullable(credentialStoreEnabled);
    }

    private static class PartialBuilder extends Service.Builder {
      @Override
      public Service build() {
        return buildPartial();
      }
    }

    @Override
    public Service.Builder toBuilder() {
      Service.Builder builder = new PartialBuilder();
      if (!_unsetProperties.contains(Service_Builder.Property.NAME)) {
        builder.name(name);
      }
      builder.nullableCredentialStoreEnabled(credentialStoreEnabled);
      return builder;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Service_Builder.Partial)) {
        return false;
      }
      Service_Builder.Partial other = (Service_Builder.Partial) obj;
      return Objects.equals(name, other.name)
          && Objects.equals(credentialStoreEnabled, other.credentialStoreEnabled)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, credentialStoreEnabled, _unsetProperties);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder("partial Service{");
      String separator = "";
      if (!_unsetProperties.contains(Service_Builder.Property.NAME)) {
        result.append("name=").append(name);
        separator = ", ";
      }
      if (credentialStoreEnabled != null) {
        result.append(separator);
        result.append("credentialStoreEnabled=").append(credentialStoreEnabled);
      }
      result.append("}");
      return result.toString();
    }
  }
}
