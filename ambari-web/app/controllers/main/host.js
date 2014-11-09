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
var validator = require('utils/validator');
var batchUtils = require('utils/batch_scheduled_requests');

App.MainHostController = Em.ArrayController.extend({
  name: 'mainHostController',

  dataSource: App.Host.find(),
  clearFilters: null,

  filteredCount: 0,
  resetStartIndex: false,
  /**
   * flag responsible for updating status counters of hosts
   */
  isCountersUpdating: false,

  hostsCountMap: {},

  startIndex: 1,

  /**
   * Components which will be shown in component filter
   * @returns {Array}
   */
  componentsForFilter: function () {
    var installedComponents = App.StackServiceComponent.find().toArray();
    installedComponents.setEach('checkedForHostFilter', false);
    return installedComponents;
  }.property('App.router.clusterController.isLoaded'),

  content: function () {
    return this.get('dataSource').filterProperty('isRequested');
  }.property('dataSource.@each.isRequested'),

  /**
   * filterProperties support follow types of filter:
   * MATCH - match of RegExp
   * EQUAL - equality "="
   * LESS - "<"
   * MORE - ">"
   * MULTIPLE - multiple values to compare
   * CUSTOM - substitute values with keys "{#}" in alias
   */
  filterProperties: [
    {
      key: 'publicHostName',
      alias: 'Hosts/public_host_name',
      type: 'MATCH'
    },
    {
      key: 'ip',
      alias: 'Hosts/ip',
      type: 'MATCH'
    },
    {
      key: 'cpu',
      alias: 'Hosts/cpu_count',
      type: 'EQUAL'
    },
    {
      key: 'memoryFormatted',
      alias: 'Hosts/total_mem',
      type: 'EQUAL'
    },
    {
      key: 'loadAvg',
      alias: 'metrics/load/load_one',
      type: 'EQUAL'
    },
    {
      key: 'hostComponents',
      alias: 'host_components/HostRoles/component_name',
      type: 'MULTIPLE'
    },
    {
      key: 'healthClass',
      alias: 'Hosts/host_status',
      type: 'EQUAL'
    },
    {
      key: 'criticalAlertsCount',
      alias: 'alerts/summary/CRITICAL{0}|alerts/summary/WARNING{1}',
      type: 'CUSTOM'
    },
    {
      key: 'componentsWithStaleConfigsCount',
      alias: 'host_components/HostRoles/stale_configs',
      type: 'EQUAL'
    },
    {
      key: 'componentsInPassiveStateCount',
      alias: 'host_components/HostRoles/maintenance_state',
      type: 'MULTIPLE'
    },
    {
      key: 'selected',
      alias: 'Hosts/host_name',
      type: 'MULTIPLE'
    }
  ],

  viewProperties: [
    Em.Object.create({
      key: 'displayLength',
      getValue: function (controller) {
        var name = controller.get('name');
        var dbValue = App.db.getDisplayLength(name);
        if (Em.isNone(this.get('viewValue'))) {
          if (dbValue) {
            this.set('viewValue', dbValue);
          } else {
            this.set('viewValue', '25'); //25 is default displayLength value for hosts page
            App.db.setDisplayLength(name, '25');
          }
        }
        return this.get('viewValue');
      },
      viewValue: null,
      alias: 'page_size'
    }),
    Em.Object.create({
      key: 'startIndex',
      getValue: function (controller) {
        var name = controller.get('name');
        var startIndex = App.db.getStartIndex(name);
        var value = this.get('viewValue');

        if (Em.isNone(value)) {
          if (Em.isNone(startIndex)) {
            value = 0;
          } else {
            value = startIndex;
            App.db.setStartIndex(name, startIndex);
          }
        }
        return (value > 0) ? value - 1 : value;
      },
      viewValue: null,
      alias: 'from'
    })
  ],

  sortProps: [
    {
      key: 'publicHostName',
      alias: 'Hosts/public_host_name'
    },
    {
      key: 'ip',
      alias: 'Hosts/ip'
    },
    {
      key: 'cpu',
      alias: 'Hosts/cpu_count'
    },
    {
      key: 'memoryFormatted',
      alias: 'Hosts/total_mem'
    },
    {
      key: 'diskUsage',
      //TODO disk_usage is relative property and need support from API, metrics/disk/disk_free used temporarily
      alias: 'metrics/disk/disk_free'
    },
    {
      key: 'loadAvg',
      alias: 'metrics/load/load_one'
    }
  ],

  /**
   * Validate and convert input string to valid url parameter.
   * Detect if user have passed string as regular expression or extend
   * string to regexp.
   *
   * @param {String} value
   * @return {String}
   **/
  getRegExp: function (value) {
    value = validator.isValidMatchesRegexp(value) ? value.replace(/(\.+\*?|(\.\*)+)$/, '') + '.*' : '^$';
    value = /^\.\*/.test(value) || value == '^$' ? value : '.*' + value;
    return value;
  },

  /**
   * Transform <code>viewProperties</code> to queryParameters
   * @returns {Object[]}
   * @method getViewProperties
   */
  getViewProperties: function() {
    return this.get('viewProperties').map(function (property) {
      return {
        key: property.get('alias'),
        value: property.getValue(this),
        type: 'EQUAL'
      };
    }, this);
  },

  /**
   * Transform <code>sortProps</code> to queryParameters
   * @returns {Object[]}
   * @method getSortProperties
   */
  getSortProperties: function() {
    var savedSortConditions = App.db.getSortingStatuses(this.get('name')) || [],
      sortProperties = this.get('sortProps'),
      queryParams = [];
    savedSortConditions.forEach(function (sort) {
      var property = sortProperties.findProperty('key', sort.name);

      if (property && (sort.status === 'sorting_asc' || sort.status === 'sorting_desc')) {
        queryParams.push({
          key: property.alias,
          value: sort.status.replace('sorting_', ''),
          type: 'SORT'
        });
      }
    });
    return queryParams;
  },

  /**
   * get query parameters computed from filter properties, sort properties and custom properties of view
   * @return {Array}
   * @method getQueryParameters
   */
  getQueryParameters: function (skipNonFilterProperties) {
    skipNonFilterProperties = skipNonFilterProperties || false;
    var queryParams = [],
      savedFilterConditions = App.db.getFilterConditions(this.get('name')) || [],
      savedSortConditions = App.db.getSortingStatuses(this.get('name')) || [],
      colPropAssoc = this.get('colPropAssoc'),
      filterProperties = this.get('filterProperties'),
      sortProperties = this.get('sortProps'),
      oldProperties = App.router.get('updateController.queryParams.Hosts');

    this.set('resetStartIndex', false);

    queryParams.pushObjects(this.getViewProperties());

    savedFilterConditions.forEach(function (filter) {
      var property = filterProperties.findProperty('key', colPropAssoc[filter.iColumn]);
      if (property && filter.value.length > 0 && !filter.skipFilter) {
        var result = {
          key: property.alias,
          value: filter.value,
          type: property.type,
          isFilter: true
        };
        if (filter.type === 'string' && sortProperties.someProperty('key', colPropAssoc[filter.iColumn])) {
          result.value = this.getRegExp(filter.value);
        }
        if (filter.type === 'number' || filter.type === 'ambari-bandwidth') {
          result.type = this.getComparisonType(filter.value);
          result.value = this.getProperValue(filter.value);
        }
        // enter an exact number for RAM filter, need to do a range number match for this
        if (filter.type === 'ambari-bandwidth' && result.type == 'EQUAL' && result.value) {
          var valuePair = this.convertMemoryToRange(filter.value);
          queryParams.push({
            key: result.key,
            value: valuePair[0],
            type: 'MORE'
          });
          queryParams.push({
            key: result.key,
            value: valuePair[1],
            type: 'LESS'
          });
        } else if (filter.type === 'ambari-bandwidth' && result.type != 'EQUAL' && result.value){
          // enter a comparison type, eg > 1, just do regular match
          result.value = this.convertMemory(filter.value);
          queryParams.push(result);
        } else if (result.value) {
          queryParams.push(result);
        }

      }
    }, this);

    if (queryParams.filterProperty('isFilter').length !== oldProperties.filterProperty('isFilter').length) {
      queryParams.findProperty('key', 'from').value = 0;
      this.set('resetStartIndex', true);
    } else {
      queryParams.filterProperty('isFilter').forEach(function (queryParam) {
        var oldProperty = oldProperties.filterProperty('isFilter').findProperty('key', queryParam.key);
        if (!oldProperty || JSON.stringify(oldProperty.value) !== JSON.stringify(queryParam.value)) {
          queryParams.findProperty('key', 'from').value = 0;
          this.set('resetStartIndex', true);
        }
      }, this);
    }

    if (!skipNonFilterProperties) {
      queryParams.pushObjects(this.getSortProperties());
    }

    return queryParams;
  },

  /**
   * update status counters of hosts
   */
  updateStatusCounters: function () {
    var self = this;

    if (this.get('isCountersUpdating')) {
      App.ajax.send({
        name: 'host.status.counters',
        sender: this,
        data: {},
        success: 'updateStatusCountersSuccessCallback',
        error: 'updateStatusCountersErrorCallback'
      });

      setTimeout(function () {
        self.updateStatusCounters();
      }, App.get('componentsUpdateInterval'));
    }
  },

  /**
   * success callback on <code>updateStatusCounters()</code>
   * map counters' value to categories
   * @param data
   */
  updateStatusCountersSuccessCallback: function (data) {
    var hostsCountMap = {
      'HEALTHY': data.Clusters.health_report['Host/host_status/HEALTHY'],
      'UNHEALTHY': data.Clusters.health_report['Host/host_status/UNHEALTHY'],
      'ALERT': data.Clusters.health_report['Host/host_status/ALERT'],
      'UNKNOWN': data.Clusters.health_report['Host/host_status/UNKNOWN'],
      'health-status-WITH-ALERTS': (data.alerts) ? data.alerts.summary.CRITICAL + data.alerts.summary.WARNING : 0,
      'health-status-RESTART': data.Clusters.health_report['Host/stale_config'],
      'health-status-PASSIVE_STATE': data.Clusters.health_report['Host/maintenance_state'],
      'TOTAL': data.Clusters.total_hosts
    };

    this.set('hostsCountMap', hostsCountMap);
  },

  /**
   * success callback on <code>updateStatusCounters()</code>
   */
  updateStatusCountersErrorCallback: function() {
    console.warn('ERROR: updateStatusCounters failed')
  },

  /**
   * Return value without predicate
   * @param {String} value
   * @return {String}
   */
  getProperValue: function (value) {
    return (value.charAt(0) === '>' || value.charAt(0) === '<' || value.charAt(0) === '=') ? value.substr(1, value.length) : value;
  },

  /**
   * Return value converted to kilobytes
   * @param {String} value
   * @return {*}
   */
  convertMemory: function (value) {
    var scale = value.charAt(value.length - 1);
    // first char may be predicate for comparison
    value = this.getProperValue(value);
    var parsedValue = parseFloat(value);

    if (isNaN(parsedValue)) {
      return value;
    }

    switch (scale) {
      case 'g':
        parsedValue *= 1048576;
        break;
      case 'm':
        parsedValue *= 1024;
        break;
      case 'k':
        break;
      default:
        //default value in GB
        parsedValue *= 1048576;
    }
    return Math.round(parsedValue);
  },

  /**
   * Return value converted to a range of kilobytes
   * @param {String} value
   * @return {Array}
   */
  convertMemoryToRange: function (value) {
    var scale = value.charAt(value.length - 1);
    // first char may be predicate for comparison
    value = this.getProperValue(value);
    var parsedValue = parseFloat(value);
    if (isNaN(parsedValue)) {
      return value;
    }
    var parsedValuePair = this.rangeConvertNumber(parsedValue, scale);
    var multiplyingFactor = 1;
    switch (scale) {
      case 'g':
        multiplyingFactor = 1048576;
        break;
      case 'm':
        multiplyingFactor = 1024;
        break;
      case 'k':
        break;
      default:
        //default value in GB
        multiplyingFactor = 1048576;
    }
    parsedValuePair[0]  = Math.round( parsedValuePair[0] * multiplyingFactor);
    parsedValuePair[1]  = Math.round( parsedValuePair[1] * multiplyingFactor);
    return parsedValuePair;
  },

  /**
   * Return value converted to a range of kilobytes
   * eg, return value 1.83 g will target 1.82500 ~ 1.83499 g
   * eg, return value 1.8 k will target 1.7500 ~ 1.8499 k
   * eg, return value 1.8 m will target 1.7500 ~ 1.8499 m
   * @param {number} value
   * @param {String} scale
   * @return {Array}
   */
  rangeConvertNumber: function (value, scale) {
    if (isNaN(value)) {
      return value;
    }
    var valuePair = [];
    switch (scale) {
      case 'g':
        valuePair = [value - 0.005000, value + 0.004999999];
        break;
      case 'm':
      case 'k':
        valuePair = [value - 0.05000, value + 0.04999];
        break;
      default:
        //default value in GB
        valuePair = [value - 0.005000, value + 0.004999999];
    }
    return valuePair;
  },

  /**
   * Return comparison type depending on populated predicate
   * @param value
   * @return {String}
   */
  getComparisonType: function (value) {
    var comparisonChar = value.charAt(0);
    var result = 'EQUAL';
    if (isNaN(comparisonChar)) {
      switch (comparisonChar) {
        case '>':
          result = 'MORE';
          break;
        case '<':
          result = 'LESS';
          break;
      }
    }
    return result;
  },

  /**
   * Filter hosts by componentName of <code>component</code>
   * @param {App.HostComponent} component
   */
  filterByComponent: function (component) {
    if (!component)
      return;
    var id = component.get('componentName');
    var column = 6;
    this.get('componentsForFilter').setEach('checkedForHostFilter', false);

    var filterForComponent = {
      iColumn: column,
      value: [id],
      type: 'multiple'
    };
    App.db.setFilterConditions(this.get('name'), [filterForComponent]);
  },

  showAlertsPopup: function (event) {
    var host = event.context;
    App.router.get('mainAlertsController').loadAlerts(host.get('hostName'), "HOST");
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/host/alerts_popup'),
        controllerBinding: 'App.router.mainAlertsController',
        alerts: function () {
          return this.get('controller.alerts');
        }.property('controller.alerts'),

        closePopup: function () {
          this.get('parentView').hide();
        }
      }),
      primary: Em.I18n.t('common.close'),
      secondary: null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
        this.$().children('.modal').css({'margin-top': '-350px'});
      }
    });
    event.stopPropagation();
  },

  /**
   * remove selected hosts
   */
  removeHosts: function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.hostName);
    });
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * remove hosts with id equal host_id
   * @param {String} host_id
   */
  checkRemoved: function (host_id) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('id', host_id);
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * Bulk operation wrapper
   * @param {Object} operationData - data about bulk operation (action, hosts or hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperation: function (operationData, hosts) {
    if (operationData.componentNameFormatted) {
      if (operationData.action === 'RESTART') {
        this.bulkOperationForHostComponentsRestart(operationData, hosts);
      }
      else {
        if (operationData.action.indexOf('DECOMMISSION') != -1) {
          this.bulkOperationForHostComponentsDecommission(operationData, hosts);
        }
        else {
          this.bulkOperationForHostComponents(operationData, hosts);
        }
      }
    }
    else {
      if (operationData.action === 'RESTART') {
        this.bulkOperationForHostsRestart(operationData, hosts);
      }
      else {
        if (operationData.action === 'PASSIVE_STATE') {
          this.bulkOperationForHostsPassiveState(operationData, hosts);
        }
        else {
          this.bulkOperationForHosts(operationData, hosts);
        }
      }
    }
  },

  /**
   * Bulk operation (start/stop all) for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHosts: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/component_name']
    }, function (data) {
      self.bulkOperationForHostsCallback(operationData, data);
    });
  },
  /**
   * run Bulk operation (start/stop all) for selected hosts
   * after host and components are loaded
   * @param operationData
   * @param data
   */
  bulkOperationForHostsCallback: function (operationData, data) {
    var query = [];
    var hostNames = [];
    var hostsMap = {};

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        if (!App.components.get('clients').contains((hostComponent.HostRoles.component_name))) {
          if (hostsMap[host.Hosts.host_name]) {
            hostsMap[host.Hosts.host_name].push(hostComponent.HostRoles.component_name);
          } else {
            hostsMap[host.Hosts.host_name] = [hostComponent.HostRoles.component_name];
          }
        }
      });
    });

    for (var hostName in hostsMap) {
      var subQuery = '(HostRoles/component_name.in(%@)&HostRoles/host_name=' + hostName + ')';
      var components = hostsMap[hostName];
      if (components.length) {
        query.push(subQuery.fmt(components.join(',')));
      }
      hostNames.push(hostName);
    }

    hostNames = hostNames.join(",");
    if (query.length) {
      query = query.join('|');
      App.ajax.send({
        name: 'common.host_components.update',
        sender: this,
        data: {
          query: query,
          HostRoles: {
            state: operationData.action
          },
          context: operationData.message,
          hostName: hostNames
        },
        success: 'bulkOperationForHostComponentsSuccessCallback'
      });
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
  },

  /**
   * Bulk restart for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  bulkOperationForHostsRestart: function (operationData, hosts) {
    batchUtils.getComponentsFromServer({
      passiveState: 'OFF',
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['host_components/HostRoles/component_name']
    }, function (data) {
      var hostComponents = [];
      data.items.forEach(function (host) {
        host.host_components.forEach(function (hostComponent) {
          hostComponents.push(Em.Object.create({
            componentName: hostComponent.HostRoles.component_name,
            hostName: host.Hosts.host_name
          }));
        })
      });
      batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
    });
  },

  /**
   * Bulk turn on/off passive state for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostsPassiveState: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['Hosts/maintenance_state']
    }, function (data) {
      var hostNames = [];

      data.items.forEach(function (host) {
        if (host.Hosts.maintenance_state !== operationData.state) {
          hostNames.push(host.Hosts.host_name);
        }
      });
      if (hostNames.length) {
        App.ajax.send({
          name: 'bulk_request.hosts.passive_state',
          sender: self,
          data: {
            hostNames: hostNames.join(','),
            passive_state: operationData.state,
            requestInfo: operationData.message
          },
          success: 'updateHostPassiveState'
        });
      } else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('hosts.bulkOperation.passiveState.nothingToDo.body'),
          secondary: false
        });
      }
    });
  },

  updateHostPassiveState: function (data, opt, params) {
    batchUtils.infoPassiveState(params.passive_state);
  },
  /**
   * Bulk operation for selected hostComponents
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponents: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF'
    }, function (data) {
      if (data.items.length) {
        var hostsWithComponentInProperState = data.items.mapProperty('Hosts.host_name');
        App.ajax.send({
          name: 'common.host_components.update',
          sender: self,
          data: {
            HostRoles: {
              state: operationData.action
            },
            query: 'HostRoles/component_name=' + operationData.componentName + '&HostRoles/host_name.in(' + hostsWithComponentInProperState.join(',') + ')&HostRoles/maintenance_state=OFF',
            context: operationData.message + ' ' + operationData.componentNameFormatted
          },
          success: 'bulkOperationForHostComponentsSuccessCallback'
        });
      }
      else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
          secondary: false
        });
      }
    });
  },

  /**
   * Bulk decommission/recommission for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsDecommission: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      components: [operationData.realComponentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/state']
    }, function (data) {
      self.bulkOperationForHostComponentsDecommissionCallBack(operationData, data)
    });
  },

  /**
   * run Bulk decommission/recommission for selected hostComponents
   * after host and components are loaded
   * @param operationData
   * @param data
   */
  bulkOperationForHostComponentsDecommissionCallBack: function (operationData, data) {
    var service = App.Service.find(operationData.serviceName);
    var components = [];

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        components.push(Em.Object.create({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name,
          workStatus: hostComponent.HostRoles.state
        }))
      });
    });

    if (components.length) {
      var hostsWithComponentInProperState = components.mapProperty('hostName');
      var turn_off = operationData.action.indexOf('OFF') !== -1;
      var svcName = operationData.serviceName;
      var masterName = operationData.componentName;
      var slaveName = operationData.realComponentName;
      var hostNames = hostsWithComponentInProperState.join(',');
      if (turn_off) {
        // For recommession
        if (svcName === "YARN" || svcName === "HBASE" || svcName === "HDFS") {
          App.router.get('mainHostDetailsController').doRecommissionAndStart(hostNames, svcName, masterName, slaveName);
        }
        else if (svcName === "MAPREDUCE") {
          App.router.get('mainHostDetailsController').doRecommissionAndRestart(hostNames, svcName, masterName, slaveName);
        }
      } else {
        hostsWithComponentInProperState = components.filterProperty('workStatus', 'STARTED').mapProperty('hostName');
        //For decommession
        if (svcName == "HBASE") {
          // HBASE service, decommission RegionServer in batch requests
          this.warnBeforeDecommission(hostNames);
        } else {
          var parameters = {
            "slave_type": slaveName
          };
          var contextString = turn_off ? 'hosts.host.' + slaveName.toLowerCase() + '.recommission' :
              'hosts.host.' + slaveName.toLowerCase() + '.decommission';
          if (turn_off) {
            parameters['included_hosts'] = hostsWithComponentInProperState.join(',')
          }
          else {
            parameters['excluded_hosts'] = hostsWithComponentInProperState.join(',');
          }
          App.ajax.send({
            name: 'bulk_request.decommission',
            sender: this,
            data: {
              context: Em.I18n.t(contextString),
              serviceName: service.get('serviceName'),
              componentName: operationData.componentName,
              parameters: parameters
            },
            success: 'bulkOperationForHostComponentsSuccessCallback'
          });
        }
      }
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
        secondary: false
      });
    }
  },


  /**
   * get info about regionserver passive_state
   * @method warnBeforeDecommission
   * @param {String} hostNames
   * @return {$.ajax}
   */
  warnBeforeDecommission: function (hostNames) {
    return App.ajax.send({
      'name': 'host_components.hbase_regionserver.active',
      'sender': this,
      'data': {
        hostNames: hostNames
      },
      success: 'warnBeforeDecommissionSuccess'
    });
  },

  /**
   * check is hbase regionserver in mm. If so - run decommission
   * otherwise shows warning
   * @method warnBeforeDecommission
   * @param {Object} data
   * @param {Object} opt
   * @param {Object} params
   */
  warnBeforeDecommissionSuccess: function(data, opt, params) {
    if (Em.get(data, 'items.length')) {
      App.router.get('mainHostDetailsController').showHbaseActiveWarning();
    } else {
      App.router.get('mainHostDetailsController').checkRegionServerState(params.hostNames);
    }
  },
  /**
   * Bulk restart for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsRestart: function (operationData, hosts) {
    var service = App.Service.find(operationData.serviceName);

    batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['Hosts/maintenance_state', 'host_components/HostRoles/stale_configs', 'host_components/HostRoles/maintenance_state']
    }, function (data) {
      var wrappedHostComponents = [];

      data.items.forEach(function (host) {
        host.host_components.forEach(function (hostComponent) {
          wrappedHostComponents.push(Em.Object.create({
            componentName: hostComponent.HostRoles.component_name,
            serviceName: operationData.serviceName,
            hostName: host.Hosts.host_name,
            hostPassiveState: host.Hosts.maintenance_state,
            staleConfigs: hostComponent.HostRoles.stale_configs,
            passiveState: hostComponent.HostRoles.maintenance_state
          }))
        });
      });

      if (wrappedHostComponents.length) {
        batchUtils.showRollingRestartPopup(wrappedHostComponents.objectAt(0).get('componentName'), service.get('displayName'), service.get('passiveState') === "ON", false, wrappedHostComponents);
      } else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
          secondary: false
        });
      }
    });
  },

  updateHostComponentsPassiveState: function (data, opt, params) {
    batchUtils.infoPassiveState(params.passive_state);
  },
  /**
   * Show BO popup after bulk request
   */
  bulkOperationForHostComponentsSuccessCallback: function () {
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },
  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[0] = 'healthClass';
    associations[1] = 'publicHostName';
    associations[2] = 'ip';
    associations[3] = 'cpu';
    associations[4] = 'memoryFormatted';
    associations[5] = 'loadAvg';
    associations[6] = 'hostComponents';
    associations[7] = 'criticalAlertsCount';
    associations[8] = 'componentsWithStaleConfigsCount';
    associations[9] = 'componentsInPassiveStateCount';
    associations[10] = 'selected';
    return associations;
  }.property()

});
