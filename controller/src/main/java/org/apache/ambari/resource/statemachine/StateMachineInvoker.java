package org.apache.ambari.resource.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.event.AsyncDispatcher;
import org.apache.ambari.event.Dispatcher;
import org.apache.ambari.event.EventHandler;

public class StateMachineInvoker {
  
  private static Dispatcher dispatcher;
  
  static {
    dispatcher = new AsyncDispatcher();
    dispatcher.register(ClusterEventType.class, new ClusterEventDispatcher());
    dispatcher.register(ServiceEventType.class, new ServiceEventDispatcher());
  }

  public Dispatcher getAMBARIDispatcher() {
    return dispatcher;
  }

  public static EventHandler getAMBARIEventHandler() {
    return dispatcher.getEventHandler();
  }

  public static class ClusterEventDispatcher 
  implements EventHandler<ClusterEvent> {
    @Override
    public void handle(ClusterEvent event) {
      ((EventHandler<ClusterEvent>)event.getCluster()).handle(event);
    }
  }
  
  public static class ServiceEventDispatcher 
  implements EventHandler<ServiceEvent> {
    @Override
    public void handle(ServiceEvent event) {
      ((EventHandler<ServiceEvent>)event.getService()).handle(event);
    }
  }
  
  public static Cluster createClusterImpl(String clusterName) {
    return new ClusterImpl(clusterName);
  }
  
  public static Service addServiceInCluster(Cluster cluster, String serviceName) {
    Service service = new ServiceImpl(cluster, serviceName);
    return addServiceInCluster(cluster, service);
  }
  
  public static Service addServiceInCluster(Cluster cluster, Service service) {
    List<Service> serviceList = new ArrayList<Service>();
    serviceList.add(service);
    cluster.addServices(serviceList);
    return service;
  }
    
}
