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
import _ from 'lodash';
import {Pagination} from 'react-bootstrap';

export default class CommonPagination extends Component{
  constructor(props){
    super(props);
  }

  handleSelect = (eventKey) => {
    this.props.callBackFunction(eventKey,this.props.tableName);
  }

  render(){
    const {activePage,pageSize,filteredEntities} = this.props;
    const totalPages = Math.ceil(filteredEntities.length / pageSize);

    return(
      <div className="pagination-wrapper">
        <div className="pull-left">
          <span>{`Showing ${activePage > 1 ? (activePage-1)*pageSize : activePage }  to ${activePage*pageSize > filteredEntities.length ? filteredEntities.length : (activePage*pageSize)} of ${filteredEntities.length} entries`}</span>
        </div>
        <Pagination
         className={`${filteredEntities.length === 0? 'hidden':'shown pull-right'}`}
         prev={false}
         next={false}
         first
         last
         ellipsis
         items={totalPages}
         maxButtons={5}
         activePage={activePage}
         onSelect={this.handleSelect}>
      </Pagination>
      </div>
    );
  }
}
