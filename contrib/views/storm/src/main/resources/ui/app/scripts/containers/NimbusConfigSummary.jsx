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
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import FSReactToastr from '../components/FSReactToastr';
import {toastOpt,pageSize} from '../utils/Constants';
import TopologyREST from '../rest/TopologyREST';
import CommonNotification from '../components/CommonNotification';
import Utils from '../utils/Utils';
import CommonPagination from '../components/CommonPagination';
import {Accordion, Panel} from 'react-bootstrap';
import CommonExpanded from '../components/CommonExpanded';

export default class NimbusConfigSummary extends Component{
  constructor(props){
    super(props);
    this.fetchData();
    this.state = {
      entity : [],
      filterValue: '',
      collapse : true,
      activePage : 1,
      expandPanel : false
    };
  }

  fetchData = () => {
    TopologyREST.getClusterConfig().then((result) => {
      if(result.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        this.setState({entity : result});
      }
    });
  }

  handleFilter = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleCollapseClick = (e) => {
    this.setState({collapse : !this.state.collapse});
  }

  callBackFunction = (eventKey) => {
    this.setState({activePage : eventKey});
  }

  onSelectFunction = (type) => {
    let tempState = _.cloneDeep(this.state);
    tempState[type] = !tempState[type];
    this.setState(tempState);
  }

  render(){
    const {entity,collapse,filterValue,activePage,expandPanel} = this.state;
    const filteredEntities = Utils.filterByKey(_.keys(entity), filterValue);
    const paginationObj = {
      activePage,
      pageSize,
      filteredEntities
    };

    const panelHeader = <h4>Nimbus Configuration
                        <CommonExpanded  expandFlag={expandPanel}/></h4>;

    return(
      <Accordion>
        <Panel header={panelHeader} eventKey="1" expanded={expandPanel} onSelect={this.onSelectFunction.bind(this,'expandPanel')}>
          <div className="input-group col-sm-4">
            <input type="text" onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Key" />
            <span className="input-group-btn">
            <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
            </span>
          </div>
          <Table className="table no-margin"  noDataText="No nimbus configuration found !"  currentPage={activePage-1} itemsPerPage={pageSize}>
            <Thead>
              <Th column="Key">Key</Th>
              <Th column="value">Value</Th>
            </Thead>
            {
              _.map(filteredEntities, (k,i) => {
                return(
                  <Tr key={i}>
                    <Td column="Key">{k}</Td>
                    <Td column="value">{entity[k]}</Td>
                  </Tr>
                );
              })
            }
          </Table>
          {
            filteredEntities.length !== 0
            ? <CommonPagination  {...paginationObj} callBackFunction={this.callBackFunction.bind(this)}/>
            : ''
          }
        </Panel>
      </Accordion>
    );
  }
}
