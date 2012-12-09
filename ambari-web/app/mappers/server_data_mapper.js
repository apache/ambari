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

App.ServerDataMapper = Em.Object.extend({
  jsonKey: false,
  map: function (json) {
    if (json) {
      var model = this.get('model');
      var jsonKey = this.get('jsonKey');

      if (jsonKey && json[jsonKey]) { // if data come as { hdfs: {...} }
        json = json[jsonKey];
      }

      $.each(json, function (field, value) {
        model.set(field, value);
      })
    }
  }
});


App.QuickDataMapper = App.ServerDataMapper.extend({
  config : {},
  map:function(json){
    if(json.items){
      var result = [];
      json.items.forEach(function(item){
        result.push(this.parseIt(item, this.config));
      }, this)
    App.store.loadMany(App.Service1, result);
    }
  },
  parseIt : function(data, config){
    var result = {};
    for(var i in config){
      if(i.substr(-4) !== '_key' && typeof config[i] == 'string'){
        result[i] = this.getJsonProperty(data, config[i]);
      } else if(typeof config[i] == 'object'){
      result[i] = [];
      var _data = data[config[i+'_key']];
      var l = _data.length;
      for(var index = 0; index<l; index++){
        result[i].push(this.parseIt(_data[index], config[i]));
      }
      }
    }
    return result;
  },
  getJsonProperty:function(json, path){
    var pathArr = path.split('.');
    var current = json;
    while(pathArr.length){
      if(pathArr[0].substr(-1) == ']'){
        var index = parseInt(pathArr[0].substr(-2,1));
        var attr = pathArr[0].substr(0, pathArr[0].length-3);
        current = current[attr][index];
      } else {
        current = current[pathArr[0]];
      }
      pathArr.splice(0,1);
    }
    return current;
  }
});
