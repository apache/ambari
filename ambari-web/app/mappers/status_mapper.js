/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.statusMapper = App.QuickDataMapper.create({

  config:{
    id:'ServiceInfo.service_name',
    work_status:'ServiceInfo.state'
  },

  config3:{
    id:'id',
    work_status:'HostRoles.state',
    desired_status: 'HostRoles.desired_state'
  },

  map:function (json) {
    var start = new Date().getTime();
    console.log('in status mapper');

    if (json.items) {
      var result = {};

      //host_components
      result = this.parse_host_components(json);

      var hostComponents = App.HostComponent.find();
      var servicesMap = {};
      var hostsMap = {};

      hostComponents.forEach(function(hostComponent) {
        var item = result[hostComponent.get('id')];
        if (item) {
          hostComponent.set('workStatus', item.work_status);
          hostComponent.set('haStatus', item.ha_status);
          this.countHostComponents(hostComponent, hostsMap, hostsMap[hostComponent.get('host.id')]);
          this.countServiceComponents(hostComponent, servicesMap, servicesMap[hostComponent.get('service.id')]);
        }
      }, this);

      json.items.forEach(function (item) {
        item = this.parseIt(item, this.config);
        result[item.id] = item;
      }, this);

      var services = App.Service.find();
      services.forEach(function(service) {
        var item = result[service.get('id')];
        if (item) {
          service.set('workStatus', item.work_status);
        }
      });

      this.updateHostsStatus(App.Host.find(), hostsMap);
      this.updateServicesStatus(App.Service.find(), servicesMap);

      console.log('out status mapper.  Took ' + (new Date().getTime() - start) + 'ms');
    }
  },

  /**
   * fill serviceMap with aggregated data of hostComponents for each service
   * @param hostComponent
   * @param servicesMap
   * @param service
   */
  countServiceComponents: function(hostComponent, servicesMap, service){
    if (!service) {
      service = {
        everyStarted: true,
        everyStartedOrMaintenance: true,
        masterComponents: [],
        isStarted: false,
        isUnknown: false,
        isStarting: false,
        isStopped: false,
        isHbaseActive: false,
        serviceName: this.get('serviceName'),
        isRunning: true,
        runningHCs: [],
        unknownHCs: [],
        hdfsHealthStatus: '',
        toolTipContent: ''
      };
      servicesMap[hostComponent.get('service.id')] = service;
    }
    if (hostComponent.get('isMaster')) {
      if (service.everyStartedOrMaintenance) {
        service.everyStartedOrMaintenance = (hostComponent.get('componentName') === 'NAMENODE' && !App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE') && App.HDFSService.find().filterProperty('activeNameNode.hostName').length > 0)
          ? true : service.everyStartedOrMaintenance = ([App.HostComponentStatus.started, App.HostComponentStatus.maintenance].contains(hostComponent.get('workStatus')));
      } else {
        service.everyStartedOrMaintenance = false;
      }
      service.everyStarted = (service.everyStarted)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.started)
        : false;
      service.isStarted = (!service.isStarted)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.started)
        : true;
      service.isUnknown = (!service.isUnknown)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.unknown)
        : true;
      service.isStarting = (!service.isStarting)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.starting)
        : true;
      service.isStopped = (!service.isStopped)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.stopped)
        : true;
      service.isHbaseActive = (!service.isHbaseActive)
        ? (hostComponent.get('haStatus') === 'active')
        : true;

      service.masterComponents.push(hostComponent);

      // set advanced nameNode display name for HA, active or standby NameNode
      // this is useful on three places: hdfs health status hover tooltip, hdfs service summary and NN component on host detail page
      if (hostComponent.get('componentName') === 'NAMENODE' && !App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE')) {
        var hostName = hostComponent.get('host.hostName');
        var services = App.Service.find();
        var hdfs;
        services.forEach(function (item) {
          if (item.get("serviceName") == "HDFS") {
            hdfs = App.HDFSService.find(item.get('id'));
          }
        }, this);
        var activeNNText = Em.I18n.t('services.service.summary.nameNode.active');
        var standbyNNText = Em.I18n.t('services.service.summary.nameNode.standby');
        if (hdfs) {
          if (hdfs.get('activeNameNode') && hdfs.get('activeNameNode').get('hostName')) {
            var activeHostname = hdfs.get('activeNameNode').get('hostName');
          }
          if (hdfs.get('standbyNameNode') && hdfs.get('standbyNameNode').get('hostName')) {
            var standbyHostname1 = hdfs.get('standbyNameNode').get('hostName');
          }
          if (hdfs.get('standbyNameNode2') && hdfs.get('standbyNameNode2').get('hostName')) {
            var standbyHostname2 = hdfs.get('standbyNameNode2').get('hostName');
          }
          if ( hostName == activeHostname) {
            hostComponent.set('displayNameAdvanced', activeNNText);
          } else if ( hostName == standbyHostname1 || hostName == standbyHostname2) {
            hostComponent.set('displayNameAdvanced', standbyNNText);
          } else {
            hostComponent.set('displayNameAdvanced', null);
          }
        }
      } else if(hostComponent.get('componentName') === 'HBASE_MASTER') {
        if (hostComponent.get('workStatus') === 'STARTED') {
          hostComponent.get('haStatus') == 'active' ? hostComponent.set('displayNameAdvanced', this.t('dashboard.services.hbase.masterServer.active')) : hostComponent.set('displayNameAdvanced', this.t('dashboard.services.hbase.masterServer.standby'));
        } else {
          hostComponent.set('displayNameAdvanced', null);
        }
      }

      if (hostComponent.get("displayNameAdvanced")) {
        service.toolTipContent += hostComponent.get("displayNameAdvanced") + " " + hostComponent.get("componentTextStatus") + "<br/>";
      } else {
        service.toolTipContent += hostComponent.get("displayName") + " " + hostComponent.get("componentTextStatus") + "<br/>";
      }

    }

    if (hostComponent.get('workStatus') !== App.HostComponentStatus.stopped &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.install_failed &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.unknown &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.maintenance) {
      service.isRunning = false;
      service.runningHCs.addObject(hostComponent);
    } else if (hostComponent.get('workStatus') == App.HostComponentStatus.unknown) {
      service.unknownHCs.addObject(hostComponent);
    }
  },

  /**
   * compute service status and properties by servicesMap of hostComponents
   * @param services
   * @param servicesMap
   */
  updateServicesStatus: function(services, servicesMap){
    services.forEach(function(_service){
      var service = servicesMap[_service.get('id')];
      var serviceName = _service.get('serviceName');
      var serviceSpecificObj = null;
      switch (serviceName) {
        case "HDFS":
          serviceSpecificObj = App.HDFSService.find(_service.get('id'));
          break;
        case "YARN":
          serviceSpecificObj = App.YARNService.find(_service.get('id'));
          break;
        case "MAPREDUCE":
          serviceSpecificObj = App.MapReduceService.find(_service.get('id'));
          break;
        case "HBASE":
          serviceSpecificObj = App.HBaseService.find(_service.get('id'));
          break;
      }
      //computation of service health status
      var isGreen = serviceName === 'HBASE' && App.supports.multipleHBaseMasters ? service.isStarted : service.everyStartedOrMaintenance;
      if (isGreen) {
        _service.set('healthStatus', 'green');
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('healthStatus', 'green');
        }
      } else if (service.isUnknown) {
        _service.set('healthStatus', 'yellow');
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('healthStatus', 'yellow');
        }
      } else if (service.isStarting) {
        _service.set('healthStatus', 'green-blinking');
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('healthStatus', 'green-blinking');
        }
      } else if (service.isStopped) {
        _service.set('healthStatus', 'red');
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('healthStatus', 'red');
        }
      } else {
        _service.set('healthStatus', 'red-blinking');
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('healthStatus', 'red-blinking');
        }
      }

      if (serviceName === 'HBASE' && App.supports.multipleHBaseMasters) {
        if (!service.isHbaseActive) {
          _service.set('healthStatus', 'red');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'red');
          }
        }
      }

      _service.set('isStarted', service.everyStarted);
      _service.set('runningHostComponents', service.runningHCs);
      _service.set('unknownHostComponents', service.unknownHCs);
      _service.set('isStopped', service.isRunning);
      _service.set('toolTipContent', service.toolTipContent);
      if (serviceSpecificObj != null) {
        serviceSpecificObj.set('isStarted', service.everyStarted);
        serviceSpecificObj.set('isStopped', service.isRunning);
        serviceSpecificObj.set('toolTipContent', service.toolTipContent);
      }
    }, this);
  },

  /**
   * fill hostsMap with aggregated data of hostComponents for each host
   * @param hostComponent
   * @param hostsMap
   * @param host
   */
  countHostComponents: function(hostComponent, hostsMap, host){
    var isMasterRunning = (hostComponent.get('isMaster') && hostComponent.get('workStatus') === App.HostComponentStatus.started);
    var isSlaveRunning = (hostComponent.get('isSlave') && hostComponent.get('workStatus') === App.HostComponentStatus.started);
    if (host) {
      host.mastersRunning = host.mastersRunning + ~~isMasterRunning;
      host.slavesRunning = host.slavesRunning + ~~isSlaveRunning;
      host.totalMasters = host.totalMasters + ~~hostComponent.get('isMaster');
      host.totalSlaves = host.totalSlaves + ~~hostComponent.get('isSlave');
    } else {
      hostsMap[hostComponent.get('host.id')] = {
        mastersRunning: ~~isMasterRunning,
        slavesRunning: ~~isSlaveRunning,
        totalMasters: ~~hostComponent.get('isMaster'),
        totalSlaves: ~~hostComponent.get('isSlave')
      }
    }
  },

  /**
   * compute host status by hostsMap of hostComponents
   * @param hosts
   * @param hostsMap
   */
  updateHostsStatus: function(hosts, hostsMap){
    hosts.forEach(function(_host){
      var healthStatus = _host.get('healthStatus');
      var host = hostsMap[_host.get('id')];
      var status;
      var masterComponentsRunning = (host.mastersRunning === host.totalMasters);
      var slaveComponentsRunning = (host.slavesRunning === host.totalSlaves);
      if (_host.get('isNotHeartBeating') || healthStatus == 'UNKNOWN') {
        status = 'DEAD-YELLOW';
      } else if (masterComponentsRunning && slaveComponentsRunning) {
        status = 'LIVE';
      } else if (host.totalMasters > 0 && !masterComponentsRunning) {
        status = 'DEAD-RED';
      } else {
        status = 'DEAD-ORANGE';
      }
      if (status) {
        healthStatus = status;
      }
      _host.set('healthClass', 'health-status-' + healthStatus);
    }, this);
  },

  parse_host_components: function(json) {
    var result = {};
    json.items.forEach(function (item) {
      item.components.forEach(function (component) {
        component.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
          result[host_component.id] = {
            work_status: host_component.HostRoles.state,
            ha_status: host_component.HostRoles.ha_status
          };
        }, this)
      }, this)
    }, this);
    return result;
  }

});
