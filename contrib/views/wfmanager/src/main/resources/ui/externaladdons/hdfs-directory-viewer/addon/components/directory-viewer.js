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

import Ember from 'ember';
import layout from '../templates/components/directory-viewer';

export default Ember.Component.extend({
  layout,
  config: Ember.Object.create({}),
  classNames: ['directory-viewer'],
  startPath: '/',
  treeData: Ember.A(),
  currentPath: Ember.computed.oneWay('startPath'),
  currentQueryParam: Ember.computed('currentPath', function() {
    return Ember.$.param({path: this.get('currentPath')});
  }),

  startFetch: Ember.on('didInitAttrs', function() {
    this.get('uploaderService').on('uploadSuccess', function(){
      this.fetchData();
    }.bind(this));
    this.fetchData();
  }),


  fetchData: function() {
    this.listPath(this.get('currentQueryParam')).then(
      (response) => {
        let list = this.filterDirectoriesIfRequired(response.files);
        this.modifyTreeViewData(list);
      }, (error) => {
        this.sendAction('errorAction', error);
      }
    );
  },

  /**
   * Makes a XHR call and returns a promise.
   */
  listPath: function(params) {
    let config = this.get('config');
    let listUrl = config.listDirectoryUrl(params);
    let headers = config.getHeaders();
    return Ember.$.ajax(listUrl, {
      headers: headers
    });
  },

  filterDirectoriesIfRequired: function(files) {
    let showOnlyDirectories = this.get('config.showOnlyDirectories');
    return files.filter((entry) => {
      return (!(showOnlyDirectories) || entry.isDirectory);
    });
  },

  modifyTreeViewData: function(response) {
    let paths = response.map((entry) => {
      let isDirectory = entry.isDirectory;
      let icon = isDirectory ? this.get('config.folderIcon') : this.get('config.fileIcon');
      let data = {
        path: entry.path,
        pathSegment: this.getNameForPath(entry.path),
        isDirectory: isDirectory,
        icon: icon,
        text: this.getNameForPath(entry.path)
      };
      if(isDirectory) {
        data.nodes = Ember.A();
      }
      return data;
    });

    var currentPath = this.get('currentPath');
    var newTreeData = Ember.copy(this.get('treeData'), true);
    if(currentPath === '/') {
      newTreeData = paths;
    } else {
      this.insertPathToTreeData(newTreeData, paths, currentPath.substring(1));
    }

    this.set('treeData', newTreeData);
    this.send('refreshTreeView');
  },

  insertPathToTreeData(treeData, paths, pathSegment) {
    let isFinalSegment = pathSegment.indexOf('/') === -1? true: false;
    let firstPathSegment = isFinalSegment? pathSegment: pathSegment.substring(0, pathSegment.indexOf('/'));
    if(treeData.length === 0) {
      treeData.pushObjects(paths);
    } else {
      treeData.forEach((entry) => {
        entry.state = {};
        if (entry.pathSegment === firstPathSegment) {
          let nodesLength = entry.nodes.length;
          entry.state.expanded = true;
          if(nodesLength === 0) {
            paths.forEach((pathEntry) => {
              entry.nodes.push(pathEntry);
            });
          } else if(nodesLength > 0 && nodesLength !== paths.length && isFinalSegment){
            entry.nodes = paths;
          } else {
            this.insertPathToTreeData(entry.nodes, paths, pathSegment.substring(pathSegment.indexOf('/') + 1));
          }
        } else {
          this.collapseAll(entry);
        }
      });
    }
  },

  collapseAll: function(node) {
    if (Ember.isNone(node.state)) {
      node.state = {};
    }
    node.state.expanded = false;
    if(!Ember.isNone(node.nodes)) {
      node.nodes.forEach((entry) => {
        this.collapseAll(entry);
      });
    }
  },

  getNameForPath: function(path) {
    return path.substring(path.lastIndexOf("/") + 1);
  },

  collapseAllExceptPath: function(pathSegment) {
    let collapseAll = function(nodes, pathSegment) {
      var firstPathSegment;
      if (pathSegment.indexOf('/') !== -1) {
        firstPathSegment = pathSegment.substring(0, pathSegment.indexOf('/'));
      } else {
        firstPathSegment = pathSegment;
      }

      nodes.forEach((entry) => {
        if (Ember.isNone(entry.state)) {
          entry.state = {};
        }
        if(firstPathSegment !== entry.pathSegment) {
          entry.state.expanded = false;
        } else {
          entry.state.expanded = true;
          collapseAll(entry.nodes, pathSegment.substring(pathSegment.indexOf('/') + 1));
        }
      });
    };
    var newTreeData = this.get('treeData');
    collapseAll(newTreeData, pathSegment);
    this.set('treeData', newTreeData);
    this.send('refreshTreeView');
  },

  actions: {
    refreshTreeView() {
      Ember.run.later(() => {
        this.$().treeview({
          data: this.get('treeData'),
          expandIcon: this.get('config.expandIcon'),
          collapseIcon: this.get('config.collapseIcon'),
          //emptyIcon: "fa",
          showBorder: false,
          onNodeSelected: (event, data) => {
            this.set('currentPath', data.path);
            this.sendAction('pathSelectAction', {path: data.path, isDirectory: data.isDirectory});
          },
          onNodeExpanded: (event, data) => {
            this.set('currentPath', data.path);
            if (!Ember.isNone(data.nodes) && data.nodes.length === 0) {
              var node = this.$().treeview('getNode', data.nodeId);
              node.icon = "fa fa-refresh fa-spin";
              this.fetchData();
            } else {
              this.collapseAllExceptPath(data.path.substring(1));
            }
          }
        });
      });
    }
  }
});
