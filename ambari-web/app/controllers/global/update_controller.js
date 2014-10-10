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

App.UpdateController = Em.Controller.extend({
  name: 'updateController',
  isUpdated: false,
  cluster: null,
  isWorking: false,
  timeIntervalId: null,
  clusterName: function () {
    return App.router.get('clusterController.clusterName');
  }.property('App.router.clusterController.clusterName'),
  location: function () {
    return App.router.get('location.lastSetURL');
  }.property('App.router.location.lastSetURL'),

  /**
   * keys which should be preloaded in order to filter hosts by host-components
   */
  hostsPreLoadKeys: ['host_components/HostRoles/component_name', 'host_components/HostRoles/stale_configs', 'host_components/HostRoles/maintenance_state'],

  paginationKeys: ['page_size', 'from'],

  getUrl: function (testUrl, url) {
    return (App.get('testMode')) ? testUrl : App.apiPrefix + '/clusters/' + this.get('clusterName') + url;
  },

  /**
   * construct URL from real URL and query parameters
   * @param testUrl
   * @param realUrl
   * @param queryParams
   * @return {String}
   */
  getComplexUrl: function (testUrl, realUrl, queryParams) {
    var prefix = App.get('apiPrefix') + '/clusters/' + App.get('clusterName'),
      params = '';

    if (App.get('testMode')) {
      return testUrl;
    } else {
      if (queryParams) {
        params = this.computeParameters(queryParams);
      }
      return prefix + realUrl.replace('<parameters>', params);
    }
  },

  /**
   * compute parameters according to their type
   * @param queryParams
   * @return {String}
   */
  computeParameters: function (queryParams) {
    var params = '';

    queryParams.forEach(function (param) {
      switch (param.type) {
        case 'EQUAL':
          params += param.key + '=' + param.value;
          break;
        case 'LESS':
          params += param.key + '<' + param.value;
          break;
        case 'MORE':
          params += param.key + '>' + param.value;
          break;
        case 'MATCH':
          params += param.key + '.matches(' + param.value + ')';
          break;
        case 'MULTIPLE':
          params += param.key + '.in(' + param.value.join(',') + ')';
          break;
        case 'SORT':
          params += 'sortBy=' + param.key + '.' + param.value;
          break;
        case 'CUSTOM':
          param.value.forEach(function(item, index){
            param.key = param.key.replace('{' + index + '}', item);
          }, this);
          params += param.key;
          break;
      }
      params += '&';
    });
    return params;
  },

  /**
   * depict query parameters of table
   */
  queryParams: Em.Object.create({
    'Hosts': []
  }),

  /**
   * map describes relations between updater function and table
   */
  tableUpdaterMap: {
    'Hosts': 'updateHost'
  },

  /**
   * Start polling, when <code>isWorking</code> become true
   */
  updateAll: function () {
    if (this.get('isWorking')) {
      App.updater.run(this, 'updateServices', 'isWorking');
      App.updater.run(this, 'updateHost', 'isWorking');
      App.updater.run(this, 'updateServiceMetricConditionally', 'isWorking', App.componentsUpdateInterval);
      App.updater.run(this, 'updateComponentsState', 'isWorking', App.componentsUpdateInterval);
      App.updater.run(this, 'graphsUpdate', 'isWorking');
      if (App.supports.hostOverrides) {
        App.updater.run(this, 'updateComponentConfig', 'isWorking');
      }
    }
  }.observes('isWorking'),
  /**
   * Update service metrics depending on which page is open
   * Make a call only on follow pages:
   * /main/dashboard
   * /main/services/*
   * @param callback
   */
  updateServiceMetricConditionally: function (callback) {
    if (/\/main\/(dashboard|services).*/.test(this.get('location'))) {
      this.updateServiceMetric(callback);
    } else {
      callback();
    }
  },

  updateHost: function (callback, error) {
    var testUrl = App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hosts.json' : '/data/hosts/hosts.json',
      self = this,
      hostDetailsFilter = '';
    var realUrl = '/hosts?<parameters>fields=Hosts/host_name,Hosts/maintenance_state,Hosts/public_host_name,Hosts/cpu_count,Hosts/ph_cpu_count,' +
      'Hosts/host_status,Hosts/last_heartbeat_time,Hosts/ip,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,' +
      'host_components/HostRoles/stale_configs,host_components/HostRoles/service_name,metrics/disk,metrics/load/load_one,Hosts/total_mem,' +
      'alerts/summary<hostAuxiliaryInfo>&minimal_response=true';
    var hostAuxiliaryInfo = ',Hosts/os_arch,Hosts/os_type,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free';

    if (App.router.get('currentState.name') == 'index' && App.router.get('currentState.parentState.name') == 'hosts') {
      App.updater.updateInterval('updateHost', App.get('contentUpdateInterval'));
    }
    else {
      if (App.router.get('currentState.name') == 'summary' && App.router.get('currentState.parentState.name') == 'hostDetails') {
        hostDetailsFilter = App.router.get('location.lastSetURL').match(/\/hosts\/(.*)\/summary/)[1];
        App.updater.updateInterval('updateHost', App.get('componentsUpdateInterval'));
      }
      else {
        callback();
        // On pages except for hosts/hostDetails, making sure hostsMapper loaded only once on page load, no need to update, but at least once
        if (App.router.get('clusterController.isLoaded')) {
          return;
        }
      }
    }
    var mainHostController = App.router.get('mainHostController'),
      sortProperties = mainHostController.getSortProperties();
    if (hostDetailsFilter) {
      //if host details page opened then request info only of one displayed host
      this.get('queryParams').set('Hosts', [
        {
          key: 'Hosts/host_name',
          value: [hostDetailsFilter],
          type: 'MULTIPLE'
        }
      ]);
    } else {
      hostAuxiliaryInfo = '';
      this.get('queryParams').set('Hosts', mainHostController.getQueryParameters(true));
    }
    realUrl = realUrl.replace('<hostAuxiliaryInfo>', hostAuxiliaryInfo);

    var clientCallback = function (skipCall, queryParams) {
      if (skipCall) {
        //no hosts match filter by component
        App.hostsMapper.map({
          items: [],
          itemTotal: '0'
        });
        callback();
      }
      else {
        var params = self.computeParameters(queryParams),
          paginationProps = self.computeParameters(queryParams.filter(function (param) {
            return (this.get('paginationKeys').contains(param.key));
          }, self)),
          sortProps = self.computeParameters(sortProperties);

        if ((params.length + paginationProps.length + sortProps.length) > 0) {
          realUrl = App.get('apiPrefix') + '/clusters/' + App.get('clusterName') +
            realUrl.replace('<parameters>', '') +
            (paginationProps.length > 0 ? '&' + paginationProps.substring(0, paginationProps.length - 1) : '') +
            (sortProps.length > 0 ? '&' + sortProps.substring(0, sortProps.length - 1) : '');
          if (App.get('testMode')) {
            realUrl = testUrl;
          }
          App.HttpClient.get(realUrl, App.hostsMapper, {
            complete: callback,
            doGetAsPost: true,
            params: params.substring(0, params.length - 1),
            error: error
          });
        }
        else {
          var hostsUrl = self.getComplexUrl(testUrl, realUrl, queryParams);
          App.HttpClient.get(hostsUrl, App.hostsMapper, {
            complete: callback,
            doGetAsPost: false,
            error: error
          });
        }
      }
    };

    if (!this.preLoadHosts(clientCallback)) {
      clientCallback(false, self.get('queryParams.Hosts'));
    }
  },

  /**
   * identify if any filter by host-component is active
   * if so run @getHostByHostComponents
   *
   * @param callback
   * @return {Boolean}
   */
  preLoadHosts: function (callback) {
    var preLoadKeys = this.get('hostsPreLoadKeys');

    if (this.get('queryParams.Hosts').length > 0 && this.get('queryParams.Hosts').filter(function (param) {
      return (preLoadKeys.contains(param.key));
    }, this).length > 0) {
      this.getHostByHostComponents(callback);
      return true;
    }
    return false;
  },

  /**
   * get hosts' names which match filter by host-component
   * @param callback
   */
  getHostByHostComponents: function (callback) {
    var testUrl = App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hosts.json' : '/data/hosts/hosts.json';
    var realUrl = '/hosts?<parameters>minimal_response=true';

    App.ajax.send({
      name: 'hosts.host_components.pre_load',
      sender: this,
      data: {
        url: this.getComplexUrl(testUrl, realUrl, this.get('queryParams.Hosts')),
        callback: callback
      },
      success: 'getHostByHostComponentsSuccessCallback',
      error: 'getHostByHostComponentsErrorCallback'
    })
  },
  getHostByHostComponentsSuccessCallback: function (data, opt, params) {
    var preLoadKeys = this.get('hostsPreLoadKeys');
    var queryParams = this.get('queryParams.Hosts');
    var hostNames = data.items.mapProperty('Hosts.host_name');
    var skipCall = hostNames.length === 0;

    /**
     * exclude pagination parameters as they were applied in previous call
     * to obtain hostnames of filtered hosts
     */
    preLoadKeys = preLoadKeys.concat(this.get('paginationKeys'));

    var itemTotal = parseInt(data.itemTotal);
    if (!isNaN(itemTotal)) {
      App.router.set('mainHostController.filteredCount', itemTotal);
    }

    if (skipCall) {
      params.callback(skipCall);
    } else {
      queryParams = queryParams.filter(function (param) {
        return !(preLoadKeys.contains(param.key));
      });

      queryParams.push({
        key: 'Hosts/host_name',
        value: hostNames,
        type: 'MULTIPLE'
      });
      params.callback(skipCall, queryParams);
    }
  },
  getHostByHostComponentsErrorCallback: function () {
    console.warn('ERROR: filtering hosts by host-component failed');
  },
  graphs: [],
  graphsUpdate: function (callback) {
    var existedGraphs = [];
    this.get('graphs').forEach(function (_graph) {
      var view = Em.View.views[_graph.id];
      if (view) {
        existedGraphs.push(_graph);
        //console.log('updated graph', _graph.name);
        view.loadData();
        //if graph opened as modal popup update it to
        if ($(".modal-graph-line .modal-body #" + _graph.popupId + "-container-popup").length) {
          view.loadData();
        }
      }
    });
    callback();
    this.set('graphs', existedGraphs);
  },

  /**
   * Updates the services information.
   *
   * @param callback
   */
  updateServiceMetric: function (callback) {
    var self = this;
    self.set('isUpdated', false);
    var isATSPresent = App.StackServiceComponent.find().findProperty('componentName','APP_TIMELINE_SERVER');

    var conditionalFields = this.getConditionalFields(),
      conditionalFieldsString = conditionalFields.length > 0 ? ',' + conditionalFields.join(',') : '',
      testUrl = App.get('isHadoop2Stack') ? '/data/dashboard/HDP2/master_components.json' : '/data/dashboard/services.json',
      isFlumeInstalled = App.cache['services'].mapProperty('ServiceInfo.service_name').contains('FLUME'),
      isATSInstalled = App.cache['services'].mapProperty('ServiceInfo.service_name').contains('YARN') && isATSPresent,
      flumeHandlerParam = isFlumeInstalled ? 'ServiceComponentInfo/component_name=FLUME_HANDLER|' : '',
      atsHandlerParam = isATSInstalled ? 'ServiceComponentInfo/component_name=APP_TIMELINE_SERVER|' : '',
      haComponents = App.get('isHaEnabled') ? 'ServiceComponentInfo/component_name=JOURNALNODE|ServiceComponentInfo/component_name=ZKFC|' : '',
      realUrl = '/components/?' + flumeHandlerParam + atsHandlerParam + haComponents +
        'ServiceComponentInfo/category=MASTER&fields=' +
        'ServiceComponentInfo/Version,' +
        'ServiceComponentInfo/StartTime,' +
        'ServiceComponentInfo/HeapMemoryUsed,' +
        'ServiceComponentInfo/HeapMemoryMax,' +
        'ServiceComponentInfo/service_name,' +
        'host_components/HostRoles/host_name,' +
        'host_components/HostRoles/state,' +
        'host_components/HostRoles/maintenance_state,' +
        'host_components/HostRoles/stale_configs,' +
        'host_components/HostRoles/ha_state,' +
        'host_components/metrics/jvm/memHeapUsedM,' +
        'host_components/metrics/jvm/HeapMemoryMax,' +
        'host_components/metrics/jvm/HeapMemoryUsed,' +
        'host_components/metrics/jvm/memHeapCommittedM,' +
        'host_components/metrics/mapred/jobtracker/trackers_decommissioned,' +
        'host_components/metrics/cpu/cpu_wio,' +
        'host_components/metrics/rpc/RpcQueueTime_avg_time,' +
        'host_components/metrics/dfs/FSNamesystem/*,' +
        'host_components/metrics/dfs/namenode/Version,' +
        'host_components/metrics/dfs/namenode/DecomNodes,' +
        'host_components/metrics/dfs/namenode/TotalFiles,' +
        'host_components/metrics/dfs/namenode/UpgradeFinalized,' +
        'host_components/metrics/dfs/namenode/Safemode,' +
        'host_components/metrics/runtime/StartTime' +
        conditionalFieldsString +
        '&minimal_response=true';

    var servicesUrl = this.getUrl(testUrl, realUrl);
    callback = callback || function () {
      self.set('isUpdated', true);
    };
    App.HttpClient.get(servicesUrl, App.serviceMetricsMapper, {
      complete: function () {
        callback();
      }
    });
  },
  /**
   * construct conditional parameters of query, depending on which services are installed
   * @return {Array}
   */
  getConditionalFields: function () {
    var conditionalFields = [];
    var serviceSpecificParams = {
      'FLUME': "host_components/metrics/flume/flume," +
        "host_components/processes/HostComponentProcess",
      'YARN': "host_components/metrics/yarn/Queue," +
        "ServiceComponentInfo/rm_metrics/cluster/activeNMcount," +
        "ServiceComponentInfo/rm_metrics/cluster/unhealthyNMcount," +
        "ServiceComponentInfo/rm_metrics/cluster/rebootedNMcount," +
        "ServiceComponentInfo/rm_metrics/cluster/decommissionedNMcount",
      'HBASE': "host_components/metrics/hbase/master/IsActiveMaster," +
        "ServiceComponentInfo/MasterStartTime," +
        "ServiceComponentInfo/MasterActiveTime," +
        "ServiceComponentInfo/AverageLoad," +
        "ServiceComponentInfo/Revision," +
        "ServiceComponentInfo/RegionsInTransition",
      'MAPREDUCE': "ServiceComponentInfo/AliveNodes," +
        "ServiceComponentInfo/GrayListedNodes," +
        "ServiceComponentInfo/BlackListedNodes," +
        "ServiceComponentInfo/jobtracker/*,",
      'STORM': /^2.1/.test(App.get('currentStackVersionNumber')) ? 'metrics/api/cluster/summary' : 'metrics/api/v1/cluster/summary,metrics/api/v1/topology/summary'
    };
    var services = App.cache['services'];
    services.forEach(function (service) {
      var urlParams = serviceSpecificParams[service.ServiceInfo.service_name];
      if (urlParams) {
        conditionalFields.push(urlParams);
      }
    });
    return conditionalFields;
  },
  updateServices: function (callback) {
    var testUrl = '/data/services/HDP2/services.json';
    var componentConfigUrl = this.getUrl(testUrl, '/services?fields=alerts/summary,ServiceInfo/state,ServiceInfo/maintenance_state&minimal_response=true');
    App.HttpClient.get(componentConfigUrl, App.serviceMapper, {
      complete: callback
    });
  },
  updateComponentConfig: function (callback) {
    var testUrl = '/data/services/host_component_stale_configs.json';
    var componentConfigUrl = this.getUrl(testUrl, '/components?ServiceComponentInfo/category.in(SLAVE,CLIENT)&host_components/HostRoles/stale_configs=true&fields=host_components/HostRoles/service_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/host_name,host_components/HostRoles/stale_configs&minimal_response=true');
    App.HttpClient.get(componentConfigUrl, App.componentConfigMapper, {
      complete: callback
    });
  },
  updateComponentsState: function (callback) {
    var testUrl = '/data/services/HDP2/components_state.json';
    var realUrl = '/components/?ServiceComponentInfo/category.in(SLAVE,CLIENT)&fields=ServiceComponentInfo/service_name,' +
      'ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/total_count&minimal_response=true';
    var url = this.getUrl(testUrl, realUrl);

    App.HttpClient.get(url, App.componentsStateMapper, {
      complete: callback
    });
  }
});
