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

/* eslint-disable @typescript-eslint/ban-types */
import { useEffect, useRef, useCallback, useState } from 'react';

function usePolling(apiFunction: Function, interval = 2000) {
  const savedCallback = useRef<Function>();
  const intervalId = useRef<NodeJS.Timeout | null>(null);
  const [isPaused, setIsPaused] = useState(false);

  const stopPolling = useCallback(() => {
    if (intervalId.current) {
      clearInterval(intervalId.current);
      intervalId.current = null;
    }
  }, []);

  const pausePolling = useCallback(() => {
    setIsPaused(true);
    stopPolling();
  }, [stopPolling]);

  const resumePolling = useCallback(() => {
    setIsPaused(false);
  }, []);

  // Remember the latest callback.
  useEffect(() => {
    savedCallback.current = apiFunction;
  }, [apiFunction]);

  // Set up the interval.
  useEffect(() => {
    function tick() {
      if (savedCallback.current) {
        savedCallback.current();
      }
    }
    
    if (!isPaused && interval !== null) {
      intervalId.current = setInterval(tick, interval);
      return () => stopPolling();
    }
  }, [interval, isPaused, stopPolling]);

  return { stopPolling, pausePolling, resumePolling, isPaused };
}

export default usePolling;