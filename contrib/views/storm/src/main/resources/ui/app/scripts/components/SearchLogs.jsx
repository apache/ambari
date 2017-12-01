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
**/

import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import {baseUrl} from '../utils/Constants';
import {DropdownButton, FormGroup, Checkbox} from 'react-bootstrap';
import fetch from 'isomorphic-fetch';

export default class SearchLogs extends Component{
  render() {
    return (
      <div className="col-md-3 pull-right searchbar">
        <div className="input-group">
          <input type="text" id="searchBox" className="form-control" placeholder="Search in Logs"/>
          <div className="input-group-btn">
            <div className="btn-group" role="group">
              <div className="dropdown dropdown-lg">
                <DropdownButton title="" pullRight id="bg-nested-dropdown">
                  <FormGroup>
                    <Checkbox id="searchArchivedLogs">Search archived logs</Checkbox>
                  </FormGroup>
                  <FormGroup>
                    <Checkbox id="deepSearch">Deep search</Checkbox>
                  </FormGroup>
                </DropdownButton>
              </div>
              <button type="button" className="btn btn-default" onClick={this.handleSearch.bind(this)}>
                <i className="fa fa-search"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }
  handleSearch(){
    var searchBoxEl = document.getElementById('searchBox');
    var searchArchivedLogsEl = document.getElementById('searchArchivedLogs');
    var deepSearchEl = document.getElementById('deepSearch');
    var topologyId = this.props.id;

    fetch(baseUrl.replace('proxy?url=/api/v1/', 'storm_details'), {"credentials": "same-origin"})
      .then((response) => {
        return response.json();
      })
      .then((response) => {
        var url = response.hostdata+'/';
        if(deepSearchEl.checked == true){
          url += "deep_search_result.html";
        }else{
          url += "search_result.html";
        }
        url += '?search='+searchBoxEl.value+'&id='+ topologyId +'&count=1';
        if(searchArchivedLogsEl.checked == true){
          if(deepSearchEl.checked == true){
            url += "&search-archived=on";
          }else{
            url += "&searchArchived=checked";
          }
        }
        window.open(url, '_blank');

        searchBoxEl.value = '';
        searchArchivedLogsEl.checked = false;
        deepSearchEl.checked = false;
      });
  }
}
