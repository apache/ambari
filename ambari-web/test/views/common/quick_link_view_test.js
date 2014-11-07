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
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': true }, m: "https for ganglia", result: "https" },
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': false }, m: "http for ganglia 1", result: "http" },
      { serviceName: "GANGLIA", m: "http for ganglia 2", result: "http" },
      { serviceName: "NAGIOS", ambariProperties: { 'nagios.https': true }, m: "https for nagios", result: "https" },
      { serviceName: "NAGIOS", ambariProperties: { 'nagios.https': false }, m: "http for nagios", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "https for yarn", result: "https" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "http for yarn", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "http for yarn (overrides hadoop.ssl.enabled)", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': false }}
      ], m: "https for yarn (overrides hadoop.ssl.enabled)", result: "https" },
      { serviceName: "YARN", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for yarn by hadoop.ssl.enabled", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "https for mapreduce2", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "http for mapreduce2", result: "http" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for mapreduce2 by hadoop.ssl.enabled", result: "https" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "http for anyservice hadoop.ssl.enabled is true but doesn't support security", servicesSupportsHttps: [], result: "http" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': false }}
      ], m: "http for anyservice hadoop.ssl.enabled is false", servicesSupportsHttps: ["ANYSERVICE"], result: "http" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for anyservice", servicesSupportsHttps: ["ANYSERVICE"], result: "https" }
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
      })
    ];

    testData.forEach(function(item) {
      it(item.service_id + ' ' + item.protocol, function () {
        expect(quickViewLinks.setPort(item, item.protocol, item.version)).to.equal(item.result);
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
                      component_name: 'JOBTRACKER'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host1'
                }
              },
              {
                host_components: [
                  {
                    HostRoles: {
                      component_name: 'HISTORYSERVER'
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host2'
                }
              }
            ]
          },
          serviceName: 'MAPREDUCE',
          hosts: ['host1', 'host2']
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
          title: 'service with master component, except HDFS, HBase, MapReduce, YARN and Storm'
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
                    }
                  }
                ],
                Hosts: {
                  public_host_name: 'host13'
                }
              }
            ]
          },
          serviceName: 'HBASE',
          hosts: ['host13'],
          title: 'HBASE, single master component'
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
                  host_name: 'host14',
                  public_host_name: 'host14'
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
                  host_name: 'host15',
                  public_host_name: 'host15'
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
                  host_name: 'host16',
                  public_host_name: 'host16'
                }
              }
            ]
          },
          serviceName: 'HBASE',
          hosts: ['host14', 'host15', 'host16'],
          multipleMasters: true,
          title: 'HBASE, multiple master components'
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
          withArgs('isRMHaEnabled').returns(item.multipleMasters).
          withArgs('supports.multipleHBaseMasters').returns(item.multipleMasters);
        if (item.multipleMasters) {
          expect(quickViewLinks.setHost(item.response, item.serviceName).mapProperty('publicHostName')).to.eql(item.hosts);
        } else {
          expect(quickViewLinks.setHost(item.response, item.serviceName)).to.eql(item.hosts);
        }
      });
    });

  });

});
