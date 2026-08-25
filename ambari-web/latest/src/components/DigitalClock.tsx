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

import { useEffect, useState } from "react";
import { formatServerClock, normalizeServerClock } from "../Utils/serverClock";

type DigitalClockProps = {
  serverClock?: number | string | null;
  timeZone?: string;
};

export default function DigitalClock({
  serverClock,
  timeZone,
}: DigitalClockProps) {
  const [currentTime, setCurrentTime] = useState(Date.now());

  useEffect(() => {
    const clientTime = Date.now();
    const normalizedServerClock = normalizeServerClock(serverClock);
    const offset = normalizedServerClock === null
      ? 0
      : normalizedServerClock - clientTime;
    const update = () => setCurrentTime(Date.now() + offset);
    update();
    const interval = window.setInterval(update, 1000);
    return () => window.clearInterval(interval);
  }, [serverClock]);

  return (
    <time
      className="navbar-text navbar-size"
      data-testid="server-clock"
      dateTime={new Date(currentTime).toISOString()}
      title="Ambari Server time"
    >
      {formatServerClock(currentTime, timeZone)}
    </time>
  );
}
