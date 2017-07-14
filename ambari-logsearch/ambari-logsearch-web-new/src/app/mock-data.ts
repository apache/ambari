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

import * as moment from 'moment-timezone';

export const mockData = {
  login: {},
  api: {
    v1: {
      audit: {
        logs: {
          logList: [
            {
              policy: 'policy',
              reason: 'Authentication required',
              result: 0,
              text: 'Please log in',
              tags: [
                'ambari_agent'
              ],
              resource: '/ambari-agent',
              sess: '0',
              access: '0',
              logType: 'AmbariAudit',
              tags_str: 'ambari_agent',
              resType: 'agent',
              reqUser: 'admin',
              reqData: 'data',
              repoType: 1,
              repo: 'ambari',
              proxyUsers: [
                'admin'
              ],
              evtTime: '2017-05-29T11:30:22.531Z',
              enforcer: 'ambari-acl',
              reqContext: 'ambari',
              cliType: 'GET',
              cliIP: '192.168.0.1',
              agent: 'agent',
              agentHost: 'localhost',
              action: 'SERVICE_CHECK',
              type: 'ambari-audit',
              _version_: 2,
              id: 'id0',
              file: 'ambari-agent.log',
              seq_num: 3,
              bundle_id: 'b0',
              case_id: 'c0',
              log_message: 'User(admin), Operation(SERVICE_CHECK)',
              logfile_line_number: 4,
              message_md5: '12345678900987654321',
              cluster: 'cl0',
              event_count: 0,
              event_md5: '09876543211234567890',
              event_dur_ms: 100,
              _ttl_: "+7DAYS",
              _expire_at_: '2017-05-29T11:30:22.531Z',
              _router_field_: 5
            },
            {
              policy: 'policy',
              reason: 'Server error',
              result: 1,
              text: 'Something went wrong',
              tags: [
                'ambari_agent'
              ],
              resource: '/ambari-agent',
              sess: '1',
              access: '1',
              logType: 'AmbariAudit',
              tags_str: 'ambari_server',
              resType: 'server',
              reqUser: 'user',
              reqData: 'data',
              repoType: 1,
              repo: 'ambari',
              proxyUsers: [
                'user'
              ],
              evtTime: '2017-05-29T11:30:22.531Z',
              enforcer: 'hdfs',
              reqContext: 'ambari_server',
              cliType: 'PUT',
              cliIP: '192.168.0.1',
              agent: 'agent',
              agentHost: 'localhost',
              action: 'SERVICE_CHECK',
              type: 'ambari-audit',
              _version_: 4,
              id: 'id1',
              file: 'ambari-agent.log',
              seq_num: 5,
              bundle_id: 'b1',
              case_id: 'c1',
              log_message: 'User(user), Operation(SERVICE_CHECK)',
              logfile_line_number: 6,
              message_md5: '10293847561029384756',
              cluster: 'cl1',
              event_count: 2,
              event_md5: '01928374650192837465',
              event_dur_ms: 500,
              _ttl_: "+7DAYS",
              _expire_at_: '2017-05-29T11:30:22.531Z',
              _router_field_: 10
            }
          ],
          bargraph: {
            graphData: [
              {
                dataCount: [
                  {
                    name: 'n0',
                    value: 1
                  },
                  {
                    name: 'n1',
                    value: 2
                  }
                ],
                name: 'graph0'
              },
              {
                dataCount: [
                  {
                    name: 'n2',
                    value: 10
                  },
                  {
                    name: 'n3',
                    value: 20
                  }
                ],
                name: 'graph1'
              }
            ]
          },
          components: {},
          resources: {
            graphData: [
              {
                dataCount: [
                  {
                    name: 'n16',
                    value: 800
                  },
                  {
                    name: 'n17',
                    value: 400
                  }
                ],
                name: 'graph8'
              },
              {
                dataCount: [
                  {
                    name: 'n18',
                    value: 600
                  },
                  {
                    name: 'n19',
                    value: 300
                  }
                ],
                name: 'graph9'
              }
            ]
          },
          schema: {
            fields: ''
          },
          serviceload: {
            graphData: [
              {
                dataCount: [
                  {
                    name: 'n4',
                    value: 1
                  },
                  {
                    name: 'n5',
                    value: 2
                  }
                ],
                name: 'graph2'
              },
              {
                dataCount: [
                  {
                    name: 'n6',
                    value: 10
                  },
                  {
                    name: 'n7',
                    value: 20
                  }
                ],
                name: 'graph3'
              }
            ]
          }
        }
      },
      public: {
        config: {}
      },
      service: {
        logs: {
          logList: [
            {
              path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
              host: 'h0',
              level: 'WARN',
              logtime: moment().valueOf(),
              ip: '192.168.0.1',
              logfile_line_number: 8,
              type: 'ams_collector',
              _version_: 9,
              id: 'id2',
              file: 'ambari-metrics-collector.log',
              seq_num: 10,
              bundle_id: 'b2',
              case_id: 'c2',
              log_message: 'Connection refused.\nPlease check Ambari Metrics.\nCheck log file for details.',
              message_md5: '1357908642',
              cluster: 'cl2',
              event_count: 5,
              event_md5: '1908755391',
              event_dur_ms: 200,
              _ttl_: "+5DAYS",
              _expire_at_: moment().add(5, 'd').valueOf(),
              _router_field_: 20
            },
            {
              path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
              host: 'h1',
              level: 'ERROR',
              logtime: moment().subtract(2, 'd'),
              ip: '192.168.0.2',
              type: 'ams_collector',
              _version_: 14,
              id: 'id3',
              file: 'ambari-metrics-collector.log',
              seq_num: 15,
              bundle_id: 'b3',
              case_id: 'c3',
              log_message: 'Connection refused.\nPlease check Ambari Metrics.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1357908642',
              cluster: 'cl3',
              event_count: 2,
              event_md5: '1029384756',
              event_dur_ms: 700,
              _ttl_: "+5DAYS",
              _expire_at_: moment().add(3, 'd').valueOf(),
              _router_field_: 5
            },
            {
              path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
              host: 'h1',
              level: 'FATAL',
              logtime: moment().subtract(10, 'd').valueOf(),
              ip: '192.168.0.3',
              type: 'ambari_agent',
              _version_: 14,
              id: 'id4',
              file: 'ambari-agent.log',
              seq_num: 15,
              bundle_id: 'b4',
              case_id: 'c4',
              log_message: 'Connection refused.\nPlease check Ambari Agent.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1038027502',
              cluster: 'cl4',
              event_count: 2,
              event_md5: '67589403',
              event_dur_ms: 100,
              _ttl_: "+5DAYS",
              _expire_at_: moment().subtract(5, 'd').valueOf(),
              _router_field_: 45
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-server.log',
              host: 'h1',
              level: 'INFO',
              logtime: moment().subtract(25, 'h').valueOf(),
              ip: '192.168.0.4',
              type: 'zookeeper_server',
              _version_: 14,
              id: 'id4',
              file: 'zookeeper_server.log',
              seq_num: 15,
              bundle_id: 'b0',
              case_id: 'c0',
              log_message: 'Connection refused.\nPlease check ZooKeeper Server.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1038027502',
              cluster: 'cl0',
              event_count: 2,
              event_md5: '67589403',
              event_dur_ms: 1000,
              _ttl_: "+5DAYS",
              _expire_at_: moment().subtract(25, 'h').add(5, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-server.log',
              host: 'h1',
              level: 'DEBUG',
              logtime: moment().subtract(25, 'd').valueOf(),
              ip: '192.168.0.4',
              type: 'zookeeper_server',
              _version_: 14,
              id: 'id4',
              file: 'zookeeper_server.log',
              seq_num: 15,
              bundle_id: 'b0',
              case_id: 'c0',
              log_message: 'Connection refused.\nPlease check ZooKeeper Server.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1038027502',
              cluster: 'cl1',
              event_count: 2,
              event_md5: '67589403',
              event_dur_ms: 1000,
              _ttl_: "+5DAYS",
              _expire_at_: moment().subtract(20, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-client.log',
              host: 'h1',
              level: 'TRACE',
              logtime: moment().subtract(2, 'h').valueOf(),
              ip: '192.168.0.4',
              type: 'zookeeper_client',
              _version_: 14,
              id: 'id4',
              file: 'zookeeper_client.log',
              seq_num: 15,
              bundle_id: 'b0',
              case_id: 'c0',
              log_message: 'Connection refused.\nPlease check ZooKeeper Client.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1038027502',
              cluster: 'cl1',
              event_count: 2,
              event_md5: '67589403',
              event_dur_ms: 1000,
              _ttl_: "+5DAYS",
              _expire_at_: moment().subtract(2, 'h').add(5, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-client.log',
              host: 'h1',
              level: 'UNKNOWN',
              logtime: moment().subtract(31, 'd').valueOf(),
              ip: '192.168.0.4',
              type: 'zookeeper_client',
              _version_: 14,
              id: 'id4',
              file: 'zookeeper_client.log',
              seq_num: 15,
              bundle_id: 'b0',
              case_id: 'c0',
              log_message: 'Connection refused.\nPlease check ZooKeeper Client.\nCheck log file for details.',
              logfile_line_number: 16,
              message_md5: '1038027502',
              cluster: 'cl1',
              event_count: 2,
              event_md5: '67589403',
              event_dur_ms: 1000,
              _ttl_: "+5DAYS",
              _expire_at_: moment().subtract(26, 'd').valueOf(),
              _router_field_: 55
            }
          ],
          aggregated: {
            graphData: [
              {
                name: 'n0',
                count: 100,
                dataList: [
                  {
                    name: 'n1',
                    count: 50,
                    dataList: null
                  },
                  {
                    name: 'n2',
                    count: 200,
                    dataList: null
                  }
                ]
              },
              {
                name: 'n3',
                count: 10,
                dataList: [
                  {
                    name: 'n4',
                    count: 5,
                    dataList: null
                  },
                  {
                    name: 'n5',
                    count: 20,
                    dataList: null
                  }
                ]
              }
            ]
          },
          components: {
            count: {
              anygraph: {
                graphData: [
                  {
                    dataCount: [
                      {
                        name: 'n8',
                        value: 50
                      },
                      {
                        name: 'n9',
                        value: 100
                      }
                    ],
                    name: 'graph4'
                  },
                  {
                    dataCount: [
                      {
                        name: 'n10',
                        value: 5
                      },
                      {
                        name: 'n11',
                        value: 10
                      }
                    ],
                    name: 'graph5'
                  }
                ]
              }
            },
            levels: {
              counts: {
                vNodeList: [
                  {
                    name: 'ambari',
                    type: 0,
                    logLevelCount: [
                      {
                        name: 'ERROR',
                        value: '10'
                      },
                      {
                        name: 'WARN',
                        value: '50'
                      }
                    ],
                    childs: [
                      {
                        name: 'hdfs',
                        type: 2,
                        logLevelCount: [
                          {
                            name: 'ERROR',
                            value: '10'
                          },
                          {
                            name: 'WARN',
                            value: '20'
                          }
                        ],
                        isParent: false,
                        isRoot: false
                      },
                      {
                        name: 'zookeeper',
                        type: 3,
                        logLevelCount: [
                          {
                            name: 'ERROR',
                            value: '20'
                          },
                          {
                            name: 'WARN',
                            value: '40'
                          }
                        ],
                        isParent: false,
                        isRoot: false
                      }
                    ],
                    isParent: true,
                    isRoot: false
                  },
                  {
                    name: 'ambari_agent',
                    type: 1,
                    logLevelCount: [
                      {
                        name: 'ERROR',
                        value: '100'
                      },
                      {
                        name: 'WARN',
                        value: '500'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  }
                ]
              }
            },
            groupList: [
              {
                type: 'ams_collector'
              },
              {
                type: 'ambari_agent'
              },
              {
                type: 'zookeeper_server'
              },
              {
                type: 'zookeeper_client'
              }
            ]
          },
          files: {
            hostLogFiles: {
              clusters: [
                'c0',
                'c1'
              ],
              services: [
                'hdfs',
                'zookeeper'
              ]
            }
          },
          histogram: {
            graphData: [
              {
                dataCount: [
                  {
                    name: 'n12',
                    value: 1000
                  },
                  {
                    name: 'n13',
                    value: 2000
                  }
                ],
                name: 'graph6'
              },
              {
                dataCount: [
                  {
                    name: 'n14',
                    value: 700
                  },
                  {
                    name: 'n15',
                    value: 900
                  }
                ],
                name: 'graph7'
              }
            ]
          },
          hosts: {
            components: {
              vNodeList: [
                {
                  name: 'ambari',
                  type: 0,
                  logLevelCount: [
                    {
                      name: 'ERROR',
                      value: '100'
                    },
                    {
                      name: 'WARN',
                      value: '500'
                    }
                  ],
                  childs: [
                    {
                      name: 'ambari_metrics',
                      type: 2,
                      logLevelCount: [
                        {
                          name: 'ERROR',
                          value: '100'
                        },
                        {
                          name: 'WARN',
                          value: '200'
                        }
                      ],
                      isParent: false,
                      isRoot: false
                    },
                    {
                      name: 'hbase',
                      type: 3,
                      logLevelCount: [
                        {
                          name: 'ERROR',
                          value: '200'
                        },
                        {
                          name: 'WARN',
                          value: '400'
                        }
                      ],
                      isParent: false,
                      isRoot: false
                    }
                  ],
                  isParent: true,
                  isRoot: false
                },
                {
                  name: 'ambari_server',
                  type: 1,
                  logLevelCount: [
                    {
                      name: 'ERROR',
                      value: '1000'
                    },
                    {
                      name: 'WARN',
                      value: '5000'
                    }
                  ],
                  isParent: false,
                  isRoot: false
                }
              ]
            },
            count: {
              getvCounts: [
                {
                  name: 'n20',
                  count: 100
                },
                {
                  name: 'n21',
                  count: 200
                }
              ]
            }
          },
          levels: {
            counts: {
              getvNameValues: [
                {
                  name: 'n22',
                  count: 1000
                },
                {
                  name: 'n23',
                  count: 2000
                }
              ]
            }
          },
          schema: {
            fields: ''
          },
          serviceconfig: '',
          tree: {
            vNodeList: [
              {
                name: 'ambari',
                type: 0,
                logLevelCount: [
                  {
                    name: 'ERROR',
                    value: '1000'
                  },
                  {
                    name: 'WARN',
                    value: '5000'
                  }
                ],
                childs: [
                  {
                    name: 'yarn',
                    type: 2,
                    logLevelCount: [
                      {
                        name: 'ERROR',
                        value: '1000'
                      },
                      {
                        name: 'WARN',
                        value: '2000'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  },
                  {
                    name: 'hive',
                    type: 3,
                    logLevelCount: [
                      {
                        name: 'ERROR',
                        value: '2000'
                      },
                      {
                        name: 'WARN',
                        value: '4000'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  }
                ],
                isParent: true,
                isRoot: false
              },
              {
                name: 'ambari_server',
                type: 1,
                logLevelCount: [
                  {
                    name: 'ERROR',
                    value: '10000'
                  },
                  {
                    name: 'WARN',
                    value: '50000'
                  }
                ],
                isParent: false,
                isRoot: false
              }
            ]
          },
          truncated: {
            logList: [
              {
                path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
                host: 'h0',
                level: 'WARN',
                logtime: '2017-05-28T11:30:22.531Z',
                ip: '192.168.0.1',
                logfile_line_number: 8,
                type: 'ams_collector',
                _version_: 9,
                id: 'id2',
                file: 'ambari-metrics-collector.log',
                seq_num: 10,
                bundle_id: 'b2',
                case_id: 'c2',
                log_message: 'Connection refused',
                message_md5: '1357908642',
                cluster: 'cl2',
                event_count: 5,
                event_md5: '1908755391',
                event_dur_ms: 200,
                _ttl_: "+5DAYS",
                _expire_at_: '2017-05-29T11:30:22.531Z',
                _router_field_: 20
              },
              {
                path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
                host: 'h1',
                level: 'ERROR',
                logtime: '2017-05-28T10:30:22.531Z',
                ip: '192.168.0.2',
                type: 'ams_collector',
                _version_: 14,
                id: 'id3',
                file: 'ambari-metrics-collector.log',
                seq_num: 15,
                bundle_id: 'b3',
                case_id: 'c3',
                log_message: 'Connection refused',
                logfile_line_number: 16,
                message_md5: '1357908642',
                cluster: 'cl3',
                event_count: 2,
                event_md5: '1029384756',
                event_dur_ms: 700,
                _ttl_: "+5DAYS",
                _expire_at_: '2017-05-29T10:30:22.531Z',
                _router_field_: 5
              }
            ]
          },
          clusters: [
            'cl0',
            'cl1',
            'cl2',
            'cl3',
            'cl4'
          ]
        }
      },
      status: {
        auditlogs: {
          znodeReady: true,
          solrCollectionReady: true,
          solrAliasReady: false,
          configurationUploaded: true
        },
        servicelogs: {
          znodeReady: true,
          solrCollectionReady: true,
          configurationUploaded: true
        },
        userconfig: {
          znodeReady: true,
          solrCollectionReady: true,
          configurationUploaded: true
        }
      },
      userconfig: {
        userConfigList: [
          {
            id: 'c0',
            userName: 'admin',
            filtername: 'service',
            values: 'hdfs',
            shareNameList: [
              's0',
              's1'
            ],
            rowType: 'history'
          },
          {
            id: 'c0',
            userName: 'user',
            filtername: 'component',
            values: 'namenode',
            shareNameList: [
              's2',
              's3'
            ],
            rowType: 'history'
          }
        ],
        filters: {
          filter0: {
            label: 'filter0',
            hosts: [
              'h0',
              'h1'
            ],
            defaultLevels: [
              'l0',
              'l1'
            ],
            overrideLevels: [
              'l2',
              'l3'
            ],
            expiryTime: '2017-05-29T11:30:22.531Z'
          },
          filter1: {
            label: 'filter1',
            hosts: [
              'h1',
              'h2'
            ],
            defaultLevels: [
              'l4',
              'l5'
            ],
            overrideLevels: [
              'l6',
              'l7'
            ],
            expiryTime: '2017-05-30T11:30:22.531Z'
          }
        },
        names: []
      }
    }
  }
};