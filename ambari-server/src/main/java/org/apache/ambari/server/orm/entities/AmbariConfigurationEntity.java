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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ambari_configuration")
@NamedQueries({
  @NamedQuery(
    name = "AmbariConfigurationEntity.findAll",
    query = "select ace from AmbariConfigurationEntity ace")
})

public class AmbariConfigurationEntity {

  @Id
  @Column(name = "id")
  private Long id;

  @OneToOne(cascade = CascadeType.ALL)
  @MapsId
  @JoinColumn(name = "id")
  private ConfigurationBaseEntity configurationBaseEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ConfigurationBaseEntity getConfigurationBaseEntity() {
    return configurationBaseEntity;
  }

  public void setConfigurationBaseEntity(ConfigurationBaseEntity configurationBaseEntity) {
    this.configurationBaseEntity = configurationBaseEntity;
  }

  @Override
  public String toString() {
    return "AmbariConfigurationEntity{" +
      "id=" + id +
      ", configurationBaseEntity=" + configurationBaseEntity +
      '}';
  }
}
