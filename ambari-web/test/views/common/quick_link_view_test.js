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

var App = require('app');
require('views/common/quick_view_link_view');

describe('App.QuickViewLinks', function () {

  var quickViewLinks = App.QuickViewLinks.create({});

  describe('#setProtocol', function() {
    var tests = [
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': 'true' }, m: "https for ganglia", result: "https" },
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': 'false' }, m: "http for ganglia 1", result: "http" },
      { serviceName: "GANGLIA", m: "http for ganglia 2", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }}
      ], m: "https for yarn", result: "https" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }}
      ], m: "http for yarn", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }}
      ], m: "http for yarn (overrides hadoop.ssl.enabled)", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }}
      ], m: "https for yarn (overrides hadoop.ssl.enabled)", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTPS_ONLY' }}
      ], m: "https for mapreduce2", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTP_ONLY' }}
      ], m: "http for mapreduce2", result: "http" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'hdfs-site', properties: { 'dfs.http.policy': 'HTTPS_ONLY' }}
      ], m: "https for anyservice", servicesSupportsHttps: ["ANYSERVICE"], result: "https" },
      { serviceName: "RANGER", configProperties: [
        { type: 'ranger-site', properties: { 'http.enabled': 'true' }}
      ], m: "http for ranger (HDP2.2)", result: "http" },
      { serviceName: "RANGER", configProperties: [
        { type: 'ranger-site', properties: { 'http.enabled': 'false' }}
      ], m: "https for ranger (HDP2.2)", result: "https" },
      { serviceName: "RANGER", configProperties: [
        { type: 'ranger-admin-site', properties: { 'ranger.service.http.enabled': 'true', 'ranger.service.https.attrib.ssl.enabled': 'false'}}
      ], m: "http for ranger (HDP2.3)", result: "http" },
      { serviceName: "RANGER", configProperties: [
        { type: 'ranger-admin-site', properties: { 'ranger.service.http.enabled': 'false', 'ranger.service.https.attrib.ssl.enabled': 'true'}}
      ], m: "https for ranger (HDP2.3)", result: "https" }
    ];

    tests.forEach(function(t) {
      it(t.m, function() {
        quickViewLinks.set('servicesSupportsHttps', t.servicesSupportsHttps);
        expect(quickViewLinks.setProtocol(t.serviceName, t.configProperties, t.ambariProperties)).to.equal(t.result);
      });
    });
  });

  describe('#setPort', function () {
    var testData = [
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'http',
        'result': '8088',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'config': 'https_config_custom',
        'site': 'yarn-site',
        'result': '9091',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)',
        'configProperties': [{
          'type': 'yarn-site',
          'properties': {
            'https_config': 'h:9090',
            'https_config_custom': 'h:9091'
          }
        }]
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'site': 'yarn-site',
        'result': '9090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)',
        'configProperties': [{
          'type': 'yarn-site',
          'properties': {
            'http_config': 'h:9088',
            'https_config': 'h:9090'
          }
        }]
      }),
      Em.Object.create({
        'service_id': 'RANGER',
        'protocol': 'http',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'result': '6080',
        'default_http_port': '6080',
        'default_https_port': '6182',
        'regex': '(\\d*)+'
      }),
      Em.Object.create({
        'service_id': 'RANGER',
        'protocol': 'https',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'result': '6182',
        'default_http_port': '6080',
        'default_https_port': '6182',
        'regex': '(\\d*)+'
      })
    ];

    after(function () {
      quickViewLinks.set('configProperties', []);
    });

    testData.forEach(function(item) {
      it(item.service_id + ' ' + item.protocol, function () {
        quickViewLinks.set('configProperties', item.configProperties || []);
        expect(quickViewLinks.setPort(item, item.protocol, item.config)).to.equal(item.result);
      })
    },this);
  });

  describe('#setHost', function () {

    var quickViewLinks = App.QuickViewLinks.create({
        content: Em.Object.create()
      }),
      cases = [
        {
          singleNodeInstall: true,
          hosts: ['host0'],
          title: 'single node install'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'STORM_UI_SERVER'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host3'
                }
              }
            ]
          },
          serviceName: 'STORM',
          hosts: ['host3']
        },
        {
          serviceName: 'PIG',
          hosts: [],
          title: 'client only service'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'ZOOKEEPER_SERVER'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host4'
                }
              }
            ]
          },
          serviceName: 'ZOOKEEPER',
          hosts: ['host4'],
          setup: function () {
            quickViewLinks.set('content', {
              hostComponents: [
                Em.Object.create({
                  componentName: 'ZOOKEEPER_SERVER',
                  isMaster: true
                })
              ]
            });
          },
          title: 'service with master component, except HDFS, HBase, YARN and Storm'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'NAMENODE'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host5'
                }
              }
            ]
          },
          serviceName: 'HDFS',
          hosts: ['host5'],
          setup: function () {
            quickViewLinks.set('content', {
              snameNode: true
            });
          },
          title: 'HDFS, HA disabled'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'NAMENODE'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host6',
                  public_host_name: 'host6'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'NAMENODE'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host7',
                  public_host_name: 'host7'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'NAMENODE'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host8',
                  public_host_name: 'host8'
                }
              }
            ]
          },
          serviceName: 'HDFS',
          multipleMasters: true,
          hosts: ['host6', 'host7', 'host8'],
          setup: function () {
            quickViewLinks.set('content', {
              hostComponents: [
                Em.Object.create({
                  componentName: 'NAMENODE',
                  hostName: 'host6'
                }),
                Em.Object.create({
                  componentName: 'NAMENODE',
                  hostName: 'host7'
                }),
                Em.Object.create({
                  componentName: 'NAMENODE',
                  hostName: 'host8'
                })
              ],
              activeNameNode: {
                hostName: 'host6'
              },
              standbyNameNode: {
                hostName: 'host7'
              },
              standbyNameNode2: {
                hostName: 'host8'
              }
            });
          },
          title: 'HDFS, HA enabled'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'RESOURCEMANAGER'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host9'
                }
              }
            ]
          },
          serviceName: 'YARN',
          hosts: ['host9'],
          title: 'YARN, HA disabled'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'RESOURCEMANAGER'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host10',
                  public_host_name: 'host10'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'RESOURCEMANAGER'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host11',
                  public_host_name: 'host11'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'RESOURCEMANAGER'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host12',
                  public_host_name: 'host12'
                }
              }
            ]
          },
          serviceName: 'YARN',
          multipleMasters: true,
          hosts: ['host10', 'host11', 'host12'],
          setup: function () {
            quickViewLinks.set('content', {
              hostComponents: [
                Em.Object.create({
                  componentName: 'RESOURCEMANAGER',
                  hostName: 'host10'
                }),
                Em.Object.create({
                  componentName: 'RESOURCEMANAGER',
                  hostName: 'host11'
                }),
                Em.Object.create({
                  componentName: 'RESOURCEMANAGER',
                  hostName: 'host12'
                })
              ]
            });
          },
          title: 'YARN, HA enabled'
        },
        {
          response: {
            items: [
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'HBASE_MASTER'
                    },
                    metrics: {
                      hbase: {
                        master: {
                          IsActiveMaster: true
                        }
                      }
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host13',
                  public_host_name: 'host13'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'HBASE_MASTER'
                    },
                    metrics: {
                      hbase: {
                        master: {
                          IsActiveMaster: false
                        }
                      }
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host14',
                  public_host_name: 'host14'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'HBASE_MASTER'
                    }
                  }
                ],
                Hosts: {
                  host_name: 'host15',
                  public_host_name: 'host15'
                }
              }
            ]
          },
          serviceName: 'HBASE',
          multipleMasters: true,
          hosts: ['host13', 'host14', 'host15']
        }
      ];

    before(function () {
      sinon.stub(App.StackService, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 'ZOOKEEPER',
            hasMaster: true
          }),
          Em.Object.create({
            serviceName: 'PIG',
            hasMaster: false
          })
        ];
      })
    });

    after(function () {
      App.StackService.find.restore();
    });

    afterEach(function () {
      App.get.restore();
    });

    cases.forEach(function (item) {
      it(item.title || item.serviceName, function () {
        if (item.setup) {
          item.setup();
        }
        sinon.stub(App, 'get').withArgs('singleNodeInstall').returns(item.singleNodeInstall).
          withArgs('singleNodeAlias').returns('host0').
          withArgs('isRMHaEnabled').returns(item.multipleMasters);
        if (item.multipleMasters) {
          expect(quickViewLinks.setHost(item.response, item.serviceName).mapProperty('publicHostName')).to.eql(item.hosts);
        } else {
          expect(quickViewLinks.setHost(item.response, item.serviceName)).to.eql(item.hosts);
        }
      });
    });

  });

});
