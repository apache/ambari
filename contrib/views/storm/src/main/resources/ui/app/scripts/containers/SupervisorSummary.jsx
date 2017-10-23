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
import RadialChart  from '../components/RadialChart';
import FSReactToastr from '../components/FSReactToastr';
import {toastOpt, pageSize} from '../utils/Constants';
import TopologyREST from '../rest/TopologyREST';
import CommonNotification from '../components/CommonNotification';
import Breadcrumbs from '../components/Breadcrumbs';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import CommonPagination from '../components/CommonPagination';
import Utils from '../utils/Utils';
import Footer from '../components/Footer';

export default class SupervisorSummary extends Component{
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
    TopologyREST.getSummary('supervisor').then((results) => {
      if(results.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.errorMessage}/>, '', toastOpt);
      } else {
        this.setState({entities : results.supervisors});
      }
    });
  }

  getLinks(){
    var links = [
      {link: '#/', title: 'Dashboard'},
      {link: '#/supervisor', title: 'Supervisor Summary'}
    ];
    return links;
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
                <h4>Supervisor Summary</h4>
                {fromDashboard ?
                <div className="box-control">
                    <a className="primary" href="#/supervisor"><i className="fa fa-external-link"></i></a>
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
              <Table className="table no-margin supervisor-table" noDataText="No records found." currentPage={0} >
                <Thead>
                  <Th column="host"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.)</Tooltip>}><span>Host</span></OverlayTrigger></Th>
                  <Th column="slots"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Slots are Workers (processes).</Tooltip>}><span>Slots</span></OverlayTrigger></Th>
                  <Th column="cpu"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">CPU that has been allocated.</Tooltip>}><span>CPU</span></OverlayTrigger></Th>
                  <Th column="memory"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Memory that has been allocated.</Tooltip>}><span>Memory</span></OverlayTrigger></Th>
                  <Th column="uptime"><OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The length of time a Supervisor has been registered to the cluster.</Tooltip>}><span>Uptime</span></OverlayTrigger></Th>
                </Thead>
                {
                  _.map(filteredEntities, (entity, i) => {
                    return (
                      <Tr key={i}>
                        <Td column="host"><a href={entity.logLink} target="_blank">{entity.host}</a></Td>
                        <Td column="slots">
                          <RadialChart
                            data={[entity.slotsUsed,entity.slotsTotal]}
                            labels={['Used','Total']}
                            innerRadius={19}
                            outerRadius={21}
                            color={["#bcbcbc", "#235693"]}
                          />
                        </Td>
                        <Td column="cpu">
                          <RadialChart
                            data={[entity.usedCpu,entity.totalCpu]}
                            labels={['Used','Total']}
                            innerRadius={19}
                            outerRadius={21}
                            color={["#bcbcbc", "#235693"]}
                          />
                        </Td>
                        <Td column="memory">
                          <RadialChart
                            data={[entity.usedMem,entity.totalMem]}
                            labels={['Used','Total']}
                            innerRadius={19}
                            outerRadius={21}
                            color={["#bcbcbc", "#235693"]}
                          />
                        </Td>
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
        {!fromDashboard ? <Footer /> : null}
      </div>
    );
  }
}
