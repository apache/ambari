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

App.MainHostController = Em.ArrayController.extend({
  name:'mainHostController',
  content:[],
  fullContent:App.Host.find(),
  clusters:App.Cluster.find(),
  //componentsForFilter: App.Component.find(),
  isAdmin: function(){
    return App.db.getUser().admin;
  }.property('App.router.loginController.loginName'),
  componentsForFilter:function() {
    var components = App.Component.find();
    var ret = new Array();
    if (!components) {
      return ret;
    }
    components.forEach(function(item) {
      var o = Ember.Object.create({
        id: item.get('id'),
        isMaster: item.get('isMaster'),
        isSlave: item.get('isSlave'),
        displayName: item.get('displayName'),
        componentName: item.get('componentName'),
        checkedForHostFilter: item.get('checkedForHostFilter')
      });
      ret.push(o);
    });
    return ret;
  }.property(),

  totalBinding:'fullContent.length',
  filters:{components:[]},
  pageSize: 25,
  pageSizeRange:[10, 25, 50, 100, 'all'],
  rangeStart:0,
//  allChecked:false,
//  selectedHostsIds:[],
  selectedRack:null,

//  assignHostsToRack:function () {
//    var selectedRack = this.get('selectedRack');
//    var sureMessage = this.t('hosts.assignToRack.sure');
//    var hostsIds = this.get('selectedHostsIds');
//
//    var hostString = hostsIds.length + " " + this.t(hostsIds.length > 1 ? "host.plural" : "host.singular");
//
//    if (selectedRack.constructor == 'App.Cluster' && hostsIds.length
//      && confirm(sureMessage.format(hostString, selectedRack.get('clusterName')))) {
//      this.get('content').forEach(function (host) {
//        if (host.get('isChecked')) {
//          host.set('cluster', selectedRack);
//          host.set('isChecked', false);
//        }
//      })
//      this.set('selectedHostsIds', []);
//    }
//
//  },

  sortingAsc:true,
  isSort:false,
  sortClass:function () {
    return this.get('sortingAsc') ? 'icon-arrow-down' : 'icon-arrow-up';
  }.property('sortingAsc'),
  isDisabled:true,

  checkRemoved:function (host_id) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('id', host_id);
    this.get('fullContent').removeObjects(selectedHosts);
  },

  masterComponents:function () {
    var components = [];
    this.get('componentsForFilter').forEach(function (component) {
      if (component.get('isMaster')) {
        components.push(component);
      }
    });
    return components;
  }.property('componentsForFilter'),

  slaveComponents:function () {
    var components = [];
    this.get('componentsForFilter').forEach(function (component) {
      if (component.get('isSlave')) {
        components.push(component);
      }
    });
    return components;
  }.property('componentsForFilter'),

  clientComponents: function() {
    var components = [];
    this.get('componentsForFilter').forEach(function(component) {
      if (!component.get('isMaster') && !component.get('isSlave')) {
        components.push(component);
      }
    });
    return components;
  }.property('componentsForFilter'),

  backgroundOperationsCount:function () {
    return 5;
  }.property(),

//  onAllChecked:function () {
//    var hosts = this.get('content');
//    hosts.setEach('isChecked', this.get('allChecked'));
//    this.set('isDisabled', !this.get('allChecked'));
//    var selectedHostsIds = this.get('allChecked') ? hosts.getEach('id') : [];
//    this.set('selectedHostsIds', selectedHostsIds);
//  }.observes('allChecked'),
//
//  onHostChecked:function (host) {
//    var selected = this.get('selectedHostsIds');
//    host.set('isChecked', !host.get('isChecked'));
//    if (host.get('isChecked')) {
//      selected.push(host.get('id'));
//    } else {
//      var index = selected.indexOf(host.get('id'));
//      if (index != -1) selected.splice(index, 1);
//    }
//    this.set('isDisabled', selected.length == 0);
//    this.propertyDidChange('selectedHostsIds');
//  },
//
//  changeSelectedHosts:function () {
//    var visibleHosts = this.get('content');
//    var selectedHosts = visibleHosts.filterProperty('isChecked', true);
//    this.get('fullContent').forEach(function (item) {
//      var index = visibleHosts.getEach('id').indexOf(item.get('id'));
//      if (index == -1) item.set('isChecked', false);
//    });
//    this.set('isDisabled', selectedHosts.length == 0);
//    this.set('selectedHostsIds', selectedHosts.getEach('id'));
//  },

  checkedComponentsIds:function () {
    var checked = [];
    this.get('componentsForFilter').forEach(function (comp) {
      if (comp.get('checkedForHostFilter'))
        checked.push(comp.get('id'));
    });

    return checked;
  },

  filterByComponentsIds:function () {
    var componentsIds = this.checkedComponentsIds();
    this.set('filters.components', componentsIds);

//      component.set('isChecked', component.get('id') != -1);

    this.changeContent();
  },

  filterHostsBy:function (field, value) {
    this.set('hostFilter' + field, value);
    this.changeContent();
  },

  filterByComponent:function (component) {
    var id = component.get('id');
    /*this.get('componentsForFilter').setEach('isChecked', false);
    component.set('isChecked', true);*/
    this.get('componentsForFilter').setEach('checkedForHostFilter', false);
    this.get('componentsForFilter').filterProperty('id', id).setEach('checkedForHostFilter', true);
    //component.set('checkedForHostFilter', true);
    this.set('filters.components', [component.get('id')]);
    console.log(this.get('filters.components').objectAt(0));
    this.changeContent();
  },


  applyHostFilters:function (items) {

    var field = 'hostName'; // make this function universal
    var value = this.get('hostFilter' + field);

    var itemsToDelete = [];
    if (value) {
      items.forEach(function (host, index) {
        if (host) {
          var fieldValue = host.get(field);
          if (fieldValue) {
            if (fieldValue.indexOf(value) == -1) {
              itemsToDelete.push(host);
            }
          }
        }
      });
    }

    if (itemsToDelete.length) {
      itemsToDelete.forEach(function (hostToDelete) {
        var index = items.indexOf(hostToDelete);
        items.removeAt(index);
      })
    }

    return items;
  },

  changeContent:function () {
    var items = [];
    var filters = this.get('filters.components');
    this.get('fullContent').forEach(function (item) {
      if (filters.length) {
        var inFilters = false;
        item.get('components').forEach(function (component) {
          if (filters.indexOf(component.get('id')) != -1) {
            inFilters = true;
          }
        });


        if (inFilters) {
          items.push(item);
        }
      }
      else {
        items.push(item);
      }
    });

    items = this.applyHostFilters(items);
    this.set('total', items.length);

    var content = items.slice(this.get('rangeStart'), this.get('rangeStop'));
    this.replace(0, this.get('length'), content);
//    this.changeSelectedHosts();
  }.observes('rangeStart', 'rangeStop', 'total'),

  showNextPage:function () {
    this.nextPage();
  },
  showPreviousPage:function () {
    this.previousPage();
  },
  assignedToRackPopup:function (event) {
    var self = this;
    App.ModalPopup.show({
      header:Em.I18n.t('hosts.assignedToRack.popup.header'),
      body:Em.I18n.t('hosts.assignedToRack.popup.body'),
      primary:'Yes',
      secondary:'No',
      onPrimary:function () {
        self.assignedToRack(event.context);
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },

  assignedToRack:function (rack) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.setEach('cluster', rack);
  },

  decommissionButtonPopup:function () {
    var self = this;
    App.ModalPopup.show({
      header:Em.I18n.t('hosts.decommission.popup.header'),
      body:Em.I18n.t('hosts.decommission.popup.body'),
      primary:'Yes',
      secondary:'No',
      onPrimary:function () {
        alert('do');
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },
  deleteButtonPopup:function () {
    var self = this;
    App.ModalPopup.show({
      header:Em.I18n.t('hosts.delete.popup.header'),
      body:Em.I18n.t('hosts.delete.popup.body'),
      primary:'Yes',
      secondary:'No',
      onPrimary:function () {
        self.removeHosts();
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },
  removeHosts:function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.hostName);
    });
//    App.db.removeHosts(selectedHosts);
    this.get('fullContent').removeObjects(selectedHosts);
  },
  sortByName:function () {
    var asc = this.get('sortingAsc');
    var objects = this.get('fullContent').toArray().sort(function (a, b) {
      var nA = a.get('hostName').toLowerCase();
      var nB = b.get('hostName').toLowerCase();
      if (nA < nB)
        return asc ? -1 : 1;
      else if (nA > nB)
        return asc ? 1 : -1;
      return 0;
    });
    this.set('fullContent', objects);
    this.set('isSort', true);
    this.set('sortingAsc', !this.get('sortingAsc'));
    this.changeContent();
  }

});