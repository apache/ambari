/*
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

package org.apache.ambari.server.orm.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Table(name = "configuration_base")
@TableGenerator(
  name = "configuration_id_generator",
  table = "ambari_sequences",
  pkColumnName = "sequence_name",
  valueColumnName = "sequence_value",
  pkColumnValue = "configuration_id_seq",
  initialValue = 1
)
@Entity
public class ConfigurationBaseEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "configuration_id_generator")
  private Long id;

  @Column(name = "version")
  private Integer version;

  @Column(name = "version_tag")
  private String versionTag;

  @Column(name = "type")
  private String type;

  @Column(name = "data")
  private String configurationData;

  @Column(name = "attributes")
  private String configurationAttributes;

  @Column(name = "create_timestamp")
  private Long createTimestamp;

  public Long getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getConfigurationData() {
    return configurationData;
  }

  public void setConfigurationData(String configurationData) {
    this.configurationData = configurationData;
  }

  public String getConfigurationAttributes() {
    return configurationAttributes;
  }

  public void setConfigurationAttributes(String configurationAttributes) {
    this.configurationAttributes = configurationAttributes;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  @Override
  public String toString() {
    return "ConfigurationBaseEntity{" +
      "id=" + id +
      ", version=" + version +
      ", versionTag='" + versionTag + '\'' +
      ", type='" + type + '\'' +
      ", configurationData='" + configurationData + '\'' +
      ", configurationAttributes='" + configurationAttributes + '\'' +
      ", createTimestamp=" + createTimestamp +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationBaseEntity that = (ConfigurationBaseEntity) o;

    return new EqualsBuilder()
      .append(id, that.id)
      .append(version, that.version)
      .append(versionTag, that.versionTag)
      .append(type, that.type)
      .append(configurationData, that.configurationData)
      .append(configurationAttributes, that.configurationAttributes)
      .append(createTimestamp, that.createTimestamp)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(id)
      .append(version)
      .append(versionTag)
      .append(type)
      .append(configurationData)
      .append(configurationAttributes)
      .append(createTimestamp)
      .toHashCode();
  }
}
