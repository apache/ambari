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
var misc = require('utils/misc');

App.MainMirroringController = Em.ArrayController.extend({
  name: 'mainMirroringController',

  datasetsData: [],

  // formatted data for targetClusterMapper
  clustersData: {},

  // counter for datasets load queries
  datasetCount: 0,

  // counter for target cluster load queries
  clusterCount: 0,

  selectedDataset: null,

  isDatasetsLoaded: false,

  isTargetClustersLoaded: false,

  isRequiredServicesStarted: false,

  isDatasetLoadingError: false,

  actionsDisabled: function () {
    return !this.get('isRequiredServicesStarted') || this.get('isDatasetLoadingError');
  }.property('isRequiredServicesStarted', 'isDatasetLoadingError'),

  isLoaded: function () {
    return this.get('isDatasetsLoaded') && this.get('isTargetClustersLoaded');
  }.property('isDatasetsLoaded', 'isTargetClustersLoaded'),

  datasets: App.Dataset.find(),

  loadData: function () {
    var isRequiredServicesStarted = App.Service.find().findProperty('serviceName', 'OOZIE').get('workStatus') == 'STARTED' && App.Service.find().findProperty('serviceName', 'FALCON').get('workStatus') == 'STARTED';
    this.set('isRequiredServicesStarted', isRequiredServicesStarted);
    if (isRequiredServicesStarted) {
      this.set('isDatasetLoadingError', false);
      this.get('datasetsData').clear();
      this.set('clustersData', {});
      this.set('datasetCount', 0);
      this.set('clusterCount', 0);
      this.loadDatasets();
      this.loadClusters();
    } else {
      this.set('isDatasetLoadingError', true);
    }
  },

  loadDatasets: function () {
    App.ajax.send({
      name: 'mirroring.get_all_entities',
      sender: this,
      data: {
        type: 'feed',
        falconServer: App.get('falconServerURL')
      },
      success: 'onLoadDatasetsListSuccess',
      error: 'onLoadDatasetsListError'
    });
  },

  onLoadDatasetsListSuccess: function (data) {
    var parsedData = misc.xmlToObject(data);
    var datasets = parsedData.entities.entity;
    if (datasets) {
      datasets = Em.isArray(datasets) ? datasets : [datasets];
      datasets = datasets.filter(function (dataset) {
        return dataset.name['#text'].indexOf(App.mirroringDatasetNamePrefix) === 0;
      });
    }
    if (datasets && datasets.length) {
      this.set('datasetCount', datasets.length);
      datasets.forEach(function (dataset) {
        App.ajax.send({
          name: 'mirroring.get_definition',
          sender: this,
          data: {
            name: dataset.name['#text'],
            type: 'feed',
            status: dataset.status['#text'],
            falconServer: App.get('falconServerURL')
          },
          success: 'onLoadDatasetDefinitionSuccess',
          error: 'onLoadDatasetDefinitionError'
        });
      }, this);
    } else {
      this.set('isDatasetsLoaded', true);
    }
  },

  onLoadDatasetsListError: function () {
    this.set('isDatasetLoadingError', true);
    console.error('Failed to load datasets list.');
  },

  onLoadDatasetDefinitionSuccess: function (data) {
    var parsedData = misc.xmlToObject(data);
    var clusters = parsedData.feed.clusters;
    var targetCluster, sourceCluster;

    if (clusters.cluster[0].locations) {
      targetCluster = clusters.cluster[0];
      sourceCluster = clusters.cluster[1];
    } else {
      targetCluster = clusters.cluster[1];
      sourceCluster = clusters.cluster[0];
    }
    var datasetName = parsedData.feed['@attributes'].name.replace(App.mirroringDatasetNamePrefix, '');
    this.get('datasetsData').push(
        Ember.Object.create({
          name: datasetName,
          status: arguments[2].status,
          sourceClusterName: sourceCluster['@attributes'].name,
          targetClusterName: targetCluster['@attributes'].name,
          sourceDir: parsedData.feed.locations.location['@attributes'].path,
          targetDir: targetCluster.locations.location['@attributes'].path,
          frequency: parsedData.feed.frequency['#text'].match(/\d+/)[0],
          frequencyUnit: parsedData.feed.frequency['#text'].match(/\w+(?=\()/)[0],
          scheduleEndDate: sourceCluster.validity['@attributes'].end,
          scheduleStartDate: sourceCluster.validity['@attributes'].start,
          instances: []
        })
    );
    var currentDate = new Date(App.dateTime());
    if (currentDate > new Date(sourceCluster.validity['@attributes'].start)) {
      App.ajax.send({
        name: 'mirroring.dataset.get_all_instances',
        sender: this,
        data: {
          dataset: parsedData.feed['@attributes'].name,
          formattedDatasetName: datasetName,
          start: sourceCluster.validity['@attributes'].start,
          end: App.router.get('mainMirroringEditDataSetController').toTZFormat(currentDate),
          falconServer: App.get('falconServerURL')
        },
        success: 'onLoadDatasetInstancesSuccess',
        error: 'onLoadDatasetsInstancesError'
      });
    } else {
      this.saveDataset();
    }
  },

  onLoadDatasetDefinitionError: function () {
    this.set('isDatasetLoadingError', true);
    console.error('Failed to load dataset definition.');
  },

  onLoadDatasetInstancesSuccess: function (data, sender, opts) {
    var datasetsData = this.get('datasetsData');
    if (data && data.instances) {
      var datasetJobs = [];
      data.instances.forEach(function (instance) {
        if (instance.cluster == App.get('clusterName')) {
          datasetJobs.push({
            dataset: opts.formattedDatasetName,
            id: instance.instance + '_' + opts.dataset,
            name: instance.instance,
            status: instance.status,
            endTime: new Date(instance.endTime).getTime() || 0,
            startTime: new Date(instance.startTime).getTime() || 0
          });
        }
      }, this);
      datasetsData.findProperty('name', opts.formattedDatasetName).set('instances', datasetJobs);
    }
    this.saveDataset();
  },

  saveDataset: function () {
    this.set('datasetCount', this.get('datasetCount') - 1);
    if (this.get('datasetCount') < 1) {
      App.dataSetMapper.map(this.get('datasetsData'));
      this.set('isDatasetsLoaded', true);
      if (App.router.get('currentState.name') === 'index' && App.router.get('currentState.parentState.name') === 'mirroring') {
        App.router.send('gotoShowJobs');
      }
    }
  },

  onLoadDatasetsInstancesError: function () {
    console.error('Failed to load dataset instances.');
    this.saveDataset();
  },

  loadClusters: function () {
    App.ajax.send({
      name: 'mirroring.get_all_entities',
      sender: this,
      data: {
        type: 'cluster',
        falconServer: App.get('falconServerURL')
      },
      success: 'onLoadClustersListSuccess',
      error: 'onLoadClustersListError'
    });
  },

  onLoadClustersListSuccess: function (data) {
    var clustersData = this.get('clustersData');
    clustersData.items = [];
    var parsedData = misc.xmlToObject(data);
    var clusters = parsedData.entities.entity;
    if (data && clusters) {
      clusters = Em.isArray(clusters) ? clusters : [clusters];
      this.set('clusterCount', clusters.length);
      clusters.mapProperty('name.#text').forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.get_definition',
          sender: this,
          data: {
            name: cluster,
            type: 'cluster',
            falconServer: App.get('falconServerURL')
          },
          success: 'onLoadClusterDefinitionSuccess',
          error: 'onLoadClusterDefinitionError'
        });
      }, this);
    } else {
      this.loadDefaultFS(function (defaultFS) {
        var clusterName = App.get('clusterName');
        var sourceCluster = Ember.Object.create({
          name: clusterName,
          execute: App.HostComponent.find().findProperty('componentName', 'RESOURCEMANAGER').get('hostName') + ':8050',
          readonly: 'hftp://' + App.HostComponent.find().findProperty('componentName', 'NAMENODE').get('hostName') + ':50070',
          workflow: 'http://' + App.HostComponent.find().findProperty('componentName', 'OOZIE_SERVER').get('hostName') + ':11000/oozie',
          write: defaultFS,
          staging: '/apps/falcon/' + clusterName + '/staging',
          working: '/apps/falcon/' + clusterName + '/working',
          temp: '/tmp'
        });
        var sourceClusterData = App.router.get('mainMirroringManageClustersController').formatClusterXML(sourceCluster);
        App.ajax.send({
          name: 'mirroring.submit_entity',
          sender: this,
          data: {
            type: 'cluster',
            entity: sourceClusterData,
            falconServer: App.get('falconServerURL')
          },
          success: 'onSourceClusterCreateSuccess',
          error: 'onSourceClusterCreateError'
        });
        clustersData.items.push(sourceCluster);
      });
    }
  },

  /**
   * Return fs.defaultFS config property loaded from server
   * @return {String}
   */
  loadDefaultFS: function (callback) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        callback: callback
      },
      success: 'onLoadConfigTagsSuccess',
      error: 'onLoadConfigTagsError'
    });
  },

  // Loaded core-site tag version
  tag: null,

  onLoadConfigTagsSuccess: function (data, opt, params) {
    this.set('tag', data.Clusters.desired_configs['core-site'].tag);
    App.router.get('configurationController').getConfigsByTags([
      {
        siteName: "core-site",
        tagName: this.get('tag')
      }
    ]).done(function (configs) {
        params.callback(configs[0].properties['fs.defaultFS']);
      });
  },

  onLoadConfigTagsError: function (request, ajaxOptions, error, opt, params) {
    console.error('Error in loading fs.defaultFS');
    params.callback(null);
  },

  onLoadClustersListError: function () {
    this.set('isDatasetLoadingError', true);
    console.error('Failed to load clusters list.');
  },

  onSourceClusterCreateSuccess: function () {
    App.targetClusterMapper.map(this.get('clustersData'));
    this.set('isTargetClustersLoaded', true);
  },

  onSourceClusterCreateError: function () {
    console.error('Error in creating source cluster entity.');
  },

  onLoadClusterDefinitionSuccess: function (data) {
    var parsedData = misc.xmlToObject(data);
    var clustersData = this.get('clustersData');
    var interfaces = parsedData.cluster.interfaces.interface;
    var locations = parsedData.cluster.locations.location;
    var staging = locations.findProperty('@attributes.name', 'staging');
    var working = locations.findProperty('@attributes.name', 'working');
    var temp = locations.findProperty('@attributes.name', 'temp');
    clustersData.items.push(
        {
          name: parsedData.cluster['@attributes'].name,
          execute: interfaces.findProperty('@attributes.type', 'execute')['@attributes'].endpoint,
          readonly: interfaces.findProperty('@attributes.type', 'readonly')['@attributes'].endpoint,
          workflow: interfaces.findProperty('@attributes.type', 'workflow')['@attributes'].endpoint,
          write: interfaces.findProperty('@attributes.type', 'write')['@attributes'].endpoint,
          staging: staging && staging['@attributes'].path,
          working: working && working['@attributes'].path,
          temp: temp && temp['@attributes'].path
        }
    );
    this.set('clusterCount', this.get('clusterCount') - 1);
    if (this.get('clusterCount') < 1) {
      App.targetClusterMapper.map(clustersData);
      this.set('isTargetClustersLoaded', true);
    }
  },

  onLoadClusterDefinitionError: function () {
    this.set('isDatasetLoadingError', true);
    console.error('Failed to load cluster definition.');
  },

  manageClusters: function () {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('mirroring.dataset.manageClusters'),
      classNames: ['sixty-percent-width-modal'],
      bodyClass: App.MainMirroringManageClusterstView.extend({
        controller: App.router.get('mainMirroringManageClustersController')
      }),
      primary: null,
      secondary: Em.I18n.t('common.close'),
      hide: function () {
        self.loadData();
        App.router.send('gotoShowJobs');
        this._super();
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
      }
    });
  }
});
