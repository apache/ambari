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

/**
 * Here will be stored slave functions related to components
 * @type {Object}
 */
module.exports = {

  /**
   * Return list of installed components. Syntax is:
   *
   * [{
   *    id : 'DATANODE',
   *    displayName : 'DataNode',
   *    isMaster : true,
   *    isSlave : false,
   *    isClient : false
   * }]
   *
   */
  getInstalledComponents : function(){
    var components = App.HostComponent.find();
    var names = components.mapProperty('componentName').uniq();
    var result = [];

    names.forEach(function(componentName){
      var component = components.findProperty('componentName', componentName);
      result.push(Ember.Object.create({
        id: componentName,
        isMaster: component.get('isMaster'),
        isSlave: component.get('isSlave'),
        isClient: component.get('isClient'),
        displayName: component.get('displayName'),
        serviceName: component.get('service.id')
      }));
    });

    return result;
  }
};
