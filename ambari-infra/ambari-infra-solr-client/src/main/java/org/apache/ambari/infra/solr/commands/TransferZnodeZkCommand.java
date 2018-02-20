package org.apache.ambari.infra.solr.commands;

import org.apache.ambari.infra.solr.AmbariSolrCloudClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;

public class TransferZnodeZkCommand extends AbstractZookeeperRetryCommand<Boolean> {

  public TransferZnodeZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected Boolean executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    boolean isSrcZk = true;
    boolean isDestZk = true;
    if ("copyToLocal".equals(client.getTransferMode())) {
      isDestZk = false;
    } else if ("copyFromLocal".equals(client.getTransferMode())) {
      isSrcZk = false;
    }
    zkClient.zkTransfer(client.getCopySrc(), isSrcZk, client.getCopyDest(), isDestZk, true);
    return true;
  }


}
