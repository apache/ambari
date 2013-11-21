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

App.MainHostServiceMenuView = Em.CollectionView.extend({
  content:function () {
    var host = this.get('host');
    var hostComponents = host.get('hostComponents');
    var services = Em.A([]);
    if (hostComponents) {
      hostComponents.forEach(function (hc) {
        var service = hc.get('service');
        var serviceName = service.get('serviceName');
        if (serviceName) {
          if(!['PIG', 'SQOOP', 'HCATALOG', 'GANGLIA'].contains(serviceName)){
            if (!services.findProperty('serviceName', serviceName)) {
              services.push(service);
            }
          }
        } else {
          console.warn("serviceName not found for " + hc.get('componentName'));
        }
      });
    }
    return misc.sortByOrder(App.Service.servicesSortOrder, services);
  }.property('host'),
  
  host: function(){
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),
  
  selectedService: null,
  
  showHostService: function(event){
    var service = event.contexts[0];
    if(service!=null){
      this.set('selectedService', service);
      var context = service;
      context.host = this.get('host');
      this.get('controller').connectOutlet('service_config_outlet', 'mainHostServiceConfigs', context);
    }else{
      this.get('controller').connectOutlet('service_config_outlet', Em.View.extend({
        template: Ember.Handlebars.compile('<i class="message">Service not available on this host</i>')
      }))
    }
  },

  didInsertElement:function () {
    var event = {
        contexts: [this.get('content').objectAt(0)]
    }
    this.showHostService(event);
  },

  activeServiceId:null,

  tagName:'ul',
  classNames:["nav", "nav-list", "nav-services"],

  itemViewClass:Em.View.extend({
    classNameBindings:["active", "clients"],
    active:function () {
      return this.get('content.serviceName') == this.get('parentView.selectedService.serviceName') ? 'active' : '';
    }.property('parentView.selectedService.serviceName'),
    template: Ember.Handlebars.compile('<a href="#" {{action showHostService view.content target="view.parentView"}} >{{view.content.displayName}}</a>')
  })
});