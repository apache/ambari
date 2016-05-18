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
require('views/main/service/menu');

describe('App.MainServiceMenuView', function () {

  var mainServiceMenuView;

  beforeEach(function(){
    mainServiceMenuView = App.MainServiceMenuView.create();
    var view = mainServiceMenuView.get('itemViewClass').create({
      content:{
        alertsCount: 2
      }
    });
    mainServiceMenuView.set('itemViewClass',view);
  });

  var cases = [
    {
      title:'alertsCount=0 test case:',
      alertsCount:0,
      result:0
    },
    {
      title:'alertsCount=5 test case:',
      alertsCount:5,
      result:5
    },
    {
      title:'alertsCount=200 test case:',
      alertsCount:200,
      result:"99+"
    },
    {
      title:'alertsCount=99 test case:',
      alertsCount:99,
      result:99
    }
  ];


  cases.forEach(function(item){
    it(item.title,function(){
      mainServiceMenuView.get('itemViewClass').set('content.alertsCount',item.alertsCount);
      expect(mainServiceMenuView.get('itemViewClass.alertsCount')).to.not.be.undefined;
      expect(mainServiceMenuView.get('itemViewClass.alertsCount')).to.equal(item.result);
    });
  });
});
