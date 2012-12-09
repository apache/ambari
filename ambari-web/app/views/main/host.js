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

App.MainHostView = Em.View.extend({
  templateName:require('templates/main/host'),
  filterByName:"",
  controller:function () {
    return App.router.get('mainHostController');
  }.property(),
  content:function () {
    return App.router.get('mainHostController.content');
  }.property('App.router.mainHostController.content'),
  componentsIds:[1, 2, 3, 4, 5, 6, 7, 8],

  isFilterOpen:false,

//  isApplyDisabled:function () {
//    return !this.get('isFilterOpen')
//  }.property('isFilterOpen'),

  btnGroupClass:function () {
    return this.get('isFilterOpen') ? 'btn-group open' : 'btn-group';
  }.property('isFilterOpen'),

  applyFilters:function () {
    this.set('isFilterOpen', false);
    $(document).unbind('click');
    App.router.get('mainHostController').filterByComponentsIds();
  },

  allComponentsChecked:false,
  toggleAllComponents:function () {
    this.set('masterComponentsChecked', this.get('allComponentsChecked'));
    this.set('slaveComponentsChecked', this.get('allComponentsChecked'));
  }.observes('allComponentsChecked'),

  masterComponentsChecked:false,
  toggleMasterComponents:function () {
    var checked = this.get('masterComponentsChecked');
    this.get('controller.masterComponents').forEach(function (comp) {
      comp.set('checkedForHostFilter', checked);
    });
  }.observes('masterComponentsChecked'),

  slaveComponentsChecked:false,
  toggleSlaveComponents:function () {
    var checked = this.get('slaveComponentsChecked');
    this.get('controller.slaveComponents').forEach(function (comp) {
      comp.set('checkedForHostFilter', checked);
    });
  }.observes('slaveComponentsChecked'),

  didInsertElement:function () {
    this._super();
    this.set('allComponentsChecked', true); // select all components (checkboxes) on start.
  },

  applyHostFilter:function () {
    App.router.get('mainHostController').filterHostsBy('hostName', this.get('filterByName'));
  }.observes('filterByName'),

  closeFilters:function () {
    $(document).unbind('click');
    this.clickFilterButton();
  },

  clickFilterButton:function () {
    var self = this;
    this.set('isFilterOpen', !this.get('isFilterOpen'));
    if (this.get('isFilterOpen')) {
      var filters = App.router.get('mainHostController.filters.components');
      $('.filter-component').each(function () {
        var componentId = parseInt($(this).attr('id').replace('component-', ''));
        var index = filters.indexOf(componentId);
        $(this).attr('checked', index == -1);
      });
//      this.set('componentsIds', filters.toArray());

      var dropDown = $('#filter-dropdown');
      var firstClick = true;
      $(document).bind('click', function (e) {
        if (!firstClick && $(e.target).closest(dropDown).length == 0) {
          self.set('isFilterOpen', false);
          $(document).unbind('click');
        }
        firstClick = false;
      });
    }
  },
  HostView:Em.View.extend({
    content:null,

    labels: function(){
      return this.get('content.components').getEach('displayName').join(', ');
    }.property('content.components.@each'),

    usageStyle:function () {
      return "width:" + this.get('content.diskUsage') + "%";
    }.property('content.diskUsage'),

    HostCheckboxView:Em.Checkbox.extend({
      content:null,
      isChecked:false,
      change:function (event) {
        this.set('isChecked', !this.get('content.isChecked'));
        App.router.get('mainHostController').onHostChecked(this.get('content'));
      }
    })
  }),

  RackCombobox:App.Combobox.extend({
    disabled:function () {
      var selectedHostsIds = App.router.get('mainHostController.selectedHostsIds');

      // when user apply assigning and hosts become unchecked, we need to clear textfield
      if (!selectedHostsIds.length) {
        this.clearTextFieldValue();
      }

      return !selectedHostsIds.length;

    }.property('App.router.mainHostController.selectedHostsIds'),

    recordArray:App.Cluster.find(),
    placeholderText:Em.I18n.t('hosts.assignRack'),
    selectionBinding:"App.router.mainHostController.selectedRack",
    optionLabelPath:"content.clusterName",
    optionValuePath:"content.id",
    didInsertElement:function () {
      this._super();
      App.router.get('mainHostController').propertyDidChange('selectedHostsIds');
    }
  }),

  assignRackButtonDisabled:function () {
    var selectedHostsIds = App.router.get('mainHostController.selectedHostsIds');
    var rack = App.router.get('mainHostController.selectedRack');
    return (selectedHostsIds.length && rack && rack.constructor == 'App.Cluster') ? false : "disabled";
  }.property('App.router.mainHostController.selectedHostsIds', 'App.router.mainHostController.selectedRack')
});