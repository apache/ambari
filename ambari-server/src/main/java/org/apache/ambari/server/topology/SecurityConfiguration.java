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

import static org.apache.ambari.server.topology.SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_PROPERTY_ID;
import static org.apache.ambari.server.topology.SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID;
import static org.apache.ambari.server.topology.SecurityConfigurationFactory.TYPE_PROPERTY_ID;

import java.util.Objects;

import org.apache.ambari.server.state.SecurityType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Holds security related properties, the securityType and security descriptor (in case of KERBEROS
 * kerberos_descriptor) either contains the whole descriptor or just the reference to it.
 *
 */
@ApiModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SecurityConfiguration {

  public static final SecurityConfiguration NONE = new SecurityConfiguration(SecurityType.NONE);

  /**
   * Security Type
   */
  private SecurityType type;

  /**
   * Holds a reference to a kerberos_descriptor resource.
   */
  private String descriptorReference;

  /**
   * Content of a kerberos_descriptor as String.
   */
  private String descriptor;

  public SecurityConfiguration(SecurityType type) {
    this.type = type;
  }

  @JsonCreator
  public SecurityConfiguration(
    @JsonProperty(TYPE_PROPERTY_ID) SecurityType type,
    @JsonProperty(KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID) String descriptorReference,
    @JsonProperty(KERBEROS_DESCRIPTOR_PROPERTY_ID) String descriptor
  ) {
    this.type = type;
    this.descriptorReference = descriptorReference;
    this.descriptor = descriptor;
  }

  @JsonProperty(TYPE_PROPERTY_ID)
  @ApiModelProperty(name = TYPE_PROPERTY_ID)
  public SecurityType getType() {
    return type;
  }

  @JsonProperty(KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID)
  @ApiModelProperty(name = KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID)
  public String getDescriptor() {
    return descriptor;
  }

  @JsonProperty(KERBEROS_DESCRIPTOR_PROPERTY_ID)
  @ApiModelProperty(name = KERBEROS_DESCRIPTOR_PROPERTY_ID)
  public String getDescriptorReference() {
    return descriptorReference;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    SecurityConfiguration other = (SecurityConfiguration) obj;

    return Objects.equals(type, other.type) &&
      Objects.equals(descriptor, other.descriptor) &&
      Objects.equals(descriptorReference, other.descriptorReference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, descriptor, descriptorReference);
  }

}
