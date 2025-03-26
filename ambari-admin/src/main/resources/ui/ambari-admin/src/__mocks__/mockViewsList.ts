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
export const mockViewsList = {
    href: "http://example.com",
    items: [
      {
        href: "http://example.com/item1",
        ViewInfo: {
          view_name: "Item 1 view",
        },
        versions: [
          {
            href: "http://example.com/item1/version1",
            ViewVersionInfo: {
              archive: "archive1",
              build_number: "1",
              cluster_configurable: true,
              description: "Version 1",
              label: "v1",
              masker_class: null,
              max_ambari_version: null,
              min_ambari_version: "1.0",
              parameters: [
                {
                  name: "param1",
                  description: "This is parameter 1",
                  label: "Parameter 1",
                  placeholder: "Enter param1",
                  defaultValue: "default1",
                  clusterConfig: "config1",
                  required: true,
                  masked: false,
                },
              ],
              status: "active",
              status_detail: "Detailed status",
              system: false,
              version: "1",
              view_name: "View 1",
            },
            instances: [
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
            ],
            permissions: [
              {
                href: "http://example.com/item1/version1/permission1",
                PermissionInfo: {
                  permission_id: 1,
                  version: "1",
                  view_name: "View 1",
                },
              },
            ],
          },
        ],
      },
    ],
  };
  
  
  export const paginatedViews = {
    href: "http://example.com",
    items: [
      {
        href: "http://example.com/item1",
        ViewInfo: {
          view_name: "Item 1 view",
        },
        versions: [
          {
            href: "http://example.com/item1/version1",
            ViewVersionInfo: {
              archive: "archive1",
              build_number: "1",
              cluster_configurable: true,
              description: "Version 1",
              label: "v1",
              masker_class: null,
              max_ambari_version: null,
              min_ambari_version: "1.0",
              parameters: [
                {
                  name: "param1",
                  description: "This is parameter 1",
                  label: "Parameter 1",
                  placeholder: "Enter param1",
                  defaultValue: "default1",
                  clusterConfig: "config1",
                  required: true,
                  masked: false,
                },
              ],
              status: "active",
              status_detail: "Detailed status",
              system: false,
              version: "1",
              view_name: "View 1",
            },
            instances: [
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 2",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 2",
                  label: "Instance 2",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance some",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance some",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance some",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance some",
                  label: "Instance some",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
              {
                href: "http://example.com/item1/version1/instance1",
                ViewInstanceInfo: {
                  cluster_handle: null,
                  cluster_type: "type1",
                  context_path: "/path1",
                  description: "Instance 1",
                  icon64_path: null,
                  icon_path: null,
                  instance_name: "Instance 1",
                  label: "Instance 1",
                  static: false,
                  version: "1",
                  view_name: "View 1",
                  visible: true,
                  instance_data: {},
                  properties: {
                    "hdfs.auth_to_local": null,
                    "hdfs.umask-mode": "022",
                    "tmp.dir": "/tmp",
                    "view.conf.keyvalues": "key1=value1,key2=value2",
                    "webhdfs.auth": "auth1",
                    "webhdfs.client.failover.proxy.provider": null,
                    "webhdfs.ha.namenode.http-address.list": null,
                    "webhdfs.ha.namenode.https-address.list": null,
                    "webhdfs.ha.namenode.rpc-address.list": null,
                    "webhdfs.ha.namenodes.list": null,
                    "webhdfs.nameservices": null,
                    "webhdfs.url": null,
                    "webhdfs.username": "user1",
                    "hadoop.http.auth.type": null,
                    "timeline.http.auth.type": null,
                    "yarn.ats.url": null,
                    "yarn.resourcemanager.url": null,
                  },
                  short_url: "http://exmpl.co/short1",
                  short_url_name: "Short 1",
                  validation_result: {
                    valid: true,
                    detail: "Validation detail",
                  },
                  property_validation_results: {
                    "hdfs.auth_to_local": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "hdfs.umask-mode": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "tmp.dir": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "view.conf.keyvalues": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.auth": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.client.failover.proxy.provider": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.http-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.https-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenode.rpc-address.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.ha.namenodes.list": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.nameservices": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.url": {
                      valid: true,
                      detail: "Validation detail",
                    },
                    "webhdfs.username": {
                      valid: true,
                      detail: "Validation detail",
                    },
                  },
                },
              },
            ],
  
  
            permissions: [
              {
                href: "http://example.com/item1/version1/permission1",
                PermissionInfo: {
                  permission_id: 1,
                  version: "1",
                  view_name: "View 1",
                },
              },
            ],
          },
        ],
      },
    ],
  }