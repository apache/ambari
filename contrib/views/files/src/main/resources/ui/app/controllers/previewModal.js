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

App.PreviewModalController = Em.ObjectController.extend({
    needs:['files', 'file'],
    offset: 3000 ,
    startIndex:0,
    file:Em.computed.alias('content'),
    filePageText:'',
    reload: false,
    pagecontent: Ember.computed('file', 'startIndex', 'endIndex', 'reload', function() {
        var file = this.get('file');
        var filepath = file.get('path');
        var filePageText = this.get('filePageText');

        var self = this,
            defer = Ember.RSVP.defer(),
            startIndex = this.get('startIndex'),
            endIndex  = this.get('endIndex');

        var pathName = window.location.pathname;
        var pathNameArray = pathName.split("/");
        var ViewVersion = pathNameArray[3];
        var viewName = pathNameArray[4];
        var previewServiceURL = "/api/v1/views/FILES/versions/"+ ViewVersion + "/instances/" + viewName + "/resources/files/preview/file" + '?path=' + filepath + '&start='+ startIndex +'&end='+ endIndex;

        var previousText = $('.preview-content').text();

        $.ajax({
            url: previewServiceURL,
            dataType: "json",
            type: 'get',
            async: false,
            contentType: 'application/json',
            success: function( response, textStatus, jQxhr ){
                self.set('filePageText', previousText + response.data);
                self.set('isFileEnd',response.isFileEnd);
            },
            error: function( jqXhr, textStatus, errorThrown ){
                console.log( "Preview Fail pagecontent : " + errorThrown );
              self.send('removePreviewModal');
              self.send('showAlert', jqXhr);
              self.set('reload', !self.get('reload'));
            }
        });

        if(self.get('isFileEnd') == true){
           this.set('showNext', false);
        }
        return self.get('filePageText');
    }),
    endIndex: Ember.computed('startIndex', 'offset', function() {
        var startIndex = this.get('startIndex'),
            offset  = this.get('offset');
        return startIndex + offset;
    }),
    showPrev : Ember.computed('startIndex', function() {
        var startIndex = this.get('startIndex');
        this.set('showNext', true);
        return ((startIndex == 0) ? false : true );
    }),
    showNext : true,
    actions:{
        next: function(){
            console.log('Next');
            this.set('startIndex', this.get('startIndex') + this.get('offset'));
            return this.get('filePageText');
        },
        prev: function(){
            console.log('Prev');
            this.set('startIndex', (this.get('startIndex') - this.get('offset')) > 0 ? (this.get('startIndex') - this.get('offset')) : 0);
        }
    }
});
