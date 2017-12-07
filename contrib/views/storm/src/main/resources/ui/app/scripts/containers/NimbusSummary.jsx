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
import {toastOpt, pageSize} from '../utils/Constants';
import TopologyREST from '../rest/TopologyREST';
import CommonNotification from '../components/CommonNotification';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import Breadcrumbs from '../components/Breadcrumbs';
import CommonPagination from '../components/CommonPagination';
import Utils from '../utils/Utils';
import Footer from '../components/Footer';

export default class NimbusSummary extends Component{
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
    TopologyREST.getSummary('nimbus').then((results) => {
      if(results.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.errorMessage}/>, '', toastOpt);
      } else {
        this.setState({entities : results.nimbuses});
      }
    });
  }

  getLinks(){
    var links = [
      {link: '#/', title: 'Dashboard'},
      {link: '#/nimbus', title: 'Nimbus Summary'}
    ];
    return links;
  }

  activeClass = (status) => {
    let classname="label ";
    switch(status){
    case 'Leader':
      classname += "label-success";
      break;// case 'Follower':
    //   classname += "label-warning";
    //   break;default:
      classname += "label-warning";
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
    const filteredEntities = Utils.filterByKey(entities, filterValue, 'host');
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
                <h4>Nimbus Summary</h4>
                {fromDashboard ?
                <div className="box-control">
                  <a className="primary" href="#/nimbus"><i className="fa fa-external-link"></i></a>
                </div>
                : ''}
            </div>
            <div className={fromDashboard ? "box-body paddless" : "box-body"}>
              {!fromDashboard ?
              <div className="input-group col-sm-4">
                <input type="text" onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Host" />
                <span className="input-group-btn">
                <button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
                </span>
              </div>
              : ''}
              <Table className="table topology-table" noDataText="No records found." currentPage={0} >
                <Thead>
                  <Th column="host:Port"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Nimbus hostname and port number</Tooltip>}><span>Host:Port</span></OverlayTrigger></Th>
                  <Th column="status"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Leader if this host is leader, Not a Leader for all other live hosts, note that these hosts may or may not be in leader lock queue, and Dead for hosts that are part of nimbus.seeds list but are not alive.</Tooltip>}><span>Status</span></OverlayTrigger></Th>
                  <Th column="uptime"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Time since this nimbus host has been running.</Tooltip>}><span>Uptime</span></OverlayTrigger></Th>
                </Thead>
                {
                  _.map(filteredEntities, (entity, i) => {
                    return (
                      <Tr key={i}>
                        <Td column="host:Port"><a href={entity.nimbusLogLink} target="_blank">{entity.host+':'+entity.port}</a></Td>
                        <Td column="status"><span className={this.activeClass(entity.status)}>{entity.status}</span></Td>
                        <Td column="uptime"><small>{entity.nimbusUpTime}</small></Td>
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
        {!fromDashboard ? <Footer /> : null}
      </div>
    );
  }
}
