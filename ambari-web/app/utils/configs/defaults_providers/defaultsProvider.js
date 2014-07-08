/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.DefaultsProvider = Em.Object.extend({

  /**
   * contains slave components assigned to hosts which has info required by config provider
   */
  slaveHostDependency: [],
  /**
   * contains master components assigned to hosts which has info required by config provider
   */
  masterHostDependency: [],

  /**
   * Look at cluster setup, the provided properties, and provide an object where keys are property-names, and values are the recommended defaults
   * @param {App.ServiceConfigProperty} serviceConfigProperty
   */
  getDefaults: function(serviceConfigProperty) {

  },

  /**
   * Cluster info used to calculate properties values
   * Should be redeclared in the child providers
   */
  getClusterData: function() {

  }
});
