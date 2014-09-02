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

App.Router.map(function() {
  this.resource('queues', { path: '/queues' }, function() {
    this.resource('queue', { path: '/:queue_id' });
    this.resource('trace', { path: '/log' });
  });
});


/**
 * The queues route.
 *
 * /queues
 */
App.TraceRoute = Ember.Route.extend({
  model: function() {
    return this.controllerFor('queues').get('alertMessage');
  }
});

/**
 * The queues route.
 *
 * /queues
 */
App.QueuesRoute = Ember.Route.extend({
  actions:{
  },
  model: function() {
    return this.store.find('queue');
  },
  setupController:function (c,model) {
    c.set('model',model);
    this.store.find('scheduler','scheduler').then(function (s) {
      c.set('scheduler',s);
    });
  }
});

/**
 * The queue route.
 *
 * /queues/:id
 */
App.QueueRoute = Ember.Route.extend({
  model: function(params,tr) {
    var queues = this.modelFor('queues') || this.store.find('queue'),
        filterQueues = function (queues) {
          return queues.filterBy('id',params.queue_id).get('firstObject');
        };

    return (queues instanceof DS.PromiseArray)?queues.then(filterQueues):filterQueues(queues);
  },
  afterModel:function (model) {
    if (!model) {
      this.transitionTo('queues');
    }
  },
  
  actions: {
    refreshQueue: function() {
    var x = "{ \"RequestInfo\" : { \"command\" : \"REFRESHQUEUES\", \"context\" : \"Refresh YARN Capacity Scheduler\",\"parameters/forceRefreshConfigTags\":\"capacity-scheduler\" }, "
    x = x + "\"Requests/resource_filters\":[{\"service_name\":\"YARN\",\"component_name\":\"RESOURCEMANAGER\",\"hosts\":\"c6403.ambari.apache.org\"}]}";
    
      App.Adapter.ajaxPost("/api/v1/clusters/MyCluster/requests", x);
    },
    willTransition:function (tr) {
      if (this.get('controller.isRenaming')) {
        tr.abort();
      };
    }
  }

});

/**
 * Routes index to /queues path
 *
 */
App.IndexRoute = Ember.Route.extend({
  beforeModel: function() {
    this.transitionTo('queues');
  }
});

/**
 * Loading spinner page.
 *
 */
App.LoadingRoute = Ember.Route.extend();

/**
 * Error page.
 *
 */
App.ErrorRoute = Ember.Route.extend({
  setupController:function (controller,model) {
    var response = JSON.parse(model.responseText);
    controller.set('model',response);
  }
});


