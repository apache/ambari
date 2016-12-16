/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';

export default Ember.Route.extend(Ember.Evented, {

  beforeModel: function(transition){
    this.set("xmlAppPath", transition.queryParams.appPath);
    this.controllerFor('design').set("xmlAppPath", transition.queryParams.appPath);
  },
  actions : {
    editWorkflow(path){
      this.trigger('openNewTab', path);
    },
    showDashboard(){
      this.controller.set('dashboardShown', true);
      this.transitionTo('design.dashboardtab');
    },
    hideDashboard(){
      this.controller.set('dashboardShown', false);
      this.transitionTo('design');
    }
  }

});
