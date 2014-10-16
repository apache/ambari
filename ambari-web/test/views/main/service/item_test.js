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
require('views/main/service/item');

describe('App.MainServiceItemView', function() {

  var view = App.MainServiceItemView.create({
    controller: Em.Object.create({
      content:{
        hostComponents: []
      }
    })
  });

  describe('#mastersExcludedCommands', function() {

    var nonCustomAction = ['RESTART_ALL', 'RUN_SMOKE_TEST', 'REFRESH_CONFIGS', 'ROLLING_RESTART', 'TOGGLE_PASSIVE', 'TOGGLE_NN_HA', 'TOGGLE_RM_HA', 'MOVE_COMPONENT', 'DOWNLOAD_CLIENT_CONFIGS','MASTER_CUSTOM_COMMAND'];

    var keys = Object.keys(view.mastersExcludedCommands);
    var mastersExcludedCommands = [];
    for (var i = 0; i < keys.length; i++) {
      mastersExcludedCommands[i] = view.mastersExcludedCommands[keys[i]];
    }
    console.log("value of masterExcluded: " + mastersExcludedCommands);
    var allMastersExcludedCommands = mastersExcludedCommands.reduce(function(previous, current){
      console.log(previous);
      return previous.concat(current);
    });
    var actionMap = view.actionMap();

    var customActionsArray = [];
    for (var iter in actionMap) {
      customActionsArray.push(actionMap[iter]);
    }
    var customActions = customActionsArray.mapProperty('customCommand').filter(function(action){
      return !nonCustomAction.contains(action);
    }).uniq();

    // remove null and undefined from the list
    customActions = customActions.filter(function(value) { return value != null; });

    customActions.forEach(function(action){
      it(action + ' should be present in App.MainServiceItemView mastersExcludedCommands object', function() {
        expect(allMastersExcludedCommands).to.contain(action);
      });
    });
  });
});

