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

App.MkdirInputComponent = Em.Component.extend({
  layoutName:'components/mkdirInput',
  newDirName:'',
  isMkdir:false,
  path:'',
  actions:{
    create:function () {
      var name = this.get('newDirName');

      if (Em.isEmpty(name)) {
        return false;
      }
      newDir = [this.get('path'),name].join('/').replace('//','/');

      this.sendAction('create',newDir);
      this.setProperties({'newDirName':'','isMkdir':false});
    },
    edit:function () {
      this.set('isMkdir',true);
    },
    cancel:function () {
      this.setProperties({'newDirName':'','isMkdir':false});
    }
  }
});
