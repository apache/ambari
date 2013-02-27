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

var states = {};

function update(obj, name, isWorking){
  if(typeof isWorking == 'string' && !obj.get(isWorking)){
    return false;
  }

  var state = states[name];

  if(!state){
    var callback = function(){
      update(obj, name, isWorking);
    };
    states[name] = state = {
      timeout: null,
      func: function(){
        obj[name](callback);
      },
      callback: callback
    };
  }

  clearTimeout(state.timeout);

  state.timeout = setTimeout(state.func, App.contentUpdateInterval);
  return true;
};

App.updater = {

  /**
   * Run function periodically with <code>App.contentUpdateInterval</code> delay.
   * Example 1(wrong way, will not be working):
   *    var obj = {
   *      method: function(callback){
   *        //do something
   *      }
   *    };
   *    App.updater.run(obj, 'method');
   *
   * Will be called only once, because <code>callback</code> will never execute. Below is right way:
   *
   * Example 2:
   *    var obj = {
   *      method: function(callback){
   *        //do something
   *        callback();
   *      }
   *    };
   *    App.updater.run(obj, 'method');
   *
   * Method will always be called.
   *
   * Example 3:
   *    var obj = {
   *      method: function(callback){
   *          //do something
   *          callback();
   *      },
   *      isWorking: true
   *    };
   *    App.updater.run(obj, 'method', 'isWorking');
   *
   * <code>obj.method</code> will be called automatically.
   * Warning: You should call <code>callback</code> parameter when function finished iteration.
   * Otherwise nothing happened next time.
   * If <code>isWorking</code> provided, library will check <code>obj.isWorking</code> before iteration and
   * stop working when it equals to false. Otherwise method will always be called.
   *
   *
   *
   * @param obj Object
   * @param name Method name
   * @param isWorking Property, which will be checked as a rule for working
   * @return {*}
   */
  run: function(obj, name, isWorking){
    return update(obj, name, isWorking);
  }

}
