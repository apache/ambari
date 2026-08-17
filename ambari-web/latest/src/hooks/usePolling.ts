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
  const savedCallback = useRef<Function | undefined>(undefined);
  const timeoutId = useRef<NodeJS.Timeout | null>(null);
  const isPausedRef = useRef(false);
  const isActiveRef = useRef(false);
  const [isPaused, setIsPaused] = useState(false);

  const stopPolling = useCallback(() => {
    if (timeoutId.current) {
      clearTimeout(timeoutId.current);
      timeoutId.current = null;
    }
  }, []);

  const pausePolling = useCallback(() => {
    setIsPaused(true);
    stopPolling();
  }, [stopPolling]);

  const resumePolling = useCallback(() => {
    setIsPaused(false);
  }, []);

  useEffect(() => {
    isPausedRef.current = isPaused;
  }, [isPaused]);

  // Remember the latest callback.
  useEffect(() => {
    savedCallback.current = apiFunction;
  }, [apiFunction]);

  // Set up the timeout-based polling.
  useEffect(() => {
    isActiveRef.current = true;

    async function tick() {
      if (!savedCallback.current || isPausedRef.current || !isActiveRef.current) {
        return;
      }

      try {
        // Execute the API call and wait for it to complete
        await Promise.resolve(savedCallback.current());
      } catch (error) {
        // Log error but don't break polling
        console.error('Polling error:', error);
      } finally {
        // Schedule next poll only after current request completes
        if (isActiveRef.current && !isPausedRef.current && interval !== null) {
          timeoutId.current = setTimeout(tick, interval);
        }
      }
    }

    // Start polling if not paused
    if (!isPausedRef.current && interval !== null) {
      // Initial call
      tick();
    }

    // Cleanup on unmount or when dependencies change
    return () => {
      isActiveRef.current = false;
      stopPolling();
    };
  }, [interval, isPaused, stopPolling]);

  return { stopPolling, pausePolling, resumePolling, isPaused };
}

export default usePolling;
