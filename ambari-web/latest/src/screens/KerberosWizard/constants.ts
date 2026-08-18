/**
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

export const preconditionOptions = {
  "Existing MIT KDC": {
    Options: {
      "Ambari Server and cluster hosts have network access to both the KDC and KDC admin hosts":
        false,
      "KDC administrative credentials are on-hand.": false,
      "The Java Cryptography Extensions (JCE) have been setup on the Ambari Server host and all hosts in the cluster.":
        false,
    },
  },
  "Existing Active Directory": {
    Options: {
      "Ambari Server and cluster hosts have network access to the Domain Controllers.":
        false,
      "Active Directory secure LDAP (LDAPS) connectivity has been configured.":
        false,
      "Active Directory User container for principals has been created and is on-hand (e.g. OU=Hadoop,OU=People,dc=apache,dc=org)":
        false,
      "Active Directory administrative credentials with delegated control of “Create, delete, and manage user accounts” on the previously mentioned User container are on-hand.":
        false,
      "The Java Cryptography Extensions (JCE) have been setup on the Ambari Server host and all hosts in the cluster.":
        false,
    },
  },
  "Existing IPA": {
    Options: {
      "All cluster hosts are joined to the IPA domain and hosts are registered in DNS":
        false,
      "If you do not plan on using Ambari to manage the krb5.conf, ensure the following is set in each krb5.conf file in your cluster: default_ccache_name = /tmp/krb5cc_%{uid}":
        false,
      "The Java Cryptography Extensions (JCE) have been setup on the Ambari Server host and all hosts in the cluster.":
        false,
    },
  },
  "Manage Kerberos principals and keytabs manually": {
    Options: {
      "Cluster hosts have network access to the KDC": false,
      "Kerberos client utilities (such as kinit) have been installed on every cluster host":
        false,
      "The Java Cryptography Extensions (JCE) have been setup on the Ambari Server host and all hosts in the cluster":
        false,
      "The Service and Ambari Principals will be manually created in the KDC before completing this wizard":
        false,
      "The keytabs for the Service and Ambari Principals will be manually created and distributed to cluster hosts before completing this wizard":
        false,
    },
  },
};

const ONEFS_PRECONDITION =
  "The Isilon administrator has setup all appropriate principals in OneFS";

export function createKerberosPreconditionOptions(
  installedServiceNames: string[],
) {
  const result: Record<string, { Options: Record<string, boolean> }> =
    Object.fromEntries(
    Object.entries(preconditionOptions).map(([plan, value]) => [
      plan,
      { Options: { ...value.Options } },
    ]),
  );
  if (installedServiceNames.includes("ONEFS")) {
    result["Existing MIT KDC"].Options[ONEFS_PRECONDITION] = false;
  }
  return result;
}

export const kdcProperties = {
  "Existing MIT KDC": ['kdc_type:KDC', 'kdc_hosts:KDC', 'realm:KDC', 'executable_search_paths:Advanced kerberos-env'],
  "Existing Active Directory": ['kdc_type:KDC', 'kdc_hosts:KDC', 'realm:KDC', 'ldap_url:KDC', 'container_dn:KDC', 'executable_search_paths:Advanced kerberos-env'],
  "Existing IPA": ['kdc_type:KDC', 'kdc_hosts:KDC', 'realm:KDC', 'executable_search_paths:Advanced kerberos-env'],
  "Manage Kerberos principals and keytabs manually": ['kdc_type:KDC', 'realm:KDC', 'executable_search_paths:Advanced kerberos-env']
};

export const preconditionOptionsValueMapper:any = {
  "Existing MIT KDC":"mit-kdc",
  "Existing Active Directory": "active-directory",
  "Existing IPA": "ipa",
  "Manage Kerberos principals and keytabs manually": "none"

}
