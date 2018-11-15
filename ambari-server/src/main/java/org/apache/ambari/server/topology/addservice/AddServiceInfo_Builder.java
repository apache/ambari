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
package org.apache.ambari.server.topology.addservice;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.apache.ambari.server.topology.addservice.model.Component;
import org.apache.ambari.server.topology.addservice.model.Host;
import org.apache.ambari.server.topology.addservice.model.Service;

/**
 * Auto-generated superclass of {@link AddServiceInfo.Builder}, derived from the API of {@link
 * AddServiceInfo}.
 */
abstract class AddServiceInfo_Builder {

  /** Creates a new builder using {@code value} as a template. */
  public static AddServiceInfo.Builder from(AddServiceInfo value) {
    return new AddServiceInfo.Builder().mergeFrom(value);
  }

  private enum Property {
    REQUEST_ID("requestId"),
    CLUSTER_NAME("clusterName"),
    REPOSITORY_VERSION_ID("repositoryVersionId"),
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

  private long requestId;
  private String clusterName;
  private long repositoryVersionId;
  private final LinkedHashMap<Service, Map<Component, Set<Host>>> newServices =
      new LinkedHashMap<>();
  private final EnumSet<AddServiceInfo_Builder.Property> _unsetProperties =
      EnumSet.allOf(AddServiceInfo_Builder.Property.class);

