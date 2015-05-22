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

define(['marionette', 'utils/LangSupport', 'utils/Utils'], function(Marionette, localization, Utils) {
  /*
   * Localization initialization
   */
  localization.setDefaultCulture(); // will take default that is en
  localization.chooseCulture();
  var App = new Marionette.Application();

  App.addRegions({
    rHeader: '#header',
    rContent: '#content',
    rTopology: '#topology',
    rCluster: '#cluster'
  });

  App.baseUrl = Utils.getStormHostDetails();

  App.addInitializer(function() {
    Backbone.history.start();
  });

  return App;
});
