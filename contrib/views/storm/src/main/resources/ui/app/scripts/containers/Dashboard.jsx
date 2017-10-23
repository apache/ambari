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
import BaseContainer from './BaseContainer';
import ClusterSummary from './ClusterSummary';
import TopologyListing from './TopologyListing';
import NimbusConfigSummary from './NimbusConfigSummary';
import SupervisorSummary from './SupervisorSummary';

export default class Dashboard extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <BaseContainer>
        <div className="row" style={{marginTop: '20px'}}>
          <div className="col-sm-5">
            <ClusterSummary />
          </div>
          <div className="col-sm-7">
            <TopologyListing fromDashboard={true} />
            <SupervisorSummary fromDashboard={true} />
          </div>
        </div>
        <div className="row">
            <div className="col-sm-12">
              <NimbusConfigSummary fromDashboard={true} />
            </div>
        </div>
      </BaseContainer>
    );
  }
}
