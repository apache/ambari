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

App.MainMirroringManageClusterstView = Em.View.extend({
  name: 'mainMirroringManageClustersView',
  templateName: require('templates/main/mirroring/manage_clusters'),

  didInsertElement: function () {
    this.get('controller').clearStep();
  },

  clusterSelect: Ember.Select.extend({
    classNames: ['cluster-select'],
    multiple: true,
    content: function () {
      return App.TargetCluster.find().mapProperty('clusterName');
    }.property(),
    selectedCluster: null,
    onSelect: function () {
      if (this.get('selection.length')) {
        if (this.get('selection').length === 1) {
          this.set('selectedCluster', this.get('selection')[0]);
        } else {
          this.set('selection', [this.get('selectedCluster')]);
        }
      } else {
        this.set('selectedCluster', null);
      }
    }.observes('selection')
  }),

  ambariClusterSelect: Ember.Select.extend({
    attributeBindings: ['disabled'],
    classNames: ['span5'],
    content: function () {
      return [App.get('clusterName')];
    }.property()
  }),

  ambariRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('controller.ambariSelected');
    }.property('controller.ambariSelected'),
    type: 'radio',

    click: function () {
      this.set('controller.ambariSelected', true);
      this.set('controller.ambariServerSelected', false);
      this.set('controller.interfacesSelected', false);
    }
  }),

  ambariServerRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('controller.ambariServerSelected');
    }.property('controller.ambariServerSelected'),
    type: 'radio',

    click: function () {
      this.set('controller.ambariSelected', false);
      this.set('controller.ambariServerSelected', true);
      this.set('controller.interfacesSelected', false);
    }
  }),

  interfacesRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('controller.interfacesSelected');
    }.property('controller.interfacesSelected'),
    type: 'radio',

    click: function () {
      this.set('controller.ambariSelected', false);
      this.set('controller.ambariServerSelected', false);
      this.set('controller.interfacesSelected', true);
    }
  })
});


