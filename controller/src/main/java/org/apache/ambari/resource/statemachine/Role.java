package org.apache.ambari.resource.statemachine;

public interface Role extends LifeCycle {
  
  public RoleState getRoleState();
  
  public String getRoleName();
  
  public Service getAssociatedService();
  
}
