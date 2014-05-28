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

var a = DS.attr;

App.File = DS.Model.extend({
  path: function() {
    return this.get('id');
  }.property('id'),
  basedir:function () {
    var path = this.get('id');
    return path.substring(0,path.lastIndexOf('/'))||'/';
  }.property('id'),
  isDirectory: a('boolean'),
  len: a('number'),
  owner: a('string'),
  group: a('string'),
  permission: a('string'),
  accessTime: a('isodate'),
  modificationTime: a('isodate'),
  blockSize: a('number'),
  replication: a('number'),
  name:function () {
    var splitpath = this.get('path').split('/');
    return splitpath.get(splitpath.length-1);
  }.property('path'),
  date:function () {
    return parseInt(moment(this.get('modificationTime')).format('X'))
  }.property('modificationTime'),
  size: Em.computed.alias('len')
});
