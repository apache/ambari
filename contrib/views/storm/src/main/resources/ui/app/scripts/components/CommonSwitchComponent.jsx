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

export default class CommonSwitchComponent extends Component {
  render(){
    const {switchCallBack,checked,textON,textOFF,KYC} = this.props;
    let switchId = "switch-"+((Math.random())*100).toFixed(0);
    return (
      <div className={`switchWrapper ${!!KYC ? 'lagSwitchSetting pull-right' : ''}`}>
        <span className={`switchSlider ${checked ?  'onSlider' : 'offSlider'}`} onClick={switchCallBack}>
          <span className={`switchItemOn sliderText ${!!KYC ? 'graphSwitchOn' : ''}`}>{textON}</span>
          <span className="switchItemMid"></span>
          <span className={`switchItemOff sliderText ${!!KYC ? 'graphSwitchOff' : ''}`}>{textOFF}</span>
        </span>
      </div>

    );
  }
}

CommonSwitchComponent.defaultProps = {
  textON : "ON",
  textOFF : "OFF"
};
