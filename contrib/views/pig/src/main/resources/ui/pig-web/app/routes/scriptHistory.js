/*
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

App.ScriptHistoryRoute = Em.Route.extend({
  enter: function() {
    this.controllerFor('script').set('activeTab','history');
  },
  model:function(param) {
    this.controllerFor('pig').set('activeScriptId', param.script_id);
    return this.store.find('job', {scriptId: param.script_id});
  },
  setupController:function (controller,model) {
    var script_id = this.controllerFor('pig').get('activeScriptId');
    model.store.recordArrayManager.registerFilteredRecordArray(model,model.type,function(job) {
      return job.get('scriptId') == script_id;
    });
    controller.set('model',model);
  }
});
