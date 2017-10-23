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
import Select from 'react-select';
import CommonSwitchComponent from './CommonSwitchComponent';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

export default class CommonWindowPanel extends Component{
  constructor(props){
    super(props);
  }

  windowChange = (obj) => {
    this.props.handleWindowChange(obj);
  }

  commonToggleChange = (params) => {
    this.props.toggleSystem(params);
  }

  commonTopologyActionHandler = (action) => {
    this.props.handleTopologyAction(action);
  }

  render(){
    const {selectedWindowKey,windowOptions,systemFlag,debugFlag,handleLogLevel,topologyStatus,KYC,handleProfiling} = this.props;
    return(
      <div className="form-group no-margin">
        <label className="col-sm-1 control-label">Window</label>
        <div className="col-sm-2">
          <Select value={selectedWindowKey} options={windowOptions} onChange={this.windowChange.bind(this)} valueKey="label" labelKey="label" clearable={false}/>
        </div>
        <label className="col-sm-2 control-label">System Summary</label>
        <div className="col-sm-2">
          <CommonSwitchComponent checked={systemFlag} switchCallBack={this.commonToggleChange.bind(this,'systemFlag')}/>
        </div>
        <label className="col-sm-1 control-label">Debug</label>
        <div className="col-sm-1">
          <CommonSwitchComponent checked={debugFlag} switchCallBack={this.commonToggleChange.bind(this,'debugFlag')}/>
        </div>
        <div className="col-sm-3 text-right">
          <div className="btn-group" role="group">
            {
              KYC === 'detailView'
              ? [ <OverlayTrigger  key={1} placement="top" overlay={<Tooltip id="tooltip1">Activate</Tooltip>}>
                    <button type="button" className="btn btn-primary" onClick={this.commonTopologyActionHandler.bind(this,'activate')} disabled={topologyStatus === 'ACTIVE' ? "disabled" : null}>
                      <i className="fa fa-play"></i>
                    </button>
                  </OverlayTrigger>,
                <OverlayTrigger key={2}  placement="top" overlay={<Tooltip id="tooltip1">Deactivate</Tooltip>}>
                  <button type="button" className="btn btn-primary" onClick={this.commonTopologyActionHandler.bind(this,'deactivate')}  disabled={topologyStatus === 'INACTIVE' ? "disabled" : null}>
                    <i className="fa fa-stop"></i>
                  </button>
                </OverlayTrigger>,
                <OverlayTrigger key={3} placement="top" overlay={<Tooltip id="tooltip1">Rebalance</Tooltip>}>
                  <button type="button" className="btn btn-primary" onClick={this.commonTopologyActionHandler.bind(this,'rebalance')} disabled={topologyStatus === 'REBALANCING' ? "disabled" : null}>
                    <i className="fa fa-balance-scale"></i>
                  </button>
                </OverlayTrigger>,
                <OverlayTrigger  key={4} placement="top" overlay={<Tooltip id="tooltip1">Kill</Tooltip>}>
                  <button type="button" className="btn btn-primary" onClick={this.commonTopologyActionHandler.bind(this,'kill')} disabled={topologyStatus === 'KILLED' ? "disabled" : null}>
                    <i className="fa fa-ban"></i>
                  </button>
                </OverlayTrigger>,
                <OverlayTrigger key={5} placement="top" overlay={<Tooltip id="tooltip1">Change Log Level</Tooltip>}>
                  <button  type="button" className="btn btn-primary" onClick={handleLogLevel}>
                    <i className="fa fa-file-o"></i>
                  </button>
                </OverlayTrigger>
              ]
              : <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip1">Profiling & Debugging</Tooltip>}>
                  <button type="button" className="btn btn-primary" onClick={handleProfiling}>
                   <i className="fa fa-cogs"></i>
                 </button>
                </OverlayTrigger>

            }
          </div>
        </div>
      </div>
    );
  }
}
