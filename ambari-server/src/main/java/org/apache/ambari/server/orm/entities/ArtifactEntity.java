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

package org.apache.ambari.server.orm.entities;

import java.util.Collections;
import java.util.Map;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import com.google.gson.Gson;
/**
 * Entity representing an Artifact.
 */
@IdClass(ArtifactEntityPK.class)
@Table(name = "artifact")
@NamedQueries({
    @NamedQuery(
      name = "artifactByNameAndForeignKeys",
      query = "SELECT artifact FROM ArtifactEntity artifact WHERE artifact.artifactName=:artifactName AND artifact.foreignKeys=:foreignKeys",
      hints = {
        @QueryHint(name = QueryHints.QUERY_RESULTS_CACHE, value = HintValues.TRUE),
        @QueryHint(name = QueryHints.QUERY_RESULTS_CACHE_SIZE, value = "100")
      }
    ),
    @NamedQuery(
      name = "artifactByName",
      query = "SELECT artifact FROM ArtifactEntity artifact WHERE artifact.artifactName=:artifactName"
    ),
    @NamedQuery(
      name = "artifactByForeignKeys",
      query = "SELECT artifact FROM ArtifactEntity artifact WHERE artifact.foreignKeys=:foreignKeys"
    )
})

@Entity
public class ArtifactEntity {
  @Id
  @Column(name = "artifact_name", nullable = false, insertable = true, updatable = false, unique = true)
  private String artifactName;

  @Id
  @Column(name = "foreign_keys", nullable = false, insertable = true, updatable = false)
  @Basic
  private String foreignKeys;

  @Column(name = "artifact_data", nullable = false, insertable = true, updatable = true)
  @Basic
  private String artifactData;

  @Transient
  private static final Gson jsonSerializer = new Gson();


  /**
   * Get the artifact name.
   *
   * @return artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Set the artifact name.
   *
   * @param artifactName  the artifact name
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Set the artifact data by specifying a map that is then
   * converted to a json string.
   *
   * @param artifactData  artifact data map
   */
  public void setArtifactData(Map<String, Object> artifactData) {
    this.artifactData = jsonSerializer.toJson(artifactData);
  }

  /**
   * Get the artifact data as a map
   *
   * @return artifact data as a map
   */
  public Map<String, Object> getArtifactData() {
    return jsonSerializer.<Map<String, Object>>fromJson(
        artifactData, Map.class);
  }

  /**
   * Set the foreign keys.
   *
   * @param foreignKeys  ordered map of foreign key property names to values
   */
  public void setForeignKeys(Map<String, String> foreignKeys) {
    this.foreignKeys = serializeForeignKeys(foreignKeys);
  }

  /**
   * Get the foreign keys.
   *
   * @return foreign key map of property name to value
   */
  public Map<String, String> getForeignKeys() {
    return foreignKeys == null ?
        Collections.emptyMap() :
        jsonSerializer.<Map<String, String>>fromJson(foreignKeys, Map.class);
  }

  /**
   * Serialize a map of foreign keys to a string.
   *
   * @param foreignKeys  map of foreign keys to values
   *
   * @return string representation of the foreign keys map
   */
  public static String serializeForeignKeys(Map<String, String> foreignKeys) {
    return jsonSerializer.toJson(foreignKeys);
  }
}
