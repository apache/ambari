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

import * as moment from 'moment';

const currentTime = moment();

export const mockData = {
  login: {},
  logout: {},
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
              evtTime: 1496057422531,
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
              _ttl_: '+7DAYS',
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
              evtTime: 1496057422531,
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
              _ttl_: '+7DAYS',
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
            6: {
              graphData: [
                {
                  dataCount: [
                    {
                      name: 'hdfs',
                      value: 800
                    },
                    {
                      name: 'zookeeper',
                      value: 400
                    },
                    {
                      name: 'ambari_metrics',
                      value: 200
                    }
                  ],
                  name: 'admin'
                },
                {
                  dataCount: [
                    {
                      name: 'ambari_agent',
                      value: 400
                    },
                    {
                      name: 'hdfs',
                      value: 600
                    },
                    {
                      name: 'ambari_metrics',
                      value: 300
                    }
                  ],
                  name: 'user'
                }
              ]
            },
            10: {
              graphData: [
                {
                  dataCount: [
                    {
                      name: 'ambari',
                      value: 800
                    },
                    {
                      name: 'hdfs',
                      value: 400
                    },
                    {
                      name: 'hbase',
                      value: 200
                    },
                  ],
                  name: '/user'
                },
                {
                  dataCount: [
                    {
                      name: 'hdfs',
                      value: 400
                    },
                    {
                      name: 'hbase',
                      value: 600
                    },
                    {
                      name: 'kafka',
                      value: 300
                    }
                  ],
                  name: '/root'
                }
              ]
            }
          },
          schema: {
            fields: {
              'cluster': 'key_lower_case',
              'ws_status': 'text_ws',
              'reason': 'text_std_token_lower_case',
              'agent': 'key_lower_case',
              'Base URL': 'key_lower_case',
              'sess': 'key_lower_case',
              'type': 'key_lower_case',
              'seq_num': 'tlong',
              'path': 'key_lower_case',
              'ugi': 'key_lower_case',
              'host': 'key_lower_case',
              'case_id': 'key_lower_case',
              'action': 'key_lower_case',
              'id': 'string',
              'logger_name': 'key_lower_case',
              'text': 'text_std_token_lower_case',
              'Repo id': 'key_lower_case',
              'Stack version': 'tdouble',
              'logfile_line_number': 'tint',
              'Status': 'tlong',
              'RequestId': 'tlong',
              'level': 'key_lower_case',
              'resource': 'key_lower_case',
              'resType': 'key_lower_case',
              'ip': 'key_lower_case',
              'Hostname': 'key_lower_case',
              'Roles': 'key_lower_case',
              'Stack': 'key_lower_case',
              'req_self_id': 'key_lower_case',
              'repoType': 'tint',
              'VersionNote': 'key_lower_case',
              'Cluster name': 'key_lower_case',
              'bundle_id': 'key_lower_case',
              'cliType': 'key_lower_case',
              'reqContext': 'key_lower_case',
              'ws_result_status': 'text_ws',
              'proxyUsers': 'key_lower_case',
              'RequestType': 'key_lower_case',
              'Repositories': 'key_lower_case',
              'logType': 'key_lower_case',
              'Repo version': 'key_lower_case',
              'TaskId': 'tlong',
              'User': 'key_lower_case',
              'access': 'key_lower_case',
              'dst': 'key_lower_case',
              'perm': 'key_lower_case',
              'event_count': 'tlong',
              'repo': 'key_lower_case',
              'reqUser': 'key_lower_case',
              'task_id': 'tlong',
              'Operation': 'key_lower_case',
              'Reason': 'key_lower_case',
              'reqData': 'text_std_token_lower_case',
              'result': 'tint',
              'file': 'key_lower_case',
              'log_message': 'key_lower_case',
              'agentHost': 'key_lower_case',
              'Component': 'key_lower_case',
              'authType': 'key_lower_case',
              'Display name': 'key_lower_case',
              'policy': 'tlong',
              'cliIP': 'key_lower_case',
              'OS': 'key_lower_case',
              'RemoteIp': 'key_lower_case',
              'ResultStatus': 'tlong',
              'evtTime': 'tdate',
              'VersionNumber': 'key_lower_case',
              'url': 'key_lower_case',
              'req_caller_id': 'key_lower_case',
              'enforcer': 'key_lower_case',
              'Command': 'key_lower_case'
            }
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
              logtime: currentTime.valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().add(5, 'd').valueOf(),
              _router_field_: 20
            },
            {
              path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
              host: 'h1',
              level: 'ERROR',
              logtime: currentTime.clone().subtract(2, 'd').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().add(3, 'd').valueOf(),
              _router_field_: 5
            },
            {
              path: '/var/log/ambari-metrics-collector/ambari-metrics-collector.log',
              host: 'h1',
              level: 'FATAL',
              logtime: currentTime.clone().subtract(10, 'd').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().subtract(5, 'd').valueOf(),
              _router_field_: 45
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-server.log',
              host: 'h1',
              level: 'INFO',
              logtime: currentTime.clone().subtract(25, 'h').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().subtract(25, 'h').add(5, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-server.log',
              host: 'h1',
              level: 'DEBUG',
              logtime: currentTime.clone().subtract(25, 'd').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().subtract(20, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-client.log',
              host: 'h1',
              level: 'TRACE',
              logtime: currentTime.clone().subtract(2, 'h').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().subtract(2, 'h').add(5, 'd').valueOf(),
              _router_field_: 55
            },
            {
              path: '/var/log/ambari-metrics-collector/zookeeper-client.log',
              host: 'h1',
              level: 'UNKNOWN',
              logtime: currentTime.clone().subtract(31, 'd').valueOf(),
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
              _ttl_: '+5DAYS',
              _expire_at_: currentTime.clone().subtract(26, 'd').valueOf(),
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
                    name: currentTime.toISOString(),
                    value: '1000'
                  },
                  {
                    name: currentTime.clone().subtract(1, 'h').toISOString(),
                    value: '2000'
                  }
                ],
                name: 'ERROR'
              },
              {
                dataCount: [
                  {
                    name: currentTime.toISOString(),
                    value: '700'
                  },
                  {
                    name: currentTime.clone().subtract(1, 'h').toISOString(),
                    value: '900'
                  }
                ],
                name: 'WARN'
              }
            ]
          },
          hosts: {
            groupList: [
              {
                host: 'h0'
              },
              {
                host: 'h1'
              }
            ],
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
            fields: {
              cluster: 'key_lower_case',
              method: 'key_lower_case',
              level: 'key_lower_case',
              event_count: 'tlong',
              ip: 'string',
              rowtype: 'key_lower_case',
              key_log_message: 'key_lower_case',
              type: 'key_lower_case',
              seq_num: 'tlong',
              path: 'key_lower_case',
              logtype: 'key_lower_case',
              file: 'key_lower_case',
              line_number: 'tint',
              thread_name: 'key_lower_case',
              bundle_id: 'key_lower_case',
              host: 'key_lower_case',
              case_id: 'key_lower_case',
              log_message: 'text_std_token_lower_case',
              id: 'string',
              logger_name: 'key_lower_case',
              text: 'text_std_token_lower_case',
              logfile_line_number: 'tint',
              logtime: 'tdate'
            }
          },
          serviceconfig: '',
          tree: {
            vNodeList: [
              {
                name: 'h0',
                type: 'H',
                value: '1',
                childs: [
                  {
                    name: 'ams_collector',
                    type: 'C',
                    value: '1',
                    logLevelCount: [
                      {
                        name: 'WARN',
                        value: '1'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  }
                ],
                logLevelCount: [
                  {
                    name: 'WARN',
                    value: '1'
                  }
                ],
                isParent: true,
                isRoot: true
              },
              {
                name: 'h1',
                type: 'H',
                value: '6',
                childs: [
                  {
                    name: 'ams_collector',
                    type: 'C',
                    value: '1',
                    logLevelCount: [
                      {
                        name: 'ERROR',
                        value: '1'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  },
                  {
                    name: 'ambari_agent',
                    type: 'C',
                    value: '1',
                    logLevelCount: [
                      {
                        name: 'FATAL',
                        value: '1'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  },
                  {
                    name: 'zookeeper_server',
                    type: 'C',
                    value: '2',
                    logLevelCount: [
                      {
                        name: 'INFO',
                        value: '1'
                      },
                      {
                        name: 'DEBUG',
                        value: '1'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  },
                  {
                    name: 'zookeeper_client',
                    type: 'C',
                    value: '2',
                    logLevelCount: [
                      {
                        name: 'TRACE',
                        value: '1'
                      },
                      {
                        name: 'UNKNOWN',
                        value: '1'
                      }
                    ],
                    isParent: false,
                    isRoot: false
                  }
                ],
                logLevelCount: [
                  {
                    name: 'ERROR',
                    value: '1'
                  },
                  {
                    name: 'FATAL',
                    value: '1'
                  },
                  {
                    name: 'INFO',
                    value: '1'
                  },
                  {
                    name: 'DEBUG',
                    value: '1'
                  },
                  {
                    name: 'TRACE',
                    value: '1'
                  },
                  {
                    name: 'UNKNOWN',
                    value: '1'
                  }
                ],
                isParent: true,
                isRoot: true
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
                _ttl_: '+5DAYS',
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
                _ttl_: '+5DAYS',
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
