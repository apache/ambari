/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useContext, useMemo } from "react";
import { ServiceContext } from "../../../store/ServiceContext";
import ChartContainer from "../ChartContainer";
import { isEmpty } from "lodash";

export default function NameNodeUptime() {
  const { allServiceModels } = useContext(ServiceContext);

  const modelValue = isEmpty(allServiceModels["hdfs"]?.["namenodeUptime"]) ? 0 : allServiceModels["hdfs"]?.["namenodeUptime"];

  const parseUptimeToMilliseconds = (uptimeStr: string): number => {
    if (!uptimeStr || uptimeStr === "Not Running") return 0;
    
    let totalMs = 0;
    const dayMatch = uptimeStr.match(/(\d+)d/);
    const hourMatch = uptimeStr.match(/(\d+)h/);
    const minuteMatch = uptimeStr.match(/(\d+)m/);
    const secondMatch = uptimeStr.match(/(\d+)s/);
    
    if (dayMatch) totalMs += parseInt(dayMatch[1]) * 24 * 60 * 60 * 1000;
    if (hourMatch) totalMs += parseInt(hourMatch[1]) * 60 * 60 * 1000;
    if (minuteMatch) totalMs += parseInt(minuteMatch[1]) * 60 * 1000;
    if (secondMatch) totalMs += parseInt(secondMatch[1]) * 1000;
    
    return totalMs;
  };

  const getStartTime = (uptimeStr: string): string => {
    const uptimeMs = parseUptimeToMilliseconds(uptimeStr);
    if (uptimeMs === 0) return "";
    
    const now = new Date();
    const startTime = new Date(now.getTime() - uptimeMs);
    
    const weekday = startTime.toLocaleDateString('en-US', { weekday: 'short' });
    const month = startTime.toLocaleDateString('en-US', { month: 'short' });
    const day = startTime.getDate();
    const year = startTime.getFullYear();
    const hours = String(startTime.getHours()).padStart(2, '0');
    const minutes = String(startTime.getMinutes()).padStart(2, '0');
    const seconds = String(startTime.getSeconds()).padStart(2, '0');
    
    return `${weekday} ${month} ${day} ${year}\n${hours}:${minutes}:${seconds}`;
  };

  const getUptime = () => {
    if (!modelValue) return "Not Running";
    return modelValue;
  };

  const uptime = getUptime();
  
  const startTime = useMemo(() => {
    return getStartTime(uptime);
  }, [uptime]);

  const dataToDisplay = startTime 
    ? `${uptime}\n${startTime}`
    : `${uptime}`;

  return (
    <ChartContainer
      text={uptime}
      onHoverContent={dataToDisplay}
    >
      <div className="p-3"></div>
    </ChartContainer>
  );
}
