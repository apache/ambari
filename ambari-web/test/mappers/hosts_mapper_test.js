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

var Ember = require('ember');
var App = require('app');

require('models/host');
require('models/host_component');
require('mappers/server_data_mapper');
require('mappers/hosts_mapper');

describe('App.hostsMapper', function () {

  describe('#sortByPublicHostName()', function () {
    var tests = [
      {
        i: [
          {public_host_name: 'host0'},
          {public_host_name: 'host1'},
          {public_host_name: 'host2'},
          {public_host_name: 'host3'}
        ],
        m: 'Sorted array',
        e: ['host0','host1','host2','host3']
      },
      {
        i: [
          {public_host_name: 'host3'},
          {public_host_name: 'host2'},
          {public_host_name: 'host1'},
          {public_host_name: 'host0'}
        ],
        m: 'Reverse sorted array',
        e: ['host0','host1','host2','host3']
      },
      {
        i: [
          {public_host_name: 'host2'},
          {public_host_name: 'host3'},
          {public_host_name: 'host0'},
          {public_host_name: 'host1'}
        ],
        m: 'Shuffled array',
        e: ['host0','host1','host2','host3']
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.hostsMapper.sortByPublicHostName(test.i).mapProperty('public_host_name')).to.eql(test.e);
      });
    });
  });

  var hosts = {
    "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/disk_info,Hosts/cpu_count,Hosts/total_mem,Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/load/load_one",
    "items" : [
      {
        "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal",
        "metrics" : {
          "disk" : {
            "disk_total" : 896.17,
            "disk_free" : 846.304936111
          },
          "load" : {
            "load_one" : 0.786194444444
          },
          "memory" : {
            "mem_total" : 7514116.0,
            "swap_free" : 0.0,
            "mem_buffers" : 114389.877778,
            "mem_shared" : 0.0,
            "mem_free" : 4320263.07778,
            "swap_total" : 0.0,
            "mem_cached" : 2229920.77778
          },
          "cpu" : {
            "cpu_speed" : 2266.0,
            "cpu_num" : 2.0,
            "cpu_wio" : 0.393055555556,
            "cpu_idle" : 85.9025,
            "cpu_nice" : 0.0,
            "cpu_aidle" : 0.0,
            "cpu_system" : 2.75111111111,
            "cpu_user" : 10.9405555556
          }
        },
        "Hosts" : {
          "host_status" : "HEALTHY",
          "cluster_name" : "tdk",
          "public_host_name" : "ec2-107-21-192-172.compute-1.amazonaws.com",
          "cpu_count" : 2,
          "total_mem" : 7518289,
          "os_arch" : "x86_64",
          "host_name" : "ip-10-83-54-214.ec2.internal",
          "disk_info" : [
            {
              "available" : "5431780",
              "used" : "2403168",
              "percent" : "31%",
              "size" : "8254240",
              "type" : "ext4",
              "mountpoint" : "/"
            },
            {
              "available" : "3757056",
              "used" : "0",
              "percent" : "0%",
              "size" : "3757056",
              "type" : "tmpfs",
              "mountpoint" : "/dev/shm"
            },
            {
              "available" : "411234588",
              "used" : "203012",
              "percent" : "1%",
              "size" : "433455904",
              "type" : "ext3",
              "mountpoint" : "/grid/0"
            },
            {
              "available" : "411234588",
              "used" : "203012",
              "percent" : "1%",
              "size" : "433455904",
              "type" : "ext3",
              "mountpoint" : "/grid/1"
            }
          ],
          "ip" : "10.83.54.214",
          "os_type" : "centos6",
          "last_heartbeat_time" : 1369829865123
        },
        "host_components" : [
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HDFS_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HDFS_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HBASE_REGIONSERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HBASE_REGIONSERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HIVE_METASTORE",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HIVE_METASTORE",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/DATANODE",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "DATANODE",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HIVE_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HIVE_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HBASE_MASTER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "ha_status" : "passive",
              "component_name" : "HBASE_MASTER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/MYSQL_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "MYSQL_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/ZOOKEEPER_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "ZOOKEEPER_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/OOZIE_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "OOZIE_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/MAPREDUCE_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "MAPREDUCE_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/NAMENODE",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "NAMENODE",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/GANGLIA_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "GANGLIA_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/TASKTRACKER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "TASKTRACKER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/SQOOP",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "SQOOP",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/GANGLIA_MONITOR",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "GANGLIA_MONITOR",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HIVE_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HIVE_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/JOBTRACKER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "JOBTRACKER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/ZOOKEEPER_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "ZOOKEEPER_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/SECONDARY_NAMENODE",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "SECONDARY_NAMENODE",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HBASE_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HBASE_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/OOZIE_CLIENT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "OOZIE_CLIENT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/WEBHCAT_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "WEBHCAT_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/HCAT",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "HCAT",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/PIG",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "PIG",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          },
          {
            "href" : "http://ec2-107-21-192-172.compute-1.amazonaws.com:8080/api/v1/clusters/tdk/hosts/ip-10-83-54-214.ec2.internal/host_components/NAGIOS_SERVER",
            "HostRoles" : {
              "cluster_name" : "tdk",
              "component_name" : "NAGIOS_SERVER",
              "host_name" : "ip-10-83-54-214.ec2.internal"
            }
          }
        ]
      }
    ]
  };

  describe('#parse', function() {
    var result = App.hostsMapper.parse(hosts.items);
    it('Hosts are loaded', function() {
      expect(result.length).to.equal(1);
    });
    var host = result[0];
    it('Disk Usage calculated', function() {
      expect(host.disk_usage).to.equal('5.6');
    });
    it('CPU Usage calculated', function() {
      expect(host.cpu_usage).to.equal('13.7');
    });
    it('Memory Usage calculated', function() {
      expect(host.memory_usage).to.equal('42.5');
    });
    it('Host Complonents loaded', function() {
      expect(host.host_components.length).to.equal(hosts.items[0].host_components.length);
    });
  });

});
