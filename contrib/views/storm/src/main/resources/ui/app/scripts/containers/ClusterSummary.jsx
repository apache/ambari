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
import RadialChart  from '../components/RadialChart';
import FSReactToastr from '../components/FSReactToastr';
import {toastOpt} from '../utils/Constants';
import TopologyREST from '../rest/TopologyREST';
import NimbusSummary from './NimbusSummary';
import CommonNotification from '../components/CommonNotification';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

export default class ClusterSummary extends Component{
  constructor(props){
    super(props);
    this.fetchData();
    this.state = {
      entity :{}
    };
  }

  fetchData = () => {
    TopologyREST.getSummary('cluster').then((result) => {
      if(result.errorMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.errorMessage}/>, '', toastOpt);
      } else {
        this.setState({entity : result});
      }
    });
  }
  render(){
    const {entity} = this.state;
    return(
      <div>
        <div className="row">
          <div className="col-sm-6">
            <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Executors are threads in a Worker process.</Tooltip>}>
              <div className="tile primary">
                  <div className="tile-header">Executor</div>
                  <div className="tile-body">
                      <i className="fa fa-play-circle-o"></i>
                      <span className="count">{entity.executorsTotal}</span>
                  </div>
              </div>
            </OverlayTrigger>
          </div>
          <div className="col-sm-6">
            <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.</Tooltip>}>
              <div className="tile warning">
                  <div className="tile-header">Tasks</div>
                  <div className="tile-body">
                      <i className="fa fa-tasks"></i>
                      <span className="count">{entity.tasksTotal}</span>
                  </div>
              </div>
            </OverlayTrigger>
          </div>
      </div>
      <div className="row">
            <div className="col-sm-6">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">The number of nodes in the cluster currently.</Tooltip>}>
                <div className="tile success">
                    <div className="tile-header" style={{textAlign:"center"}}>Supervisor</div>
                    <div className="tile-body" style={{textAlign:"center"}}>
                        <div id="supervisorCount">
                          <RadialChart
                            data={[entity.supervisors,entity.supervisors]}
                            labels={['Used','Total']}
                            width={100}
                            height={100}
                            innerRadius={46}
                            outerRadius={50}
                            color={["rgba(255,255,255,0.6)", "rgba(255,255,255,1)"]}
                          />
                        </div>
                    </div>
                </div>
              </OverlayTrigger>
            </div>
            <div className="col-sm-6">
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip1">Slots are Workers (processes).</Tooltip>}>
                <div className="tile danger">
                    <div className="tile-header" style={{textAlign:"center"}}>Slots</div>
                    <div className="tile-body" style={{textAlign:"center"}}>
                        <div id="slotsCount">
                          <RadialChart
                            data={[entity.slotsUsed,entity.slotsTotal]}
                            labels={['Used','Total']}
                            width={100}
                            height={100}
                            innerRadius={46}
                            outerRadius={50}
                            color={["rgba(255,255,255,0.6)", "rgba(255,255,255,1)"]}
                          />
                        </div>
                    </div>
                </div>
              </OverlayTrigger>
            </div>
        </div>
        <div className="row">
          <div className="col-sm-12">
            <NimbusSummary fromDashboard={true}/>
          </div>
        </div>
      </div>
    );
  }
}
