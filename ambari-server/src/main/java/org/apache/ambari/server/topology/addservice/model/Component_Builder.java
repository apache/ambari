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
import java.util.function.UnaryOperator;

/**
 * Auto-generated superclass of {@link Component.Builder}, derived from the API of {@link
 * Component}.
 */
abstract class Component_Builder {

  /** Creates a new builder using {@code value} as a template. */
  public static Component.Builder from(Component value) {
    return new Component.Builder().mergeFrom(value);
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
  private final EnumSet<Component_Builder.Property> _unsetProperties =
      EnumSet.allOf(Component_Builder.Property.class);

  /**
   * Sets the value to be returned by {@link Component#name()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code name} is null
   */
  public Component.Builder name(String name) {
    this.name = Objects.requireNonNull(name);
    _unsetProperties.remove(Component_Builder.Property.NAME);
    return (Component.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link Component#name()} by applying {@code mapper} to it
   * and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public Component.Builder mapName(UnaryOperator<String> mapper) {
    Objects.requireNonNull(mapper);
    return name(mapper.apply(name()));
  }

  /**
   * Returns the value that will be returned by {@link Component#name()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String name() {
    if (_unsetProperties.contains(Component_Builder.Property.NAME)) {
      throw new IllegalStateException("name not set");
    }
    return name;
  }

  /** Sets all property values using the given {@code Component} as a template. */
  public Component.Builder mergeFrom(Component value) {
    Component_Builder _defaults = new Component.Builder();
    if (_defaults._unsetProperties.contains(Component_Builder.Property.NAME)
        || !Objects.equals(value.name(), _defaults.name())) {
      name(value.name());
    }
    return (Component.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public Component.Builder mergeFrom(Component.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    Component_Builder base = template;
    Component_Builder _defaults = new Component.Builder();
    if (!base._unsetProperties.contains(Component_Builder.Property.NAME)
        && (_defaults._unsetProperties.contains(Component_Builder.Property.NAME)
            || !Objects.equals(template.name(), _defaults.name()))) {
      name(template.name());
    }
    return (Component.Builder) this;
  }

  /** Resets the state of this builder. */
  public Component.Builder clear() {
    Component_Builder _defaults = new Component.Builder();
    name = _defaults.name;
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (Component.Builder) this;
  }

  /**
   * Returns a newly-created {@link Component} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public Component build() {
    if (!_unsetProperties.isEmpty()) {
      throw new IllegalStateException("Not set: " + _unsetProperties);
    }
    return new Component_Builder.Value(this);
  }

  /**
   * Returns a newly-created partial {@link Component} for use in unit tests. State checking will
   * not be performed. Unset properties will throw an {@link UnsupportedOperationException} when
   * accessed via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  public Component buildPartial() {
    return new Component_Builder.Partial(this);
  }

  private static final class Value implements Component {
    private final String name;

    private Value(Component_Builder builder) {
      this.name = builder.name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Component.Builder toBuilder() {
      return new Component.Builder().mergeFrom(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Component_Builder.Value)) {
        return false;
      }
      Component_Builder.Value other = (Component_Builder.Value) obj;
      return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public String toString() {
      return "Component{name=" + name + "}";
    }
  }

  private static final class Partial implements Component {
    private final String name;
    private final EnumSet<Component_Builder.Property> _unsetProperties;

    Partial(Component_Builder builder) {
      this.name = builder.name;
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public String name() {
      if (_unsetProperties.contains(Component_Builder.Property.NAME)) {
        throw new UnsupportedOperationException("name not set");
      }
      return name;
    }

    private static class PartialBuilder extends Component.Builder {
      @Override
      public Component build() {
        return buildPartial();
      }
    }

    @Override
    public Component.Builder toBuilder() {
      Component.Builder builder = new PartialBuilder();
      if (!_unsetProperties.contains(Component_Builder.Property.NAME)) {
        builder.name(name);
      }
      return builder;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Component_Builder.Partial)) {
        return false;
      }
      Component_Builder.Partial other = (Component_Builder.Partial) obj;
      return Objects.equals(name, other.name)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, _unsetProperties);
    }

    @Override
    public String toString() {
      return "partial Component{"
          + (!_unsetProperties.contains(Component_Builder.Property.NAME) ? "name=" + name : "")
          + "}";
    }
  }
}
