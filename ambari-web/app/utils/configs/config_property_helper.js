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
var stringUtils = require('utils/string_utils');

module.exports = {

  initialValue: function (configProperty, localDB, dependencies) {
    var masterComponentHostsInDB = localDB.masterComponentHosts;
    var isOnlyFirstOneNeeded = true;
    var hostWithPort = "([\\w|\\.]*)(?=:)";
    var hostWithPrefix = ":\/\/" + hostWithPort;
    switch (configProperty.get('name')) {
      case 'ranger_admin_password':
        this.generateRangerPassword(configProperty);
        break;
      case 'dfs.namenode.rpc-address':
      case 'dfs.http.address':
      case 'dfs.namenode.http-address':
      case 'dfs.https.address':
      case 'dfs.namenode.https-address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setRecommendedValue(configProperty, hostWithPort,nnHost);
        break;
      case 'fs.default.name':
      case 'fs.defaultFS':
      case 'hbase.rootdir':
      case 'instance.volumes':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setRecommendedValue(configProperty, hostWithPrefix,'://' + nnHost);
        break;
      case 'dfs.secondary.http.address':
      case 'dfs.namenode.secondary.http-address':
        var snnHost = masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE');
        if (snnHost) {
          this.setRecommendedValue(configProperty, hostWithPort,snnHost.hostName);
        }
        break;
      case 'yarn.log.server.url':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setRecommendedValue(configProperty, hostWithPrefix,'://' + hsHost);
        break;
      case 'mapreduce.jobhistory.webapp.address':
      case 'mapreduce.jobhistory.address':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setRecommendedValue(configProperty, hostWithPort,hsHost);
        break;
      case 'yarn.resourcemanager.hostname':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        configProperty.set('recommendedValue',rmHost);
        configProperty.set('value',configProperty.get('recommendedValue'));
        break;
      case 'yarn.resourcemanager.resource-tracker.address':
      case 'yarn.resourcemanager.webapp.https.address':
      case 'yarn.resourcemanager.webapp.address':
      case 'yarn.resourcemanager.scheduler.address':
      case 'yarn.resourcemanager.address':
      case 'yarn.resourcemanager.admin.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setRecommendedValue(configProperty, hostWithPort,rmHost);
        break;
      case 'yarn.timeline-service.webapp.address':
      case 'yarn.timeline-service.webapp.https.address':
      case 'yarn.timeline-service.address':
        var atsHost =  masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER');
        if (atsHost && atsHost.hostName) {
          this.setRecommendedValue(configProperty, hostWithPort,atsHost.hostName);
        }
        break;
      case 'mapred.job.tracker':
      case 'mapred.job.tracker.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName;
        this.setRecommendedValue(configProperty, hostWithPort,jtHost);
        break;
      case 'mapreduce.history.server.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'HISTORYSERVER').hostName;
        this.setRecommendedValue(configProperty, hostWithPort,jtHost);
        break;
      case 'hive_hostname':
        configProperty.set('recommendedValue', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        configProperty.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'hive_master_hosts':
        var hostNames = masterComponentHostsInDB.filter(function (masterComponent) {
          return ['HIVE_METASTORE', 'HIVE_SERVER'].contains(masterComponent.component);
        });
        configProperty.set('value', hostNames.mapProperty('hostName').uniq().join(','));
        break;
      case 'hive_database':
        var newMySQLDBOption = configProperty.get('options').findProperty('displayName', 'New MySQL Database');
        if (newMySQLDBOption) {
          var isNewMySQLDBOptionHidden = !App.get('supports.alwaysEnableManagedMySQLForHive') && App.get('router.currentState.name') != 'configs' &&
            !App.get('isManagedMySQLForHiveEnabled');
          if (isNewMySQLDBOptionHidden && configProperty.get('value') == 'New MySQL Database') {
            configProperty.set('value', 'Existing MySQL Database');
          }
          Em.set(newMySQLDBOption, 'hidden', isNewMySQLDBOptionHidden);
        }
        break;
      case 'oozie_hostname':
        configProperty.set('recommendedValue', masterComponentHostsInDB.filterProperty('component', 'OOZIE_SERVER').mapProperty('hostName')[0]);
        configProperty.set('value', masterComponentHostsInDB.filterProperty('component', 'OOZIE_SERVER').mapProperty('hostName')[0]);
        break;
      case 'oozie.base.url':
        var oozieHost = masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName;
        this.setRecommendedValue(configProperty, hostWithPrefix,'://' + oozieHost);
        break;
      case 'hadoop_host':
        configProperty.set('value', masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName'));
        break;
      case 'hive.metastore.uris':
        var hiveMSUris = this.getHiveMetastoreUris(masterComponentHostsInDB, dependencies['hive.metastore.uris']);
        if (hiveMSUris) {
          this.setRecommendedValue(configProperty, "(.*)", hiveMSUris);
        }
        break;
      case 'storm.zookeeper.servers':
        configProperty.set('value', masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName'));
        break;
      case 'nimbus.host':
        configProperty.set('value', masterComponentHostsInDB.findProperty('component', 'NIMBUS').hostName);
        break;
      case 'nimbus.seeds':
        configProperty.set('value', masterComponentHostsInDB.filterProperty('component', 'NIMBUS').mapProperty('hostName'));
        break;
      case 'kafka.ganglia.metrics.host':
        var gangliaHost =  masterComponentHostsInDB.findProperty('component', 'GANGLIA_SERVER');
        if (gangliaHost) {
          configProperty.set('value', gangliaHost.hostName);
        }
        break;
      case 'hbase.zookeeper.quorum':
        if (configProperty.get('filename') == 'hbase-site.xml') {
          var zkHosts = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
          this.setRecommendedValue(configProperty, "(\\w*)", zkHosts);
        }
        break;
      case 'yarn.resourcemanager.zk-address':
        var value = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').map(function (component) {
          return component.hostName + ':' + dependencies.clientPort
        }).join(',');
        configProperty.setProperties({
          value: value,
          recommendedValue: value
        });
        break;
      case 'zookeeper.connect':
      case 'hive.zookeeper.quorum':
      case 'templeton.zookeeper.hosts':
      case 'hadoop.registry.zk.quorum':
      case 'hive.cluster.delegation.token.store.zookeeper.connectString':
      case 'instance.zookeeper.host': // for accumulo
        var zkHosts = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
        var zkHostPort = zkHosts;
        var regex = "\\w*:(\\d+)";   //regex to fetch the port
        var portValue = configProperty.get('recommendedValue') && configProperty.get('recommendedValue').match(new RegExp(regex));
        if (!portValue) return;
        if (portValue[1]) {
          for ( var i = 0; i < zkHosts.length; i++ ) {
            zkHostPort[i] = zkHosts[i] + ":" + portValue[1];
          }
        }
        this.setRecommendedValue(configProperty, "(.*)", zkHostPort);
        break;
      case 'templeton.hive.properties':
        var hiveMSUris = this.getHiveMetastoreUris(masterComponentHostsInDB, dependencies['hive.metastore.uris']).replace(',', '\\,');
        if (/\/\/localhost:/g.test(configProperty.get('value'))) {
          configProperty.set('recommendedValue', configProperty.get('value') + ',hive.metastore.execute.setugi=true');
        }
        this.setRecommendedValue(configProperty, "(hive\\.metastore\\.uris=)([^\\,]+)", "$1" + hiveMSUris);
        break;
      case 'dfs.name.dir':
      case 'dfs.namenode.name.dir':
      case 'dfs.data.dir':
      case 'dfs.datanode.data.dir':
      case 'yarn.nodemanager.local-dirs':
      case 'yarn.nodemanager.log-dirs':
      case 'mapred.local.dir':
      case 'log.dirs':  // for Kafka Broker
        this.unionAllMountPoints(configProperty, !isOnlyFirstOneNeeded, localDB);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
      case 'yarn.timeline-service.leveldb-timeline-store.path':
      case 'yarn.timeline-service.leveldb-state-store.path':
      case 'dataDir':
      case 'oozie_data_dir':
      case 'storm.local.dir':
      case '*.falcon.graph.storage.directory':
      case '*.falcon.graph.serialize.path':
        this.unionAllMountPoints(configProperty, isOnlyFirstOneNeeded, localDB);
        break;
      case '*.broker.url':
        var falconServerHost = masterComponentHostsInDB.findProperty('component', 'FALCON_SERVER').hostName;
        this.setRecommendedValue(configProperty, 'localhost', falconServerHost);
        break;
      case 'RANGER_HOST':
        var rangerAdminHost = masterComponentHostsInDB.findProperty('component', 'RANGER_ADMIN');
        if(rangerAdminHost) {
          configProperty.set('value', rangerAdminHost.hostName);
        } else {
          configProperty.set('isVisible', 'false');
          configProperty.set('isRequired', 'false');
        }
        break;
      case 'hawq_master_address_host':
        configProperty.set('value', masterComponentHostsInDB.findProperty('component', 'HAWQMASTER').hostName);
        break;
      case 'hawq_standby_address_host':
        var hawqStandby = masterComponentHostsInDB.findProperty('component', 'HAWQSTANDBY');
        if (hawqStandby) {
          configProperty.set('value', hawqStandby.hostName);
        }
        break;
      case 'hawq_dfs_url':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setRecommendedValue(configProperty, hostWithPort, nnHost);
        break;
      case 'hawq_rm_yarn_address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER');
        if (rmHost) {
          this.setRecommendedValue(configProperty, hostWithPort, rmHost.hostName);
        }
        break;
      case 'hawq_rm_yarn_scheduler_address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER');
        if (rmHost) {
          this.setRecommendedValue(configProperty, hostWithPort, rmHost.hostName);
        }
        break;
    }
  },

  
  /**
   * Get hive.metastore.uris initial value
   * @param hosts
   * @param recommendedValue
   * @returns {string}
   */
  getHiveMetastoreUris: function (hosts, recommendedValue) {
    var hiveMSHosts = hosts.filterProperty('component', 'HIVE_METASTORE').mapProperty('hostName'),
      hiveMSUris = hiveMSHosts,
      regex = "\\w*:(\\d+)",
      portValue = recommendedValue && recommendedValue.match(new RegExp(regex));

    if (!portValue) return '';
    if (portValue[1]) {
      for (var i = 0; i < hiveMSHosts.length; i++) {
        hiveMSUris[i] = "thrift://" + hiveMSHosts[i] + ":" + portValue[1];
      }
    }
    return hiveMSUris.join(',');
  },

  /**
   * method to randomly generate <code>ranger_admin_password</code>
   * @returns {*}
   */
  generateRangerPassword: function(configProperty) {
    var value = 'P1!q' + stringUtils.getRandomString(12);
    Em.setProperties(configProperty, {'value': value, 'recommendedValue': value, 'retypedPassword': value});
    return configProperty;
  },
  /**
   * @param regex : String
   * @param replaceWith : String
   * @param configProperty
   */
  setRecommendedValue: function(configProperty, regex, replaceWith) {
    var recommendedValue = !Em.isNone(configProperty.get('recommendedValue')) ? configProperty.get('recommendedValue') : '';
    var re = new RegExp(regex);
    recommendedValue = recommendedValue.replace(re,replaceWith);
    configProperty.set('recommendedValue', recommendedValue);
    configProperty.set('value', !Em.isNone(configProperty.get('recommendedValue')) ? configProperty.get('recommendedValue') : '');
  },

  unionAllMountPoints: function (configProperty, isOnlyFirstOneNeeded, localDB) {
    var hostname = '';
    var mountPointsPerHost = [];
    var mountPointAsRoot;
    var masterComponentHostsInDB = localDB.masterComponentHosts;
    var slaveComponentHostsInDB = localDB.slaveComponentHosts;
    var hostsInfo = localDB.hosts; // which we are setting in installerController in step3.
    //all hosts should be in local storage without using App.Host model
    App.Host.find().forEach(function(item){
      if(!hostsInfo[item.get('id')]){
        hostsInfo[item.get('id')] = {
          name: item.get('id'),
          cpu: item.get('cpu'),
          memory: item.get('memory'),
          disk_info: item.get('diskInfo'),
          bootStatus: "REGISTERED",
          isInstalled: true
        };
      }
    });
    var temp = '';
    var setOfHostNames = [];
    var components = [];
    switch (configProperty.get('name')) {
      case 'dfs.namenode.name.dir':
      case 'dfs.name.dir':
        components = masterComponentHostsInDB.filterProperty('component', 'NAMENODE');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
        components = masterComponentHostsInDB.filterProperty('component', 'SECONDARY_NAMENODE');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'dfs.data.dir':
      case 'dfs.datanode.data.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'DATANODE');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'mapred.local.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'TASKTRACKER') || slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'yarn.nodemanager.log-dirs':
      case 'yarn.nodemanager.local-dirs':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'yarn.timeline-service.leveldb-timeline-store.path':
      case 'yarn.timeline-service.leveldb-state-store.path':
        components = masterComponentHostsInDB.filterProperty('component', 'APP_TIMELINE_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'dataDir':
        components = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'oozie_data_dir':
        components = masterComponentHostsInDB.filterProperty('component', 'OOZIE_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'storm.local.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'SUPERVISOR');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        components = masterComponentHostsInDB.filterProperty('component', 'NIMBUS');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case '*.falcon.graph.storage.directory':
      case '*.falcon.graph.serialize.path':
        components = masterComponentHostsInDB.filterProperty('component', 'FALCON_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'log.dirs':
        components = masterComponentHostsInDB.filterProperty('component', 'KAFKA_BROKER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
    }

    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (setOfHostNames.length === 0) {
      return;
    }

    var allMountPoints = [];
    for (var i = 0; i < setOfHostNames.length; i++) {
      hostname = setOfHostNames[i];

      mountPointsPerHost = hostsInfo[hostname].disk_info;

      mountPointAsRoot = mountPointsPerHost.findProperty('mountpoint', '/');

      // If Server does not send any host details information then atleast one mountpoint should be presumed as root
      // This happens in a single container Linux Docker environment.
      if (!mountPointAsRoot) {
        mountPointAsRoot = {mountpoint: '/'};
      }

      mountPointsPerHost = mountPointsPerHost.filter(function (mPoint) {
        return !(['/', '/home'].contains(mPoint.mountpoint)
          || ['/etc/resolv.conf', '/etc/hostname', '/etc/hosts'].contains(mPoint.mountpoint) // docker specific mount points
          || mPoint.mountpoint && (mPoint.mountpoint.startsWith('/boot') || mPoint.mountpoint.startsWith('/mnt') || mPoint.mountpoint.startsWith('/tmp'))
          || ['devtmpfs', 'tmpfs', 'vboxsf', 'CDFS'].contains(mPoint.type)
          || mPoint.available == 0);
      });

      mountPointsPerHost.forEach(function (mPoint) {
        if( !allMountPoints.findProperty("mountpoint", mPoint.mountpoint)) {
          allMountPoints.push(mPoint);
        }
      }, this);
    }
    if (allMountPoints.length == 0) {
      allMountPoints.push(mountPointAsRoot);
    }
    configProperty.set('value', '');
    var winRegex = /^([a-z]):\\?$/;
    if (!isOnlyFirstOneNeeded) {
      var mPoint = configProperty.get('value');
      if (!mPoint) {
        mPoint = "";
      }
      allMountPoints.forEach(function (eachDrive) {
        if (eachDrive.mountpoint === "/") {
          mPoint += configProperty.get('recommendedValue') + "\n";
        } else if(winRegex.test(eachDrive.mountpoint.toLowerCase())) {
          switch (configProperty.get('name')) {
            case 'dfs.name.dir':
            case 'dfs.namenode.name.dir':
            case 'dfs.data.dir':
            case 'dfs.datanode.data.dir':
              var winDriveUrl = eachDrive.mountpoint.toLowerCase().replace(winRegex, "file:///$1:");
              mPoint += winDriveUrl + configProperty.get('recommendedValue') + "\n";
              break;
            default:
              var winDrive = eachDrive.mountpoint.toLowerCase().replace(winRegex, "$1:");
              var winDir = configProperty.get('recommendedValue').replace(/\//g, "\\");
              mPoint += winDrive + winDir + "\n";
          }
        } else {
          mPoint += eachDrive.mountpoint + configProperty.get('recommendedValue') + "\n";
        }
      }, this);
      configProperty.set('value', mPoint);
      configProperty.set('recommendedValue', mPoint);
    } else {
      var mPoint = allMountPoints[0].mountpoint;
      if (mPoint === "/") {
        mPoint = configProperty.get('recommendedValue');
      } else if(winRegex.test(mPoint.toLowerCase())) {
        switch (configProperty.get('name')) {
          case 'fs.checkpoint.dir':
          case 'dfs.namenode.checkpoint.dir':
            var winDriveUrl = mPoint.toLowerCase().replace(winRegex, "file:///$1:");
            mPoint = winDriveUrl + configProperty.get('recommendedValue') + "\n";
            break;
          case 'zk_data_dir':
            var winDrive = mPoint.toLowerCase().replace(winRegex, "$1:");
            var winDir = configProperty.get('recommendedValue').replace(/\//g, "\\\\");
            mPoint = winDrive + winDir + "\n";
            break;
          default:
            var winDrive = mPoint.toLowerCase().replace(winRegex, "$1:");
            var winDir = configProperty.get('recommendedValue').replace(/\//g, "\\");
            mPoint = winDrive + winDir + "\n";
        }
      } else {
        mPoint = mPoint + configProperty.get('recommendedValue');
      }
      configProperty.set('value', mPoint);
      configProperty.set('recommendedValue', mPoint);
    }
  }
};
