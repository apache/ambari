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

var _permissionsProp = function(n, l) {
  return function (arg,val) {
    if (arguments.length > 1) {
      this.set('permissions', this.replaceAt(n,(val)?l:'-'));
      return val;
    }
    return this.get('permissions')[n]===l;
  };
};

App.ChmodModalController = Em.ObjectController.extend({
  needs:['files'],
  classNames:'chmod-row',
  file:Em.computed.alias('content'),
  permissions:Em.computed.alias('file.permission'),
  usrR:_permissionsProp(1, 'r').property('permissions'),
  usrW:_permissionsProp(2, 'w').property('permissions'),
  usrE:_permissionsProp(3, 'x').property('permissions'),
  grpR:_permissionsProp(4, 'r').property('permissions'),
  grpW:_permissionsProp(5, 'w').property('permissions'),
  grpE:_permissionsProp(6, 'x').property('permissions'),
  otrR:_permissionsProp(7, 'r').property('permissions'),
  otrW:_permissionsProp(8, 'w').property('permissions'),
  otrE:_permissionsProp(9, 'x').property('permissions'),
  replaceAt:function (index,p) {
    var perm = this.get('permissions');
    return perm.substr(0, index) + p + perm.substr(index + p.length);
  }
});
