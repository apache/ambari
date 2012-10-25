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
  controller: function(){
    return App.router.get('mainHostController');
  }.property(),
  content:function () {
    return App.router.get('mainHostController.content');
  }.property('App.router.mainHostController.content'),
  componentsIds: [1, 2, 3, 4, 5, 6, 7, 8],

  isFilterOpen:false,
  isApplyDisabled:function () {
    return !this.get('isFilterOpen')
  }.property('isFilterOpen'),
  btnGroupClass:function () {
    return this.get('isFilterOpen') ? 'btn-group open' : 'btn-group';
  }.property('isFilterOpen'),

  applyFilters:function () {
    this.set('isFilterOpen', false);
    App.router.get('mainHostController').filterByComponentsIds();
  },

  allComponentsChecked: true,
  toggleAllComponents: function(){
    this.set('masterComponentsChecked', this.get('allComponentsChecked'));
    this.set('slaveComponentsChecked', this.get('allComponentsChecked'));
  }.observes('allComponentsChecked'),

  masterComponentsChecked: false,
  toggleMasterComponents: function(){
    var checked = this.get('masterComponentsChecked');
    this.get('controller.masterComponents').forEach(function(comp){
      comp.set('checkedForHostFilter', checked);
    });
  }.observes('masterComponentsChecked'),

  slaveComponentsChecked:false,
  toggleSlaveComponents: function(){
    var checked = this.get('slaveComponentsChecked');
    this.get('controller.slaveComponents').forEach(function(comp){
      comp.set('checkedForHostFilter', checked);
    });
  }.observes('slaveComponentsChecked'),

  applyHostFilter:function () {
    App.router.get('mainHostController').filterHostsBy('hostName', this.get('filterByName'));
  }.observes('filterByName'),

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
    labels:'',
    init:function () {
      this._super();
      var labels = this.get('content.components').getEach('label');
      this.set('labels', labels.join(', '));
    },
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

  ComponentCheckboxView:Em.Checkbox.extend({
    content:null,
    elementId:function () {
      return 'component-' + this.get('content.id');
    }.property('content.id'),
    classNames:['filter-component'],
    parentView:function () {
      return this._parentView.templateData.view;
    }.property(),
    checkedBinding:"content.checkedForHostFilter"

//    willInsertElement: function() {
//      this._super();
//      console.warn("CONTENT_HOST_CHECKED:", this.get('content.checkedForHostFilter'), " THIS CHECKED: ", this.get('checked'));
//    },
//
//    didInsertElement: function(){
//      this._super();
//      this.propertyDidChange('content.checkedForHostFilter');
//      console.warn("CONTENT_HOST_CHECKED:", this.get('content.checkedForHostFilter'), " THIS CHECKED: ", this.get('checked'));
//    }

//    test: function(){
//      console.warn("Test:", this.get('content.checkedForHostFilter'));
//    }.observes("content.checkedForHostFilter")

//    change:function (event) {
//      var parent = this._parentView.templateData.view;
//      var componentsIds = parent.get('componentsIds');
//      var componentId = this.get('content.id');
//      var index = componentsIds.indexOf(componentId);
//      if (index == -1) componentsIds.push(componentId);
//      else componentsIds.splice(index, 1);
//    }
  })
});