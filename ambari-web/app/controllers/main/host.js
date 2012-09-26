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


App.MainHostController = Em.ArrayController.extend(App.Pagination, {
  name:'mainHostController',
  content: [],
  fullContent: App.Host.find(),
  clusters: App.Cluster.find(),
  allComponents: App.Component.find(),
  totalBinding: 'fullContent.length',
  filters: {components:[]},
  pageSize: 3,
  pageSizeRange: [1,3,5,'all'],
  rangeStart: 0,
  allChecked: false,
  selectedHostsIds: [],
  sortingAsc: true,

  isDisabled:true,

  onAllChecked: function () {
    var hosts = this.get('content');
    hosts.setEach('isChecked', this.get('allChecked'));
    this.set('isDisabled', !this.get('allChecked'));
    var selectedHostsIds = this.get('allChecked') ? hosts.getEach('id'):[];
    this.set('selectedHostsIds', selectedHostsIds);
  }.observes('allChecked'),

  onHostChecked: function (checked, hostId) {
    var selected = this.get('selectedHostsIds');
    if (checked) selected.push(hostId);
    else {
      var index = selected.indexOf(hostId);
      if(index!=-1) selected.splice(index, 1);
    }
    this.set('isDisabled', selected.length == 0);
  },

  changeSelectedHosts: function() {
    var visibleHosts = this.get('content');
    var selectedHosts = visibleHosts.filterProperty('isChecked', true);
    this.get('fullContent').forEach(function(item) {
      var index = visibleHosts.getEach('id').indexOf(item.get('id'));
      if(index == -1) item.set('isChecked', false);
    });
    this.set('isDisabled', selectedHosts.length == 0);
    this.set('selectedHostsIds', selectedHosts.getEach('id'));
  },

  setFilters: function(checked, componentId) {
    var filters = this.get('filters.components');
    if (checked){
      filters.push(componentId);
    } else {
      var index = filters.indexOf(componentId);
      if(index!=-1) filters.splice(index, 1);
    }
    this.changeContent();
  },

  filterByComponentId: function(componentId) {
    this.set('filters.components', [componentId]);
    this.changeContent();
  },

  changeContent: function() {
    var items = [];
    var filters = this.get('filters.components');
    if (filters.length){
      this.get('fullContent').forEach(function(item) {
        var inFilters = true;
        $.each(filters, function (i, componentId) {
          if (item.get('components').getEach('id').indexOf(componentId) == -1){
            inFilters = false;
          }
        });
        if (inFilters){
          items.push(item);
        }
      });
      this.set('total', items.length);
    } else {
      items = this.get('fullContent');
      this.set('total', this.get('fullContent.length'));
    }
    var content = items.slice(this.get('rangeStart'), this.get('rangeStop'));
    this.replace(0, this.get('length'), content);
    this.changeSelectedHosts();
  }.observes('rangeStart', 'rangeStop', 'filters.components', 'total'),

  showNextPage: function() {
    this.nextPage();
  },
  showPreviousPage: function() {
    this.previousPage();
  },
  assignedToRackPopup: function(event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.assignedToRack.popup.header'),
      body: Em.I18n.t('hosts.assignedToRack.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.assignedToRack(event.context);
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  assignedToRack: function(rack) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.setEach('cluster', rack);
  },

  decommissionButtonPopup: function() {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.decommission.popup.header'),
      body: Em.I18n.t('hosts.decommission.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        alert('do');
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  deleteButtonPopup: function() {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.header'),
      body: Em.I18n.t('hosts.delete.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.removeHosts();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  removeHosts: function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.hostName);
    });
//    App.db.removeHosts(selectedHosts);
    this.get('fullContent').removeObjects(selectedHosts);
  },
  sortByName: function () {
    var asc = this.get('sortingAsc');
    var objects = this.get('fullContent').toArray().sort(function(a, b)
    {
      var nA = a.get('hostName').toLowerCase();
      var nB = b.get('hostName').toLowerCase();
      if(nA < nB)
        return asc ? -1 : 1;
      else if(nA > nB)
        return asc ? 1 : -1;
      return 0;
    });
    this.set('fullContent', objects);
    this.set('sortingAsc', !this.get('sortingAsc'));
    this.changeContent();
  }

});