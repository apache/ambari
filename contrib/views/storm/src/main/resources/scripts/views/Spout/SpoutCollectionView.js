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

define([
  'require',
  'views/Spout/SpoutItemView'
  ], function(require, vSpoutItemView) {
  'use strict';

  var spoutCollectionView = Marionette.CollectionView.extend({
    childView: vSpoutItemView,

    childViewOptions: function() {
      return {
        collection: this.collection,
        topologyId: this.topologyId,
        systemBoltFlag: this.systemBoltFlag,
        windowTimeFrame: this.windowTimeFrame,
        emptyMsg: "No spouts"
      };
    },

    initialize: function(options) {
      this.collection = options.collection;
      if(options.collection.length == 0){
        this.collection.add(new Backbone.Model());
      }
      this.topologyId = options.topologyId;
      this.systemBoltFlag = options.systemBoltFlag;
      this.windowTimeFrame = options.windowTimeFrame;
    },

    onRender: function(){}

  });

  return spoutCollectionView;
});