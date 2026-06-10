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
export const mockInstanceDetails = {
  href: 'http://example.com/root',
  ViewInstanceInfo: {
    cluster_handle: 123,
    cluster_type: 'LOCAL_AMBARI',
    context_path: '/context/path',
    description: 'A description of the view instance',
    icon64_path: null,
    icon_path: null,
    instance_name: 'Instance1',
    label: 'Instance Label',
    short_url: 'http://short.url',
    short_url_name: 'short-url',
    static: false,
    validation_result: {
      valid: true,
      detail: 'Validation successful'
    },
    version: '1.0',
    view_name: 'View1',
    visible: true,
    instance_data: {},
    properties: {
      "hdfs.auth_to_local": {
        viewInfo: {
          name: 'hdfs.auth_to_local',
          description: 'Description for hdfs.auth_to_local',
          label: 'HDFS Auth To Local',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "hdfs.umask-mode": {
        viewInfo: {
          name: 'hdfs.umask-mode',
          description: 'Description for hdfs.umask-mode',
          label: 'HDFS Umask Mode',
          placeholder: null,
          defaultValue: '022',
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: '022',
          isSetting: false
        }
      },
      "tmp.dir": {
        viewInfo: {
          name: 'tmp.dir',
          description: 'Description for tmp.dir',
          label: 'Temporary Directory',
          placeholder: '/tmp',
          defaultValue: '/tmp',
          clusterConfig: null,
          required: true,
          masked: false,
          value: '/tmp',
          isSetting: false
        }
      },
      "view.conf.keyvalues": {
        viewInfo: {
          name: 'view.conf.keyvalues',
          description: 'Description for view.conf.keyvalues',
          label: 'View Config Key Values',
          placeholder: null,
          defaultValue: null,
          clusterConfig: null,
          required: false,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.auth": {
        viewInfo: {
          name: 'webhdfs.auth',
          description: 'Description for webhdfs.auth',
          label: 'WebHDFS Auth',
          placeholder: 'auth-placeholder',
          defaultValue: null,
          clusterConfig: null,
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.client.failover.proxy.provider": {
        viewInfo: {
          name: 'webhdfs.client.failover.proxy.provider',
          description: 'Description for webhdfs.client.failover.proxy.provider',
          label: 'WebHDFS Client Failover Proxy Provider',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.ha.namenode.http-address.list": {
        viewInfo: {
          name: 'webhdfs.ha.namenode.http-address.list',
          description: 'Description for webhdfs.ha.namenode.http-address.list',
          label: 'WebHDFS HA Namenode HTTP Address List',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.ha.namenode.https-address.list": {
        viewInfo: {
          name: 'webhdfs.ha.namenode.https-address.list',
          description: 'Description for webhdfs.ha.namenode.https-address.list',
          label: 'WebHDFS HA Namenode HTTPS Address List',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.ha.namenode.rpc-address.list": {
        viewInfo: {
          name: 'webhdfs.ha.namenode.rpc-address.list',
          description: 'Description for webhdfs.ha.namenode.rpc-address.list',
          label: 'WebHDFS HA Namenode RPC Address List',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.ha.namenodes.list": {
        viewInfo: {
          name: 'webhdfs.ha.namenodes.list',
          description: 'Description for webhdfs.ha.namenodes.list',
          label: 'WebHDFS HA Namenodes List',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.nameservices": {
        viewInfo: {
          name: 'webhdfs.nameservices',
          description: 'Description for webhdfs.nameservices',
          label: 'WebHDFS Nameservices',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.url": {
        viewInfo: {
          name: 'webhdfs.url',
          description: 'Description for webhdfs.url',
          label: 'WebHDFS URL',
          placeholder: null,
          defaultValue: null,
          clusterConfig: 'cluster-config',
          required: true,
          masked: false,
          value: null,
          isSetting: false
        }
      },
      "webhdfs.username": {
        viewInfo: {
          name: 'webhdfs.username',
          description: 'Description for webhdfs.username',
          label: 'WebHDFS Username',
          placeholder: 'username-placeholder',
          defaultValue: 'default-username',
          clusterConfig: null,
          required: true,
          masked: false,
          value: 'default-username',
          isSetting: false
        }
      }
    },
    property_validation_results: {
      "hdfs.auth_to_local": {
        valid: true,
        detail: 'Validation successful for hdfs.auth_to_local'
      },
      "hdfs.umask-mode": {
        valid: true,
        detail: 'Validation successful for hdfs.umask-mode'
      },
      "tmp.dir": {
        valid: true,
        detail: 'Validation successful for tmp.dir'
      },
      "view.conf.keyvalues": {
        valid: true,
        detail: 'Validation successful for view.conf.keyvalues'
      },
      "webhdfs.auth": {
        valid: true,
        detail: 'Validation successful for webhdfs.auth'
      },
      "webhdfs.client.failover.proxy.provider": {
        valid: true,
        detail: 'Validation successful for webhdfs.client.failover.proxy.provider'
      },
      "webhdfs.ha.namenode.http-address.list": {
        valid: true,
        detail: 'Validation successful for webhdfs.ha.namenode.http-address.list'
      },
      "webhdfs.ha.namenode.https-address.list": {
        valid: true,
        detail: 'Validation successful for webhdfs.ha.namenode.https-address.list'
      },
      "webhdfs.ha.namenode.rpc-address.list": {
        valid: true,
        detail: 'Validation successful for webhdfs.ha.namenode.rpc-address.list'
      },
      "webhdfs.ha.namenodes.list": {
        valid: true,
        detail: 'Validation successful for webhdfs.ha.namenodes.list'
      },
      "webhdfs.nameservices": {
        valid: true,
        detail: 'Validation successful for webhdfs.nameservices'
      },
      "webhdfs.url": {
        valid: true,
        detail: 'Validation successful for webhdfs.url'
      },
      "webhdfs.username": {
        valid: true,
        detail: 'Validation successful for webhdfs.username'
      }
    }
  },
  privileges: [
    {
      href: 'http://example.com/privilege/1',
      PrivilegeInfo: {
        instance_name: 'Instance1',
        permission_label: 'Read',
        permission_name: 'READ_PRIVILEGE',
        principal_name: 'User1',
        principal_type: 'USER',
        privilege_id: 1,
        version: '1.0',
        view_name: 'View1',
      },
    },
    {
      href: 'http://example.com/privilege/2',
      PrivilegeInfo: {
        instance_name: 'Instance1',
        permission_label: 'Write',
        permission_name: 'WRITE_PRIVILEGE',
        principal_name: 'User2',
        principal_type: 'USER',
        privilege_id: 2,
        version: '1.0',
        view_name: 'View1',
      },
    },
  ],
  resources: [
    {
      href: 'http://example.com/resource/1',
      instance_name: 'Instance1',
      name: 'Resource1',
      version: '1.0',
      view_name: 'View1',
    },
    {
      href: 'http://example.com/resource/2',
      instance_name: 'Instance1',
      name: 'Resource2',
      version: '1.0',
      view_name: 'View1',
    },
  ],
};

export const mockGroupData = {
  href: 'http://example.com',
  items: [
    {
      href: 'http://example.com/group/1',
      Groups: {
        group_name: 'Group One',
        group_type: 'LOCAL',
        ldap_group: false,
      },
    },
    {
      href: 'http://example.com/group/2',
      Groups: {
        group_name: 'Group Two',
        group_type: 'LOCAL',
        ldap_group: true,
      },
    },
  ],
};

export const mockPrivileges = 
{
  href: 'http://example.com/root',
  ViewInstanceInfo: {
    instance_name: 'Instance1',
    version: '1.0',
    view_name: 'View1',
  },
  privileges: [
    {
      href: 'http://example.com/privilege/1',
      PrivilegeInfo: {
        instance_name: 'Instance1',
        permission_label: 'Read',
        permission_name: 'READ_PRIVILEGE',
        principal_name: 'User1',
        principal_type: 'USER',
        privilege_id: 1,
        version: '1.0',
        view_name: 'View1',
      },
    },
    {
      href: 'http://example.com/privilege/2',
      PrivilegeInfo: {
        instance_name: 'Instance1',
        permission_label: 'Write',
        permission_name: 'WRITE_PRIVILEGE',
        principal_name: 'User2',
        principal_type: 'USER',
        privilege_id: 2,
        version: '1.0',
        view_name: 'View1',
      },
    },
    {
      href: 'http://example.com/privilege/2',
      PrivilegeInfo: {
        instance_name: 'Instance1',
        permission_label: 'Write',
        permission_name: 'WRITE_PRIVILEGE',
        principal_name: 'Group1',
        principal_type: 'GROUP',
        privilege_id: 2,
        version: '1.0',
        view_name: 'View1',
      },
    },
  ],
};

export const mockViewsData = {
  href: 'http://example.com',
  ViewVersionInfo: {
    archive: 'archive.zip',
    build_number: '100',
    cluster_configurable: true,
    description: null,
    label: 'Label 1',
    masker_class: null,
    max_ambari_version: null,
    min_ambari_version: 'v1.0',
    parameters: [
      {
        name: 'param1',
        description: 'This is parameter 1',
        label: 'Label 1',
        placeholder: 'Placeholder 1',
        defaultValue: 'Default 1',
        clusterConfig: 'Config 1',
        required: true,
        masked: false,
      },
    ],
    status: 'active',
    status_detail: 'Detail 1',
    system: false,
    version: 'v1.0',
    view_name: 'View 1',
  },
  instances: [
    {
      href: 'http://example.com/instance/1',
      ViewInstanceInfo: {
        instance_name: 'Instance 1',
        version: 'v1.0',
        view_name: 'View 1',
      },
    },
  ],
  permissions: [
    {
      href: 'http://example.com/permission/1',
      PermissionInfo: {
        permission_id: 1,
        version: 'v1.0',
        view_name: 'View 1',
      },
    },
  ],
};

export const mockUsersdata = {
  href: 'http://example.com',
  items: [
    {
      href: 'http://example.com/user/1',
      Users: {
        active: true,
        admin: false,
        consecutive_failures: 0,
        created: 1622470423,
        display_name: 'User One',
        groups: ['group1', 'group2'],
        ldap_user: false,
        local_user_name: 'userone',
        user_name: 'userone',
        user_type: 'LOCAL',
      },
    },
    {
      href: 'http://example.com/user/2',
      Users: {
        active: false,
        admin: true,
        consecutive_failures: 3,
        created: 1622470424,
        display_name: 'User Two',
        groups: ['group3'],
        ldap_user: true,
        local_user_name: 'usertwo',
        user_name: 'usertwo',
        user_type: 'LOCAL',
      },
    },
  ],
};

