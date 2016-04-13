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
'use strict';

angular.module('ambariAdminConsole')
.factory('View', ['$http', '$q', 'Settings', function($http, $q, Settings) {

  function ViewInstance(item){
    angular.extend(this, item);
  }

  ViewInstance.find = function(viewName, version, instanceName) {
    var deferred = $q.defer();
    var fields = [
      'privileges/PrivilegeInfo',
      'ViewInstanceInfo',
      'resources'
    ];

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/'+viewName+'/versions/'+version+'/instances/'+instanceName,
      mock: 'view/views.json',
      params:{
        'fields': fields.join(',')
      }
    })
    .success(function(data) {
      deferred.resolve(new ViewInstance(data));
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };


  function View(item){
    var self = this;
    self.view_name = item.ViewInfo.view_name;
    self.versions = '';
    self.instances = [];
    self.canCreateInstance = false;
    var versions = {};
    angular.forEach(item.versions, function(version) {
      versions[version.ViewVersionInfo.version] = {count: version.instances.length, status: version.ViewVersionInfo.status};
      if(version.ViewVersionInfo.status === 'DEPLOYED'){ // if atelast one version is deployed
        self.canCreateInstance = true;
      }
      
      angular.forEach(version.instances, function(instance) {
        instance.label = instance.ViewInstanceInfo.label || version.ViewVersionInfo.label || instance.ViewInstanceInfo.view_name;
      });

      self.instances = self.instances.concat(version.instances);
    });
    self.versions = versions;

    self.versionsList = item.versions;
  }

  View.getInstance = function(viewName, version, instanceName) {
    return ViewInstance.find(viewName, version, instanceName);
  };

  View.deleteInstance = function(viewName, version, instanceName) {
    return $http.delete(Settings.baseUrl +'/views/'+viewName+'/versions/'+version+'/instances/'+instanceName, {
      headers: {
        'X-Requested-By': 'ambari'
      }
    });
  };

  View.updateInstance = function(viewName, version, instanceName, data) {
    return $http({
      method: 'PUT',
      url: Settings.baseUrl + '/views/' +viewName + '/versions/'+version+'/instances/' + instanceName,
      data: data
    });
  };

  View.getPermissions = function(params) {
    var deferred = $q.defer();

    var fields = [
      'permissions/PermissionInfo/permission_name'
    ];
    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/' + params.viewName + '/versions/'+ params.version,
      params: {
        'fields': fields.join(',')
      }
    }).success(function(data) {
      deferred.resolve(data.permissions);
    })
    .catch(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  View.getPrivileges = function(params) {
    var deferred = $q.defer();

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/' + params.viewName + '/versions/' + params.version + '/instances/' + params.instanceId,
      params: {
        fields: 'privileges/PrivilegeInfo'
      }
    })
    .success(function(data) {
      deferred.resolve(data.privileges);
    })
    .catch(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  

  View.getVersions = function(viewName) {
    var deferred = $q.defer();

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/'+viewName + '?versions/ViewVersionInfo/status=DEPLOYED'
    }).success(function(data) {
      var versions = [];
      angular.forEach(data.versions, function(version) {
        versions.push(version.ViewVersionInfo.version);
      });

      deferred.resolve(versions);
    }).catch(function(data) {
      deferred.reject(data);
    });
    return deferred.promise;
  };

  View.createInstance = function(instanceInfo) {
    var deferred = $q.defer(),
      properties = {},
      settings = {},
      data = {
        instance_name: instanceInfo.instance_name,
        label: instanceInfo.label,
        visible: instanceInfo.visible,
        icon_path: instanceInfo.icon_path,
        icon64_path: instanceInfo.icon64_path,
        description: instanceInfo.description
      };

    angular.forEach(instanceInfo.properties, function(property) {
      if(property.clusterConfig) {
        properties[property.name] = property.value
      }else {
        settings[property.name] = property.value
      }
    });

    data.properties = settings;

    if(instanceInfo.isLocalCluster) {
      data.cluster_handle = instanceInfo.clusterName;
    } else {
      angular.extend(data.properties, properties);
    }

    $http({
      method: 'POST',
      url: Settings.baseUrl + '/views/' + instanceInfo.view_name +'/versions/'+instanceInfo.version + '/instances/'+instanceInfo.instance_name,
      data:{
        'ViewInstanceInfo' : data
      }
    })
    .success(function(data) {
      deferred.resolve(data);
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };

  View.createPrivileges = function(params, data) {
    return $http({
      method: 'POST',
      url: Settings.baseUrl + '/views/' + params.view_name +'/versions/'+params.version+'/instances/'+params.instance_name+'/privileges',
      data: data
    });
  };

  View.deletePrivileges = function(params, data) {
    return $http({
      method: 'DELETE',
      url: Settings.baseUrl + '/views/' + params.view_name +'/versions/'+params.version+'/instances/'+params.instance_name+'/privileges',
      data: data
    });
  };

  View.updatePrivileges = function(params, privileges) {
    return $http({
      method: 'PUT',
      url: Settings.baseUrl + '/views/' + params.view_name +'/versions/'+params.version+'/instances/'+params.instance_name+'/privileges',
      data: privileges
    });
  };

  View.deletePrivilege = function(params) {
    return $http({
      method: 'DELETE',
      url: Settings.baseUrl + '/views/' + params.view_name +'/versions/'+params.version+'/instances/'+params.instance_name+'/privileges',
      params: {
        'PrivilegeInfo/principal_type': params.principalType,
        'PrivilegeInfo/principal_name': params.principalName,
        'PrivilegeInfo/permission_name': params.permissionName
      }
    });
  };

  View.getMeta = function(view_name, version) {
    return $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/'+view_name+'/versions/'+version
    });
  };

  View.checkViewVersionStatus = function(view_name, version) {
    var deferred = $q.defer();

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views/' + view_name + '/versions/' + version,
      params:{
        'fields': 'ViewVersionInfo/status'
      }
    }).then(function(data) {
      deferred.resolve(data.data.ViewVersionInfo.status);
    }).catch(function(err) {
      deferred.reject(err);
    });

    return deferred;
  };

  View.getAllVisibleInstance = function() {
    var deferred = $q.defer();
    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views',
      mock: 'view/views.json',
      params:{
        'fields': 'versions/instances/ViewInstanceInfo',
        'versions/ViewVersionInfo/system': false,
        'versions/instances/ViewInstanceInfo/visible': true
      }
    }).then(function(data) {
      var instances = [];
      data.data.items.forEach(function(view) {
        if (Array.isArray(view.versions)) {
          view.versions.forEach(function(version) {
            version.instances.forEach(function(instance) {
              instances.push(instance.ViewInstanceInfo);
            });
          });
        }
      });
      deferred.resolve(instances);
    });

    return deferred.promise;
  };

  View.all = function() {
    var deferred = $q.defer();
    var fields = [
      'versions/ViewVersionInfo/version',
      'versions/instances/ViewInstanceInfo',
      'versions/*'
    ];

    $http({
      method: 'GET',
      url: Settings.baseUrl + '/views',
      params:{
        'fields': fields.join(','),
        'versions/ViewVersionInfo/system': false
      }
    }).success(function(data) {
      var views = [];
      angular.forEach(data.items, function(item) {
        views.push(new View(item));
      });
      deferred.resolve(views);
    })
    .error(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  };
  return View;
}]);
