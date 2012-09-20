package org.apache.ambari.server.security;

public enum ClientSecurityType {
  LOCAL("local"),
  LDAP("ldap");

  private String value;
  ClientSecurityType(String value) {
    this.value = value;
  }

  public static ClientSecurityType fromString(String value) {
    for (ClientSecurityType securityType : ClientSecurityType.values()) {
      if (securityType.toString().equals(value)) {
        return securityType;
      }
    }
    return null;
  }


  @Override
  public String toString() {
    return value;
  }
}
