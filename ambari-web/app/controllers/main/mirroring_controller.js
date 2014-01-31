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

  datasetCount: 0,

  datasets: [],

  isLoaded: false,

  loadDatasets: function () {
    this.set('isLoaded', false);
    this.get('datasetsData').clear();
    this.set('datasetCount', 0);
    this.set('datasets', []);
    App.ajax.send({
      name: 'mirroring.get_all_datasets',
      sender: this,
      success: 'onLoadDatasetsListSuccess',
      error: 'onLoadDatasetsListError'
    });
  },

  onLoadDatasetsListSuccess: function (data) {
    if (data && data.entity) {
      this.set('datasetCount', data.entity.length);
      data.entity.mapProperty('name').forEach(function (dataset) {
        App.ajax.send({
          name: 'mirroring.get_dataset_definition',
          sender: this,
          data: {
            dataset: dataset
          },
          success: 'onLoadDatasetDefinitionSuccess',
          error: 'onLoadDatasetDefinitionError'
        });
      }, this);
    } else {
      this.onLoadDatasetsListError();
    }
  },

  onLoadDatasetsListError: function () {
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
    this.get('datasetsData').push(
        Ember.Object.create({
          name: parsedData.feed['@attributes'].name,
          sourceClusterName: sourceCluster['@attributes'].name,
          targetClusterName: targetCluster['@attributes'].name,
          sourceDir: parsedData.feed.locations.location['@attributes'].path,
          targetDir: targetCluster.locations.location['@attributes'].path,
          instances: []
        })
    );
    App.ajax.send({
      name: 'mirroring.dataset.get_all_instances',
      sender: this,
      data: {
        dataset: parsedData.feed['@attributes'].name
      },
      success: 'onLoadDatasetInstancesSuccess',
      error: 'onLoadDatasetsInstancesError'
    });
  },

  onLoadDatasetDefinitionError: function () {
    console.error('Failed to load dataset definition.');
  },

  onLoadDatasetInstancesSuccess: function (data, sender, opts) {
    var datasetJobs = [];
    data.instances.forEach(function (instance) {
      datasetJobs.push({
        dataset: opts.dataset,
        id: instance.instance,
        status: instance.status,
        endTime: new Date(instance.endTime).getTime(),
        startTime: new Date(instance.startTime).getTime()
      });
    }, this);
    this.get('datasetsData').findProperty('name', opts.dataset).set('instances', datasetJobs);
    this.set('datasetCount', this.get('datasetCount') - 1);
    var sortedDatasets = [];
    if (this.get('datasetCount') < 1) {
      App.dataSetMapper.map(this.get('datasetsData'));
      sortedDatasets = App.Dataset.find().toArray().sort(function (a, b) {
        if (a.get('name') < b.get('name'))  return -1;
        if (a.get('name') > b.get('name'))  return 1;
        return 0;
      });
      this.set('datasets', sortedDatasets);
      this.set('isLoaded', true);
    }
    var selectedDataset = this.get('selectedDataset');
    App.router.transitionTo('showDatasetJobs', selectedDataset || sortedDatasets[0]);
  },

  onLoadDatasetsInstancesError: function () {
    console.error('Failed to load dataset instances.');
  },

  targetClusters: function () {
    return App.TargetCluster.find();
  }.property(),

  manageClusters: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('mirroring.dataset.manageClusters'),
      bodyClass: App.MainMirroringManageClusterstView.extend({
        controllerBinding: 'App.router.mainMirroringManageClustersController'
      }),
      primary: Em.I18n.t('common.save'),
      secondary: null,
      onPrimary: function () {
        this.hide();
        App.router.transitionTo('main.mirroring.index');
      },
      onClose: function () {
        this.hide();
        App.router.transitionTo('main.mirroring.index');
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
      }
    });
  }
});
