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
  this.route('refuse');
});

var RANGER_SITE = 'ranger-yarn-plugin-properties';
var RANGER_YARN_ENABLED = 'ranger-yarn-plugin-enabled';
/**
 * The queues route.
 *
 * /queues
 */
App.QueuesRoute = Ember.Route.extend({
  actions:{
    rollbackProp:function (prop, item) {
      var attributes = item.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        item.set(prop,attributes[prop][0]);
      }
    }
  },
  beforeModel:function (transition) {
    var controller = this.container.lookup('controller:loading') || this.generateController('loading');
    controller.set('model', {message:'cluster check'});
    return this.get('store').checkCluster().catch(Em.run.bind(this,'loadingError',transition));
  },
  model: function() {
    var store = this.get('store'),
        controller = this.controllerFor('queues'),
        loadingController = this.container.lookup('controller:loading');
    var _this = this;
    return new Ember.RSVP.Promise(function (resolve,reject) {
      loadingController.set('model', {message:'access check'});
      store.checkOperator().then(function   (isOperator) {
        controller.set('isOperator', isOperator);

        loadingController.set('model', {message:'loading node labels'});
        return store.get('nodeLabels');
      }).then(function(){
        return store.findQuery( 'config', {siteName : RANGER_SITE, configName : RANGER_YARN_ENABLED}).then(function(){
          return store.find( 'config', "siteName_" + RANGER_SITE + "_configName_" + RANGER_YARN_ENABLED)
              .then(function(data){
                console.log("router.queuesRoute : data.configValue isRangerEnabled : " + data.get('configValue'));
                _this.controllerFor('configs').set('isRangerEnabledForYarn', data.get('configValue'));
              });
        })
      }).then(function () {
        loadingController.set('model', {message:'loading queues'});
        return store.find('queue');
      }).then(function (queues) {
        resolve(queues);
      }).catch(function (e) {
        reject(e);
      });
    }, 'App: QueuesRoute#model');
  },
  setupController:function (c,model) {
    c.set('model',model);
    this.store.find('scheduler','scheduler').then(function (s) {
      c.set('scheduler',s);
    });
  },
  loadingError: function (transition, error) {
    var refuseController = this.container.lookup('controller:refuse') || this.generateController('refuse'),
        message = error.responseJSON || {'message':'Something went wrong.'};

    transition.abort();

    refuseController.set('model', message);

    this.transitionTo('refuse');
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
          return queues.findBy('id',params.queue_id);
        };

    return (queues instanceof DS.PromiseArray)?queues.then(filterQueues):filterQueues(queues);
  },
  afterModel:function (model) {
    if (!model) {
      this.transitionTo('queues');
    }
  },
  actions: {
    willTransition: function (tr) {
      if (this.get('controller.isRenaming')) {
        tr.abort();
      }
    }
  }
});

/**
 * Routes index to /queues path
 *
 */
App.IndexRoute = Ember.Route.extend({
  redirect: function() {
    this.transitionTo('queues');
  }
});

/**
 * Page for trace output.
 *
 * /queues/log
 */
App.TraceRoute = Ember.Route.extend({
  model: function() {
    return this.controllerFor('queues').get('alertMessage');
  }
});

/**
 * Connection rejection page.
 *
 * /refuse
 */
App.RefuseRoute = Ember.Route.extend({
  setupController:function (controller,model) {
    if (Em.isEmpty(controller.get('model'))) {
      this.transitionTo('queues');
    }
  }
});

/**
 * Loading spinner page.
 *
 */
App.LoadingRoute = Ember.Route.extend({
  setupController:function(controller) {
    if (Em.isEmpty(controller.get('model'))) {
      this._super();
    }
  }
});

/**
 * Error page.
 *
 */
App.ErrorRoute = Ember.Route.extend({
  setupController:function (controller,model) {
    //TODO Handle Ember Error!
    var response;
    try {
      response = JSON.parse(model.responseText);
    } catch (e) {
      throw model;
      response = model;
    }
    model.trace = model.stack;
    controller.set('model',response);
  }
});

