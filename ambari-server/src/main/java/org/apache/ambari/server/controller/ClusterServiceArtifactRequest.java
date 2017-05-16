package org.apache.ambari.server.controller;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request schema for endpoint {@link org.apache.ambari.server.api.services.ServiceService#createArtifact(String,
 * javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo, String, String)}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface ClusterServiceArtifactRequest extends ApiModel {

  @ApiModelProperty(name = "Artifacts")
  public ClusterServiceArtifactRequestInfo getClusterServiceArtifactRequestInfo();

  @ApiModelProperty(name = "artifact_data")
  public Map<String, Object> getArtifactData();

  public interface ClusterServiceArtifactRequestInfo {
    @ApiModelProperty(name = "artifact_name")
    public String getArtifactName();
  }

}
