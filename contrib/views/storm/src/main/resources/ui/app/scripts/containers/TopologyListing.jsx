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
import {toastOpt} from '../utils/Constants';
import TopologyREST from '../rest/TopologyREST';
import CommonNotification from '../components/CommonNotification';
import {Link} from 'react-router';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import Breadcrumbs from '../components/Breadcrumbs';
import CommonPagination from '../components/CommonPagination';
import {pageSize} from '../utils/Constants';
import Utils from '../utils/Utils';
import Footer from '../components/Footer';

export default class TopologyListing extends Component{
  constructor(props){
    super(props);
    this.fetchData();
    this.state = {
      entities : [],
      filterValue: '',
      activePage: 1
    };
  }

  fetchData = () => {
    TopologyREST.getSummary('topology').then((results) => {
      if(results.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.errorMessage}/>, '', toastOpt);
      } else {
        let stateObj={};
        stateObj.entities = results.topologies;
        if(!this.props.fromDashboard){
          var additionalColumns = [
            {name: 'assignedTotalMem', title: 'Memory Assigned (MB)'},
            {name: 'workersTotal', title: 'Workers'},
            {name: 'executorsTotal', title: 'Executors'},
            {name: 'tasksTotal', title: 'Tasks'},
            {name: 'owner', title: 'Owner'}
          ];
          Array.prototype.push.apply(stateObj.entities, additionalColumns);
        }
        this.setState({entities : stateObj.entities});
      }
    });
  }

  getLinks(){
    var links = [
      {link: '#/', title: 'Dashboard'},
      {link: '#/topology', title: 'Topology Listing'}
    ];
    return links;
  }

  activeClass = (status) => {
    let classname="label ";
    switch(status){
    case 'ACTIVE':
      classname += "label-success";
      break;
    case 'INACTIVE':
      classname += "label-default";
      break;
    case 'REBALANCING':
      classname += "label-warning";
      break;
    case 'KILLED':
      classname += "label-danger";
      break;
    default:
      classname += "label-primary";
      break;
    }
    return classname;
  }

  handleFilter = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  callBackFunction = (eventKey) => {
    this.setState({activePage : eventKey});
  }

  render(){
    const {entities, filterValue, activePage} = this.state;
    const {fromDashboard} = this.props;
    const topologies = _.filter(entities, (e)=>{return e.id !== undefined;});
    const filteredEntities = Utils.filterByKey(topologies, filterValue, 'name');
    const paginationObj = {
      activePage,
      pageSize,
      filteredEntities
    };
    return(
      <div className={fromDashboard ? "" : "container-fluid"}>
        {!fromDashboard ? <Breadcrumbs links={this.getLinks()} /> : ''}
        <div className="box">
            <div className="box-header">
                <h4>Topology Listing</h4>
                {fromDashboard ?
                <div className="box-control">
                    <a className="primary" href="#/topology"><i className="fa fa-external-link"></i></a>
                </div>
                : ''}
            </div>
            <div className={fromDashboard ? "box-body paddless" : "box-body"}>
              {!fromDashboard ?
              <div className="input-group col-sm-4">
                <input type="text" onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Topology Name" />
                <span className="input-group-btn">
                <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
                </span>
              </div>
              : ''}
              <Table className="table topology-table" noDataText="No topology found." currentPage={0} >
                <Thead>
                  <Th column="topologyName"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The name given to the topology by when it was submitted. Click the name to view the Topology's information.</Tooltip>}><span>Topology Name</span></OverlayTrigger></Th>
                  <Th column="status"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The status can be one of ACTIVE, INACTIVE, KILLED, or REBALANCING.</Tooltip>}><span>Status</span></OverlayTrigger></Th>
                  {
                    !fromDashboard
                    ? [
                      <Th key={3} column="assignedTotalMem">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Assigned Total Memory by Scheduler.</Tooltip>}>
                          <span>Memory Assigned (MB)</span>
                        </OverlayTrigger></Th>,
                      <Th key={4} column="workersTotal">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of Workers (processes).</Tooltip>}>
                           <span>Workers</span>
                        </OverlayTrigger>
                      </Th>,
                      <Th key={5} column="executorsTotal">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Executors are threads in a Worker process.</Tooltip>}>
                           <span>Executors</span>
                        </OverlayTrigger>
                      </Th>,
                      <Th key={6} column="tasksTotal">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.</Tooltip>}>
                           <span>Tasks</span>
                        </OverlayTrigger>
                      </Th>,
                      <Th key={7} column="owner" title="">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The user that submitted the Topology, if authentication is enabled.</Tooltip>}>
                           <span>Owner</span>
                        </OverlayTrigger>
                      </Th>,
                      <Th key={8} column="uptime">
                        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The time since the Topology was submitted.</Tooltip>}>
                           <span>Uptime</span>
                        </OverlayTrigger>
                      </Th>
                    ]
                    : <Th column="uptime"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The time since the Topology was submitted.</Tooltip>}><span>Uptime</span></OverlayTrigger></Th>
                  }
                </Thead>
                {
                  _.map(filteredEntities, (entity, i) => {
                    return (
                      <Tr key={i}>
                        <Td column="topologyName"><Link to={"topology/"+entity.id}>{entity.name}</Link></Td>
                        <Td column="status"><span className={this.activeClass(entity.status)}>{entity.status}</span></Td>
                        {
                          !fromDashboard
                          ? [
                            <Td key={i+'assignedTotalMem'} column="assignedTotalMem">{entity.assignedTotalMem}</Td>,
                            <Td key={i+'workersTotal'} column="workersTotal">{entity.workersTotal}</Td>,
                            <Td key={i+'executorsTotal'} column="executorsTotal">{entity.executorsTotal}</Td>,
                            <Td key={i+'tasksTotal'} column="tasksTotal">{entity.tasksTotal}</Td>,
                            <Td key={i+'owner'} column="owner">{entity.owner}</Td>
                          ]
                          : ''
                        }
                        <Td column="uptime"><small>{entity.uptime}</small></Td>
                      </Tr>
                    );
                  })
                }
              </Table>
              {
                !fromDashboard && filteredEntities.length !== 0
                ? <CommonPagination  {...paginationObj} callBackFunction={this.callBackFunction.bind(this)}/>
                : ''
              }
            </div>
        </div>
        {
          !fromDashboard
          ? <Footer />
          : null
        }
      </div>
    );
  }
}
