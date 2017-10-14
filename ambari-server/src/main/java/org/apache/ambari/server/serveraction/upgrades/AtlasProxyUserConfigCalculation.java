package org.apache.ambari.server.serveraction.upgrades;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class AtlasProxyUserConfigCalculation extends AbstractUpgradeServerAction {

  private static final String ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE = "application-properties";
  private static final String KNOX_ENV_CONFIG_TYPE = "knox-env";
  private static final String KNOX_USER_CONFIG = "knox_user";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = m_clusters.getCluster(clusterName);
    String outputMessage = "";

    Config atlasApplicationProperties = cluster.getDesiredConfigByType(ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE);
    if (null == atlasApplicationProperties) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("Config type {0} not found, skipping updating property in same.", ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE), "");
    }

    Config knoxEnvConfig = cluster.getDesiredConfigByType(KNOX_ENV_CONFIG_TYPE);
    String atlasProxyUsers = "knox";
    if (null != knoxEnvConfig && knoxEnvConfig.getProperties().containsKey(KNOX_USER_CONFIG)) {
      atlasProxyUsers = knoxEnvConfig.getProperties().get(KNOX_USER_CONFIG);
    }

    Map<String, String> currentAtlasApplicationProperties = atlasApplicationProperties.getProperties();
    currentAtlasApplicationProperties.put("atlas.proxyusers", atlasProxyUsers);
    atlasApplicationProperties.setProperties(currentAtlasApplicationProperties);
    atlasApplicationProperties.save();

    outputMessage = outputMessage + MessageFormat.format("Successfully updated {0} config type.\n", ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE);
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputMessage, "");
  }
}
