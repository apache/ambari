package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.ivory.Cluster;
import org.apache.ambari.server.controller.ivory.Feed;
import org.apache.ambari.server.controller.ivory.Instance;
import org.apache.ambari.server.controller.ivory.IvoryService;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An IvoryService implementation used for testing the DR related resource providers.
 */
public class TestIvoryService implements IvoryService{

  private final Map<String, Feed> feeds = new HashMap<String, Feed>();
  private final Map<String, Cluster> clusters = new HashMap<String, Cluster>();
  private final Map<String, Map<String, Instance>> instanceMap = new HashMap<String, Map<String, Instance>>();

  private final Map<String, String> suspendedFeedStatusMap = new HashMap<String, String>();
  private final Map<String, String> suspendedInstanceStatusMap = new HashMap<String, String>();


  public TestIvoryService(Map<String, Feed> feeds,
                          Map<String, Cluster> clusters,
                          HashMap<String, Map<String, Instance>> instanceMap) {
    if (feeds != null) {
      this.feeds.putAll(feeds);
    }
    if (clusters != null) {
      this.clusters.putAll(clusters);
    }
    if (instanceMap != null) {
      this.instanceMap.putAll(instanceMap);
    }
  }

  @Override
  public void submitFeed(Feed feed) {
    feeds.put(feed.getName(), feed);
  }

  @Override
  public Feed getFeed(String feedName) {
    return feeds.get(feedName);
  }

  @Override
  public List<String> getFeedNames() {
    return new LinkedList<String>(feeds.keySet());
  }

  @Override
  public void updateFeed(Feed feed) {
    feeds.put(feed.getName(), feed);
  }

  @Override
  public void suspendFeed(String feedName) {
    suspendedFeedStatusMap.put(feedName, setFeedStatus(feedName, "SUSPENDED"));
  }

  @Override
  public void resumeFeed(String feedName) {
    String suspendedStatus = suspendedFeedStatusMap.get(feedName);
    if (suspendedStatus != null) {
      setFeedStatus(feedName, suspendedStatus);
      suspendedFeedStatusMap.remove(feedName);
    }
  }

  @Override
  public void scheduleFeed(String feedName) {
    setFeedStatus(feedName, "SCHEDULED");
  }

  @Override
  public void deleteFeed(String feedName) {
    feeds.remove(feedName);
  }

  @Override
  public void submitCluster(Cluster cluster) {
    clusters.put(cluster.getName(), cluster);
  }

  @Override
  public Cluster getCluster(String clusterName) {
    return clusters.get(clusterName);
  }

  @Override
  public List<String> getClusterNames() {
    return new LinkedList<String>(clusters.keySet());
  }

  @Override
  public void updateCluster(Cluster cluster) {
    clusters.put(cluster.getName(), cluster);
  }

  @Override
  public void deleteCluster(String clusterName) {
    clusters.remove(clusterName);
  }

  @Override
  public List<Instance> getInstances(String feedName) {
    return new LinkedList<Instance>(instanceMap.get(feedName).values());
  }

  @Override
  public void suspendInstance(String feedName, String instanceId) {
    String instanceKey = feedName + "/" + instanceId;

    suspendedFeedStatusMap.put(instanceKey, setInstanceStatus(feedName, instanceId, "SUSPENDED"));
  }

  @Override
  public void resumeInstance(String feedName, String instanceId) {
    String instanceKey = feedName + "/" + instanceId;

    String suspendedStatus = suspendedInstanceStatusMap.get(instanceKey);
    if (suspendedStatus != null) {
      setInstanceStatus(feedName, instanceId, suspendedStatus);
      suspendedInstanceStatusMap.remove(instanceKey);
    }
  }

  @Override
  public void killInstance(String feedName, String instanceId) {
    Map<String, Instance> instances = instanceMap.get(feedName);
    if (instances != null) {
      instances.remove(instanceId);
    }
  }


  // ----- helper methods ----------------------------------------------------

  private String setFeedStatus(String feedName, String status) {
    String currentStatus = null;
    Feed feed = feeds.get(feedName);

    if (feed != null) {
      currentStatus = feed.getStatus();
      if (!currentStatus.equals(status)) {
        feed = new Feed(feed.getName(),
            feed.getDescription(),
            status,
            feed.getSchedule(),
            feed.getSourceClusterName(),
            feed.getTargetClusterName());
        feeds.put(feed.getName(), feed);
      }
    }
    return currentStatus;
  }

  private String setInstanceStatus(String feedName, String instanceId, String status) {
    String currentStatus = null;
    Map<String, Instance> instances = instanceMap.get(feedName);

    if (instances != null) {
      Instance instance = instances.get(instanceId);
      if (instance != null) {
        currentStatus = instance.getStatus();
        if (!currentStatus.equals(status)) {
          instance = new Instance(instance.getFeedName(),
                                  instance.getId(),
                                  status,
                                  instance.getStartTime(),
                                  instance.getEndTime(),
                                  instance.getDetails(),
                                  instance.getLog());
          instances.put(instance.getId(), instance);
        }
      }
    }
    return currentStatus;
  }
}
