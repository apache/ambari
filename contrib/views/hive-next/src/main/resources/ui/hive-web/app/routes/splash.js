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

import Ember from 'ember';

export default Ember.Route.extend({

  model: function() {
    return Ember.Object.create({
      hdfsTest: null,
      hdfsTestDone: null,
      hiveserverTest: null,
      hiveserverTestDone: null,
      atsTest: null,
      atsTestDone: null,
      userhomeTest: null,
      userhomeTestDone: null,
      percent: 0,
      numberOfChecks: null,
      serviceCheckPolicy: null,
    });
  },

  setupController: function(controller, model) {

    if (!model) {
      return;
    }

    controller.set('model', model);
    var self = this;

    function loadView(){
      if (model.get("hiveserverTest")
        && model.get("hdfsTest")
        && model.get("atsTest")
        && model.get("userhomeTest")) {
        Ember.run.later(this, function() {
          self.send('transition');
        }, 2000);
      }

    }


    function checkHive() {
      controller.checkConnection().then(function () {
        var percent = model.get('percent');
        model.set("hiveserverTest", true);
        model.set("hiveserver" + 'TestDone', true);
        model.set('percent', percent + (100/model.get("numberOfChecks")));
        loadView();
      }, function () {
        if (model.get('ldapFailure')) {
          var percent = model.get('percent');
          controller.requestLdapPassword(function () {
            // check the connection again
            controller.checkConnection().then(function () {
              model.set("hiveserverTest", true);
              model.set("hiveserver" + 'TestDone', true);
              model.set('percent', percent + (100/model.get("numberOfChecks")));
              loadView();
            }, function () {
              var percent = model.get('percent');
              var checkFailedMessage = "Hive authentication failed";
              var errors = controller.get("errors");
              errors += checkFailedMessage;
              errors += '<br>';
              controller.set("errors", errors);
              model.get("hiveserverTest", false);
              model.set("hiveserver" + 'TestDone', true);
              model.set('percent', percent + (100/model.get("numberOfChecks")));
              loadView();
            });
          });
        } else {
          model.get("hiveserverTest", false);
          model.set("hiveserver" + 'TestDone', true);
          model.set('percent', model.get('percent') + (100/model.get("numberOfChecks")));
          loadView();
        }
      });
    }

    this.fetchServiceCheckPolicy()
      .then (function(data) {
        var numberOfChecks = 0;
        var serviceCheckPolicy = data.serviceCheckPolicy;
        for (var serviceCheck in serviceCheckPolicy) {
          if (serviceCheckPolicy[serviceCheck] === true) {
            numberOfChecks++;
          }
        }
        model.set("numberOfChecks", numberOfChecks);
        model.set("serviceCheckPolicy", serviceCheckPolicy);
        controller.startTests().then(function () {
          if(serviceCheckPolicy.checkHive === true){
            checkHive();
          }else{
            model.set("hiveserver" + 'TestDone', true);
            model.set("hiveserver" + 'Test', true);
            loadView();
          }
        });
      });
  },

  fetchServiceCheckPolicy: function(){
    var adapter = this.container.lookup('adapter:service-check');
    return adapter.fetchServiceCheckPolicy();
  },
  actions: {
    transition: function() {
      this.transitionTo('index');
    }
  }

});