  /**
   * Sets the value to be returned by {@link AddServiceInfo#requestId()}.
   *
   * @return this {@code Builder} object
   */
  public AddServiceInfo.Builder requestId(long requestId) {
    this.requestId = requestId;
    _unsetProperties.remove(AddServiceInfo_Builder.Property.REQUEST_ID);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link AddServiceInfo#requestId()} by applying {@code
   * mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public AddServiceInfo.Builder mapRequestId(UnaryOperator<Long> mapper) {
    Objects.requireNonNull(mapper);
    return requestId(mapper.apply(requestId()));
  }

  /**
   * Returns the value that will be returned by {@link AddServiceInfo#requestId()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public long requestId() {
    if (_unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)) {
      throw new IllegalStateException("requestId not set");
    }
    return requestId;
  }

  /**
   * Sets the value to be returned by {@link AddServiceInfo#clusterName()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code clusterName} is null
   */
  public AddServiceInfo.Builder clusterName(String clusterName) {
    this.clusterName = Objects.requireNonNull(clusterName);
    _unsetProperties.remove(AddServiceInfo_Builder.Property.CLUSTER_NAME);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link AddServiceInfo#clusterName()} by applying {@code
   * mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public AddServiceInfo.Builder mapClusterName(UnaryOperator<String> mapper) {
    Objects.requireNonNull(mapper);
    return clusterName(mapper.apply(clusterName()));
  }

  /**
   * Returns the value that will be returned by {@link AddServiceInfo#clusterName()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public String clusterName() {
    if (_unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)) {
      throw new IllegalStateException("clusterName not set");
    }
    return clusterName;
  }

  /**
   * Sets the value to be returned by {@link AddServiceInfo#repositoryVersionId()}.
   *
   * @return this {@code Builder} object
   */
  public AddServiceInfo.Builder repositoryVersionId(long repositoryVersionId) {
    this.repositoryVersionId = repositoryVersionId;
    _unsetProperties.remove(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Replaces the value to be returned by {@link AddServiceInfo#repositoryVersionId()} by applying
   * {@code mapper} to it and using the result.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mapper} is null or returns null
   * @throws IllegalStateException if the field has not been set
   */
  public AddServiceInfo.Builder mapRepositoryVersionId(UnaryOperator<Long> mapper) {
    Objects.requireNonNull(mapper);
    return repositoryVersionId(mapper.apply(repositoryVersionId()));
  }

  /**
   * Returns the value that will be returned by {@link AddServiceInfo#repositoryVersionId()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public long repositoryVersionId() {
    if (_unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)) {
      throw new IllegalStateException("repositoryVersionId not set");
    }
    return repositoryVersionId;
  }

  /**
   * Associates {@code key} with {@code value} in the map to be returned from {@link
   * AddServiceInfo#newServices()}. If the map previously contained a mapping for the key, the old
   * value is replaced by the specified value.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if either {@code key} or {@code value} are null
   */
  public AddServiceInfo.Builder putNewServices(Service key, Map<Component, Set<Host>> value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    newServices.put(key, value);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Copies all of the mappings from {@code map} to the map to be returned from {@link
   * AddServiceInfo#newServices()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code map} is null or contains a null key or value
   */
  public AddServiceInfo.Builder putAllNewServices(
      Map<? extends Service, ? extends Map<Component, Set<Host>>> map) {
    for (Map.Entry<? extends Service, ? extends Map<Component, Set<Host>>> entry : map.entrySet()) {
      putNewServices(entry.getKey(), entry.getValue());
    }
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Removes the mapping for {@code key} from the map to be returned from {@link
   * AddServiceInfo#newServices()}, if one is present.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code key} is null
   */
  public AddServiceInfo.Builder removeNewServices(Service key) {
    Objects.requireNonNull(key);
    newServices.remove(key);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Invokes {@code mutator} with the map to be returned from {@link AddServiceInfo#newServices()}.
   *
   * <p>This method mutates the map in-place. {@code mutator} is a void consumer, so any value
   * returned from a lambda will be ignored. Take care not to call pure functions, like {@link
   * Collection#stream()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code mutator} is null
   */
  public AddServiceInfo.Builder mutateNewServices(
      Consumer<? super Map<Service, Map<Component, Set<Host>>>> mutator) {
    // If putNewServices is overridden, this method will be updated to delegate to it
    mutator.accept(newServices);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Removes all of the mappings from the map to be returned from {@link
   * AddServiceInfo#newServices()}.
   *
   * @return this {@code Builder} object
   */
  public AddServiceInfo.Builder clearNewServices() {
    newServices.clear();
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Returns an unmodifiable view of the map that will be returned by {@link
   * AddServiceInfo#newServices()}. Changes to this builder will be reflected in the view.
   */
  public Map<Service, Map<Component, Set<Host>>> newServices() {
    return Collections.unmodifiableMap(newServices);
  }

  /** Sets all property values using the given {@code AddServiceInfo} as a template. */
  public AddServiceInfo.Builder mergeFrom(AddServiceInfo value) {
    AddServiceInfo_Builder _defaults = new AddServiceInfo.Builder();
    if (_defaults._unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)
        || !Objects.equals(value.requestId(), _defaults.requestId())) {
      requestId(value.requestId());
    }
    if (_defaults._unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)
        || !Objects.equals(value.clusterName(), _defaults.clusterName())) {
      clusterName(value.clusterName());
    }
    if (_defaults._unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)
        || !Objects.equals(value.repositoryVersionId(), _defaults.repositoryVersionId())) {
      repositoryVersionId(value.repositoryVersionId());
    }
    putAllNewServices(value.newServices());
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public AddServiceInfo.Builder mergeFrom(AddServiceInfo.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    AddServiceInfo_Builder base = template;
    AddServiceInfo_Builder _defaults = new AddServiceInfo.Builder();
    if (!base._unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)
        && (_defaults._unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)
            || !Objects.equals(template.requestId(), _defaults.requestId()))) {
      requestId(template.requestId());
    }
    if (!base._unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)
        && (_defaults._unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)
            || !Objects.equals(template.clusterName(), _defaults.clusterName()))) {
      clusterName(template.clusterName());
    }
    if (!base._unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)
        && (_defaults._unsetProperties.contains(
                AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)
            || !Objects.equals(template.repositoryVersionId(), _defaults.repositoryVersionId()))) {
      repositoryVersionId(template.repositoryVersionId());
    }
    putAllNewServices(base.newServices);
    return (AddServiceInfo.Builder) this;
  }

  /** Resets the state of this builder. */
  public AddServiceInfo.Builder clear() {
    AddServiceInfo_Builder _defaults = new AddServiceInfo.Builder();
    requestId = _defaults.requestId;
    clusterName = _defaults.clusterName;
    repositoryVersionId = _defaults.repositoryVersionId;
    newServices.clear();
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (AddServiceInfo.Builder) this;
  }

  /**
   * Returns a newly-created {@link AddServiceInfo} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public AddServiceInfo build() {
    if (!_unsetProperties.isEmpty()) {
      throw new IllegalStateException("Not set: " + _unsetProperties);
    }
    return new AddServiceInfo_Builder.Value(this);
  }

  /**
   * Returns a newly-created partial {@link AddServiceInfo} for use in unit tests. State checking
   * will not be performed. Unset properties will throw an {@link UnsupportedOperationException}
   * when accessed via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  public AddServiceInfo buildPartial() {
    return new AddServiceInfo_Builder.Partial(this);
  }

  private static final class Value extends AddServiceInfo {
    private final long requestId;
    private final String clusterName;
    private final long repositoryVersionId;
    private final Map<Service, Map<Component, Set<Host>>> newServices;

    private Value(AddServiceInfo_Builder builder) {
      this.requestId = builder.requestId;
      this.clusterName = builder.clusterName;
      this.repositoryVersionId = builder.repositoryVersionId;
      this.newServices = immutableMap(builder.newServices);
    }

    @Override
    public long requestId() {
      return requestId;
    }

    @Override
    public String clusterName() {
      return clusterName;
    }

    @Override
    public long repositoryVersionId() {
      return repositoryVersionId;
    }

    @Override
    public Map<Service, Map<Component, Set<Host>>> newServices() {
      return newServices;
    }

    @Override
    public AddServiceInfo.Builder toBuilder() {
      return new AddServiceInfo.Builder().mergeFrom(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof AddServiceInfo_Builder.Value)) {
        return false;
      }
      AddServiceInfo_Builder.Value other = (AddServiceInfo_Builder.Value) obj;
      return Objects.equals(requestId, other.requestId)
          && Objects.equals(clusterName, other.clusterName)
          && Objects.equals(repositoryVersionId, other.repositoryVersionId)
          && Objects.equals(newServices, other.newServices);
    }

    @Override
    public int hashCode() {
      return Objects.hash(requestId, clusterName, repositoryVersionId, newServices);
    }
  }

  private static final class Partial extends AddServiceInfo {
    private final long requestId;
    private final String clusterName;
    private final long repositoryVersionId;
    private final Map<Service, Map<Component, Set<Host>>> newServices;
    private final EnumSet<AddServiceInfo_Builder.Property> _unsetProperties;

    Partial(AddServiceInfo_Builder builder) {
      this.requestId = builder.requestId;
      this.clusterName = builder.clusterName;
      this.repositoryVersionId = builder.repositoryVersionId;
      this.newServices = immutableMap(builder.newServices);
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public long requestId() {
      if (_unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)) {
        throw new UnsupportedOperationException("requestId not set");
      }
      return requestId;
    }

    @Override
    public String clusterName() {
      if (_unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)) {
        throw new UnsupportedOperationException("clusterName not set");
      }
      return clusterName;
    }

    @Override
    public long repositoryVersionId() {
      if (_unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)) {
        throw new UnsupportedOperationException("repositoryVersionId not set");
      }
      return repositoryVersionId;
    }

    @Override
    public Map<Service, Map<Component, Set<Host>>> newServices() {
      return newServices;
    }

    private static class PartialBuilder extends AddServiceInfo.Builder {
      @Override
      public AddServiceInfo build() {
        return buildPartial();
      }
    }

    @Override
    public AddServiceInfo.Builder toBuilder() {
      AddServiceInfo.Builder builder = new PartialBuilder();
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)) {
        builder.requestId(requestId);
      }
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)) {
        builder.clusterName(clusterName);
      }
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)) {
        builder.repositoryVersionId(repositoryVersionId);
      }
      builder.putAllNewServices(newServices);
      return builder;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof AddServiceInfo_Builder.Partial)) {
        return false;
      }
      AddServiceInfo_Builder.Partial other = (AddServiceInfo_Builder.Partial) obj;
      return Objects.equals(requestId, other.requestId)
          && Objects.equals(clusterName, other.clusterName)
          && Objects.equals(repositoryVersionId, other.repositoryVersionId)
          && Objects.equals(newServices, other.newServices)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          requestId, clusterName, repositoryVersionId, newServices, _unsetProperties);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder("partial AddServiceInfo{");
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.REQUEST_ID)) {
        result.append("requestId=").append(requestId);
        result.append(", ");
      }
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.CLUSTER_NAME)) {
        result.append("clusterName=").append(clusterName);
        result.append(", ");
      }
      if (!_unsetProperties.contains(AddServiceInfo_Builder.Property.REPOSITORY_VERSION_ID)) {
        result.append("repositoryVersionId=").append(repositoryVersionId);
        result.append(", ");
      }
      result.append("newServices=").append(newServices);
      result.append("}");
      return result.toString();
    }
  }

  private static <K, V> Map<K, V> immutableMap(Map<K, V> entries) {
    switch (entries.size()) {
      case 0:
        return Collections.emptyMap();
      case 1:
        Map.Entry<K, V> entry = entries.entrySet().iterator().next();
        return Collections.singletonMap(entry.getKey(), entry.getValue());
      default:
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }
  }
}
