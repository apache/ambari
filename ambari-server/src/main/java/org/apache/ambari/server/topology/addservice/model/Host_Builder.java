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

/** Auto-generated superclass of {@link Host.Builder}, derived from the API of {@link Host}. */
abstract class Host_Builder {

  /** Creates a new builder using {@code value} as a template. */
  public static Host.Builder from(Host value) {
    return new Host.Builder().mergeFrom(value);
  }

  private enum Property {
    HOSTNAME("hostname"),
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

  private String hostname;
  private final EnumSet<Host_Builder.Property> _unsetProperties =
      EnumSet.allOf(Host_Builder.Property.class);

  /**
   * Sets the value to be returned by {@link Host#hostname()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code hostname} is null
   */
  public Host.Builder hostname(String hostname) {
    this.hostname = Objects.requireNonNull(hostname);
    _unsetProperties.remove(Host_Builder.Property.HOSTNAME);
    return (Host.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link Host#hostname()} by applying {@code mapper} to it
   * and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public Host.Builder mapHostname(UnaryOperator<String> mapper) {
    Objects.requireNonNull(mapper);
    return hostname(mapper.apply(hostname()));
  }

  /**
   * Returns the value that will be returned by {@link Host#hostname()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String hostname() {
    if (_unsetProperties.contains(Host_Builder.Property.HOSTNAME)) {
      throw new IllegalStateException("hostname not set");
    }
    return hostname;
  }

  /** Sets all property values using the given {@code Host} as a template. */
  public Host.Builder mergeFrom(Host value) {
    Host_Builder _defaults = new Host.Builder();
    if (_defaults._unsetProperties.contains(Host_Builder.Property.HOSTNAME)
        || !Objects.equals(value.hostname(), _defaults.hostname())) {
      hostname(value.hostname());
    }
    return (Host.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public Host.Builder mergeFrom(Host.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    Host_Builder base = template;
    Host_Builder _defaults = new Host.Builder();
    if (!base._unsetProperties.contains(Host_Builder.Property.HOSTNAME)
        && (_defaults._unsetProperties.contains(Host_Builder.Property.HOSTNAME)
            || !Objects.equals(template.hostname(), _defaults.hostname()))) {
      hostname(template.hostname());
    }
    return (Host.Builder) this;
  }

  /** Resets the state of this builder. */
  public Host.Builder clear() {
    Host_Builder _defaults = new Host.Builder();
    hostname = _defaults.hostname;
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (Host.Builder) this;
  }

  /**
   * Returns a newly-created {@link Host} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public Host build() {
    if (!_unsetProperties.isEmpty()) {
      throw new IllegalStateException("Not set: " + _unsetProperties);
    }
    return new Host_Builder.Value(this);
  }

  /**
   * Returns a newly-created partial {@link Host} for use in unit tests. State checking will not be
   * performed. Unset properties will throw an {@link UnsupportedOperationException} when accessed
   * via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  public Host buildPartial() {
    return new Host_Builder.Partial(this);
  }

  private static final class Value implements Host {
    private final String hostname;

    private Value(Host_Builder builder) {
      this.hostname = builder.hostname;
    }

    @Override
    public String hostname() {
      return hostname;
    }

    @Override
    public Host.Builder toBuilder() {
      return new Host.Builder().mergeFrom(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Host_Builder.Value)) {
        return false;
      }
      Host_Builder.Value other = (Host_Builder.Value) obj;
      return Objects.equals(hostname, other.hostname);
    }

    @Override
    public int hashCode() {
      return Objects.hash(hostname);
    }

    @Override
    public String toString() {
      return "Host{hostname=" + hostname + "}";
    }
  }

  private static final class Partial implements Host {
    private final String hostname;
    private final EnumSet<Host_Builder.Property> _unsetProperties;

    Partial(Host_Builder builder) {
      this.hostname = builder.hostname;
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public String hostname() {
      if (_unsetProperties.contains(Host_Builder.Property.HOSTNAME)) {
        throw new UnsupportedOperationException("hostname not set");
      }
      return hostname;
    }

    private static class PartialBuilder extends Host.Builder {
      @Override
      public Host build() {
        return buildPartial();
      }
    }

    @Override
    public Host.Builder toBuilder() {
      Host.Builder builder = new PartialBuilder();
      if (!_unsetProperties.contains(Host_Builder.Property.HOSTNAME)) {
        builder.hostname(hostname);
      }
      return builder;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Host_Builder.Partial)) {
        return false;
      }
      Host_Builder.Partial other = (Host_Builder.Partial) obj;
      return Objects.equals(hostname, other.hostname)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(hostname, _unsetProperties);
    }

    @Override
    public String toString() {
      return "partial Host{"
          + (!_unsetProperties.contains(Host_Builder.Property.HOSTNAME)
              ? "hostname=" + hostname
              : "")
          + "}";
    }
  }
}
