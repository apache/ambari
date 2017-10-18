package org.apache.ambari.server.topology;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceId {
  private String serviceGroup;
  private String name;

  public static ServiceId of(String name, String serviceGroup) {
    ServiceId id = new ServiceId();
    id.name = name;
    id.serviceGroup = serviceGroup;
    return id;
  }

  public String getServiceGroup() {
    return serviceGroup;
  }

  @JsonProperty("service_group")
  public void setServiceGroup(String serviceGroup) {
    this.serviceGroup = serviceGroup;
  }

  public String getName() {
    return name;
  }

  @JsonProperty("service_name")
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceId serviceId = (ServiceId) o;

    if (serviceGroup != null ? !serviceGroup.equals(serviceId.serviceGroup) : serviceId.serviceGroup != null)
      return false;
    return name != null ? name.equals(serviceId.name) : serviceId.name == null;
  }

  @Override
  public int hashCode() {
    int result = serviceGroup != null ? serviceGroup.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
