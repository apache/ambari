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

App.SliderAppsController = Ember.ArrayController.extend({

  /**
   *  Load resources on controller initialization
   * @method initResources
   */
  initResources:function () {
    this.getClusterName();
  },

  initialValuesToLoad: Em.Object.create({
    ambariAddress: null,
    clusterName: null,
    hdfsAddress: null,
    yarnRMAddress: null,
    yarnRMSchedulerAddress: null,
    zookeeperQuorum: null
  }),

  zookeeperHosts: [],

  /**
   * Get cluster name from server
   * @returns {$.ajax}
   * @method getClusterName
   */
  getClusterName: function() {
    return App.ajax.send({
      name: 'cluster_name',
      sender: this,
      data: {
        urlPrefix: '/api/v1/'
      },
      success: 'getClusterNameSuccessCallback'
    });
  },

  /**
   * Success callback for clusterName-request
   * @param {object} data
   * @method getClusterNameSuccessCallback
   */
  getClusterNameSuccessCallback: function(data) {
    var clusterName = Em.get(data.items[0], 'Clusters.cluster_name');
    App.set('clusterName', clusterName);
    App.ApplicationStatusMapper.loop('load');
    this.loadConfigsTags();
    this.loadComponentHost({componentName:"GANGLIA_SERVER",callback:"loadGangliaHostSuccessCallback"});
    this.loadComponentHost({componentName:"NAGIOS_SERVER",callback:"loadNagiosHostSuccessCallback"});
    this.loadComponentHost({componentName:"ZOOKEEPER_SERVER",callback:"setZookeeperQuorum"});
  },

  loadConfigsTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        urlPrefix: '/api/v1/'
      },
      success: 'onLoadConfigsTags'
    });
  },

  onLoadConfigsTags: function (data) {
    var urlParams = [];
    if(data.Clusters.desired_configs['yarn-site'] && data.Clusters.desired_configs['zookeeper-env']){
      var coreSiteTag = data.Clusters.desired_configs['core-site'].tag;
      var yarnSiteTag = data.Clusters.desired_configs['yarn-site'].tag;
      var zookeeperTag = data.Clusters.desired_configs['zookeeper-env'].tag;
      urlParams.push('(type=core-site&tag=' + coreSiteTag + ')');
      urlParams.push('(type=yarn-site&tag=' + yarnSiteTag + ')');
      urlParams.push('(type=zookeeper-env&tag=' + zookeeperTag + ')');

      App.ajax.send({
        name: 'get_all_configurations',
        sender: this,
        data: {
          urlParams: urlParams.join('|'),
          urlPrefix: '/api/v1/'
        },
        success: 'onLoadConfigs'
      });
    }
  },

  onLoadConfigs: function (data) {
    var hdfs = data.items.findProperty('type', 'core-site'),
    yarn = data.items.findProperty('type', 'yarn-site'),
    zookeeper = data.items.findProperty('type', 'zookeeper-env'),
    initialValuesToLoad = this.get('initialValuesToLoad');
    initialValuesToLoad.set('ambariAddress', location.protocol+"//"+document.location.host);
    initialValuesToLoad.set('clusterName', App.get('clusterName'));
    initialValuesToLoad.set('hdfsAddress', hdfs.properties['fs.defaultFS']);
    initialValuesToLoad.set('yarnRMAddress', yarn.properties['yarn.resourcemanager.address']);
    initialValuesToLoad.set('yarnRMSchedulerAddress', yarn.properties['yarn.resourcemanager.scheduler.address']);
    initialValuesToLoad.set('zookeeperQuorum', zookeeper.properties.clientPort);
    this.setZookeeperQuorum();
  },

  setZookeeperQuorum: function (data){
    var zookeeperHosts = this.get('zookeeperHosts'),
    hosts = [],
    initialValuesToLoad = this.get('initialValuesToLoad');

    //done
    if(initialValuesToLoad.zookeeperQuorum !== null){
      if(data){
        hosts = data.items.map(function(item) {
          return item.Hosts.host_name + ":" + initialValuesToLoad.zookeeperQuorum;
        });
        initialValuesToLoad.set('zookeeperQuorum', hosts.join(','));
        this.sendInitialValues();
      }else if(zookeeperHosts.length > 0){
        hosts = zookeeperHosts.map(function(host) {
          return host + ":" + initialValuesToLoad.zookeeperQuorum;
        });
        initialValuesToLoad.set('zookeeperQuorum', hosts.join(','));
        this.sendInitialValues();
      }
    }else{
      this.set('zookeeperHosts', data.items.mapProperty('Hosts.host_name'));
    }
  },

  /**
   * Send request to server to save initialValues
   * @return {$.ajax}
   * @method sendInitialValues
   */
  sendInitialValues: function () {
    return App.ajax.send({
      name: 'saveInitialValues',
      sender: this,
      data: {
        data:  {
          ViewInstanceInfo: {
            properties: this.get('initialValuesToLoad')
          }
        }
      }
    });
  },

  /**
   * Load ganglia server host
   * @method loadGangliaHost
   */
  loadComponentHost: function (params) {
    return App.ajax.send({
      name: 'components_hosts',
      sender: this,
      data: {
        componentName: params.componentName,
        urlPrefix: '/api/v1/'
      },
      success: params.callback
    });

  },

  /**
   * Success callback for hosts-request
   * Save host name to gangliaHost
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadGangliaHostSuccessCallback: function (data) {
    if(data.items[0]){
      App.set('gangliaHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  },

  /**
   * Success callback for hosts-request
   * Save host name to nagiosHost
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadNagiosHostSuccessCallback: function (data) {
    if(data.items[0]){
      App.set('nagiosHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  }
});
