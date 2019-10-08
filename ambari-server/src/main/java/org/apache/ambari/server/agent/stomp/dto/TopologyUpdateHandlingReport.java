package org.apache.ambari.server.agent.stomp.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.NullHostNameException;

public class TopologyUpdateHandlingReport {
  private Set<String> updatedHostNames = new HashSet<>();
  private boolean mappingChanged = false;

  public boolean wasChanged(){
    return mappingChanged || !updatedHostNames.isEmpty();
  }

  public Set<String> getUpdatedHostNames() {
    return updatedHostNames;
  }

  public void addHostName(String updatedHostName) throws NullHostNameException {
    if (updatedHostName == null) {
      throw new NullHostNameException("Host name could not be a null");
    }
    this.updatedHostNames.add(updatedHostName);
  }

  public void addHostsNames(Set<String> updatedHostNames) {
    this.updatedHostNames.addAll(updatedHostNames);
  }

  public void mappingWasChanged() {
    this.mappingChanged = true;
  }
}
