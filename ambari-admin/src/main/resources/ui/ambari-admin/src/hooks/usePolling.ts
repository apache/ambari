/* eslint-disable @typescript-eslint/ban-types */
import { useEffect, useRef } from 'react';

function usePolling(apiFunction:Function, interval=10000) {
  const savedCallback = useRef<Function>();

  // Remember the latest callback.
  useEffect(() => {
    savedCallback.current = apiFunction;
  }, [apiFunction]);

// Set up the interval.
  useEffect(() => {
    function tick() {
      if (savedCallback.current) {
        savedCallback.current?.();
      }
    }
    if (interval !== null) {
      const id = setInterval(tick, interval);
      return () => clearInterval(id);
    }
  }, [interval]);
}

export default usePolling