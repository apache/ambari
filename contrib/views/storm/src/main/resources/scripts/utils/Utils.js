/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['require',
    'react',
    'react-dom',
    'bootbox',
    'bootstrap',
    'bootstrap-notify'], function(require, React, ReactDOM, bootbox) {
    'use strict';
    var Utils = {};

    Utils.ArrayToCollection = function(array, collection){
        if(array.length){
            array.map(function(obj){
                collection.add(new Backbone.Model(obj));
            });
        }
        return collection;
    };

    Utils.ConfirmDialog = function(message, title, successCallback, cancelCallback) {
        bootbox.dialog({
            message: message,
            title: title,
            className: 'confirmation-dialog',
            buttons: {
                cancel: {
                    label: 'No',
                    className: 'btn-default btn-small',
                    callback: cancelCallback ? cancelCallback : function(){}
                },
                success: {
                    label: 'Yes',
                    className: 'btn-success btn-small',
                    callback: successCallback
                }
            }
        });
    };

    Utils.notifyError = function(message) {
        $.notify({
            icon: 'fa fa-warning',
            message: message
        },{
            type: 'danger',
            allow_dismiss: true,
            animate: {
                enter: 'animated fadeInDown',
                exit: 'animated fadeOutUp'
            }
        });
    };
    Utils.notifySuccess = function(message) {
        $.notify({
            icon: 'fa fa-check',
            message: message
        },{
            type: 'success',
            allow_dismiss: true,
            animate: {
                enter: 'animated fadeInDown',
                exit: 'animated fadeOutUp'
            }
        });
    };

    Utils.notifyInfo = function(message) {
        $.notify({
            icon: 'fa fa-info',
            message: message
        },{
            type: 'info',
            allow_dismiss: true,
            animate: {
                enter: 'animated fadeInDown',
                exit: 'animated fadeOutUp'
            }
        });
    };

    Utils.notifyWarning = function(message) {
        $.notify({
            icon: 'fa fa-warning',
            message: message
        },{
            type: 'warning',
            allow_dismiss: true,
            animate: {
                enter: 'animated fadeInDown',
                exit: 'animated fadeOutUp'
            }
        });
    };

    return Utils;
});
