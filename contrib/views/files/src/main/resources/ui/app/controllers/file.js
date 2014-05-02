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

App.FileController = Ember.ObjectController.extend({
  init:function () {
    this.set('content.selected', false);
    this._super();
  },
  actions:{
    download:function (option) {
      this.store.linkFor([this.get('content')],option).then(function (link) {
        window.location.href = link;
      });
    },
    rename:function (opt,file) {
      var file = this.get('content'),
          self,path,name,newPath;
      if (opt === 'edit') {
        this.set('tmpName',file.get('title'));
        this.set('isRenaming',true);
      };

      if (opt === 'cancel') {
        this.set('tmpName','');
        this.set('isRenaming',false);
      };

      if (opt === 'confirm') {
        self = this;
        path = file.get('path');
        name = this.get('tmpName');

        if (Em.isEmpty(name)) {
          return false;
        }

        if (name === file.get('title')) {
          return self.set('isRenaming',false);
        }

        newPath = path.substring(0,path.lastIndexOf('/')+1)+name;

        this.store.move(file,newPath).then(function () {
          self.set('tmpName','');
          self.set('isRenaming',false);
        });
      };
    },
    open:function (file) {
      if (this.get('content.isDirectory')) {
        return this.transitionToRoute('files',{queryParams: {path: this.get('content.id')}});
      } else{
        return this.send('download');
      };
    },
    removeFile:function (opt) {
      if (opt=='ask') {
        this.toggleProperty('isRemoving');
        console.log('ask removeFile')
        return false;
      };

      if (opt == 'cancel'  && !this.isDestroyed) {
        this.set('isRemoving',false);
        console.log('cancel removeFile')
      }

      if (opt == 'confirm') {
        this.set('isRemoving',false);
        this.store.remove(this.get('content'));
      }
    },
    deleteFile:function () {
      var file = this.get('content');
      this.store.remove(file);
    },
  },
  tmpName:'',
  selected:false,
  isRenaming:false,
  isRemoving:false,
  isMoving:function () {
    var movingFile = this.get('parentController.movingFile.path');
    var thisFile = this.get('content.id');
    return movingFile === thisFile;
  }.property('parentController.movingFile'),
  
  setSelected:function (controller,observer) {
    this.set('selected',this.get(observer))
  }.observes('content.selected')
});
