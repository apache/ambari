/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import Ember from 'ember';
import CommonUtils from "../utils/common-utils";
var MappingMixin= Ember.Mixin.create({
  handleMapping(nodeDomain,nodeObj,mappings,nodeName){
    var self=this;
    mappings.forEach(function(mapping){
        console.log("mapping==",mapping);
        var nodeVals=[];
        if (mapping.mandatory){
          if (!(nodeDomain[mapping.domain] || mapping.customHandler)){
            var msgForVal=mapping.domain;
            if (mapping.displayName){
              msgForVal=mapping.displayName;
            }
            self.getContext().addError({node:{name:nodeName}, message:"Mandatory element missing for "+msgForVal});
          }
        }
        if (mapping.domain && nodeDomain[mapping.domain]){
          if (!mapping.occurs){
            mapping.occurs="once";
          }
          var objs=[];
          if (mapping.occurs==="once"){
            objs.push(mapping.ignoreValue?"":nodeDomain[mapping.domain]);
          }else{
            if (mapping.domainProperty){
              var tempObjs=[];
              nodeDomain[mapping.domain].forEach(function(value){
                tempObjs.push(value[mapping.domainProperty]);
              });
              objs=tempObjs;
            }else{
              objs=mapping.ignoreValue?"":nodeDomain[mapping.domain];
            }
          }
          if (!Ember.isArray(objs) || Ember.isArray(objs)&& objs.length>0){
            nodeObj[mapping.xml]=objs;
          }
        }else if (mapping.customHandler){
          var result=mapping.customHandler.hanldeGeneration(nodeDomain,nodeObj);
          if (result){
              nodeObj[mapping.xml]=result;
          }
        }
    });
  },

  handleImportMapping(actionNode,json,mappings){
    var domain={};
    if (json._xmlns){
      var version=CommonUtils.extractSchemaVersion(json._xmlns);
      this.schemaVersions.setActionVersion(actionNode.actionType,version);
    }
    actionNode.set("domain",domain);
    mappings.forEach(function(mapping){
      console.log("mappping==",mapping);
      if (!mapping.occurs) {
          mapping.occurs = "once";
      }
      if (mapping.domain && (json[mapping.xml] ||  json[mapping.xml]==="")){
        if (mapping.occurs==="once"){
          if (mapping.ignoreValue){
            domain[mapping.domain]=json[mapping.xml]!==null || json[mapping.xml]!==undefined;
          }else{
            domain[mapping.domain]=json[mapping.xml];
          }
        }else{
          if (!domain[mapping.domain]){
            domain[mapping.domain]=Ember.A([]);
          }
          if (Ember.isArray(json[mapping.xml])){
            if (mapping.domainProperty){
              json[mapping.xml].forEach(function(mappingVal){
                var obj={};
                obj[mapping.domainProperty]=  mappingVal;
                domain[mapping.domain].pushObject(obj);
              });
            }else{
                domain[mapping.domain].pushObjects(json[mapping.xml]);
            }
          }else{
            if(mapping.domainProperty){
              var obj = {};
              obj[mapping.domainProperty]=  json[mapping.xml];
              domain[mapping.domain].pushObject(obj);
            }else{
              domain[mapping.domain].pushObject(json[mapping.xml]);
            }
          }
        }
      }else if (mapping.customHandler){
        console.log("handling import =",mapping.customHandler);
        if (json[mapping.xml]){
          mapping.customHandler.handleImport(domain,json[mapping.xml]);
        }
      }
    });
  }
});
var ConfigurationMapper= Ember.Object.extend({
  hanldeGeneration(node,nodeObj){
    if (!node.configuration || !node.configuration.property){
      return;
    }
    var props=[];
    node.configuration.property.forEach(function(config){
         props.push({name:config.name,value:config.value});
    });
    if (props.length>0){
      var configuration={"property":props};
      return configuration;
    }
  },
  handleImport(domain,nodeObj){
    console.log("Handle import called");
    if (!nodeObj.property){
      return;
    }
    var configs=Ember.A([]);
    domain.configuration={property:configs};
    if (Ember.isArray(nodeObj.property)){
      nodeObj.property.forEach(function(prop){
        var propObj=Ember.Object.create({
           name: prop.name,
           value: prop.value
        });
        configs.pushObject(propObj);
      });
    }else{
      var propObj=Ember.Object.create({
         name: nodeObj.property.name,
         value: nodeObj.property.value
      });
      configs.pushObject(propObj);
    }
  }
});

var PrepareMapper= Ember.Object.extend({
  hanldeGeneration(node,nodeObj){
    console.log("handle prep called");
    if (node.prepare && node.prepare.length>0){
      node.prepare.sort(function(a,b){
        if (a.type==="delete"){
          return -1;
        }else{
          return 1;
        }
      });
      var prepareObjs={};
      nodeObj["prepare"]=prepareObjs;
      node.prepare.forEach(function(prep){
        if (!prepareObjs[prep.type]){
          prepareObjs[prep.type]=[];
        }
        prepareObjs[prep.type].push({"_path":prep.path});
      });
    }
  },
  handleImport(domain,nodeObj){
    console.log("Handle import called");
    domain.prepare=[];
    if (nodeObj.delete){
      this.handlePrepActionInternal(domain.prepare,nodeObj.delete,"delete");
    }
    if (nodeObj.mkdir){
      this.handlePrepActionInternal(domain.prepare,nodeObj.mkdir,"mkdir");
    }

  },
  handlePrepActionInternal(prepareDomain,actionObjs,type){
    if (Ember.isArray(actionObjs)){
      actionObjs.forEach(function(actionObj){
        var obj=Ember.Object.create({
           path: actionObj._path,
           type: type
        });
        prepareDomain.push(obj);
      });
    }else{
      var obj=Ember.Object.create({
         path: actionObjs._path,
         type: type
      });
      prepareDomain.push(obj);
    }
  }
});
export {MappingMixin,ConfigurationMapper,PrepareMapper};
