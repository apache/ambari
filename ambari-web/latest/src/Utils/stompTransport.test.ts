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

import { describe, expect, it, vi } from "vitest";
import {
  createStompTransport,
  shouldFallbackToSockJs,
  SOCKJS_TRANSPORTS,
} from "./stompTransport";

const location = {
  host: "ambari.example:8443",
  origin: "https://ambari.example:8443",
  protocol: "https:",
};

describe("STOMP transport selection", () => {
  it("falls back only when the initial native connection has not succeeded", () => {
    expect(shouldFallbackToSockJs(false, false)).toBe(true);
    expect(shouldFallbackToSockJs(true, false)).toBe(false);
    expect(shouldFallbackToSockJs(false, true)).toBe(false);
  });

  it("opens the native websocket endpoint first", () => {
    const socket = {} as WebSocket;
    const nativeFactory = vi.fn(() => socket);
    const sockJsFactory = vi.fn();

    expect(createStompTransport(false, location, nativeFactory, sockJsFactory)).toBe(socket);
    expect(nativeFactory).toHaveBeenCalledWith(
      "wss://ambari.example:8443/api/stomp/v1/websocket",
    );
    expect(sockJsFactory).not.toHaveBeenCalled();
  });

  it("uses the classic SockJS transports after native connection failure", () => {
    const socket = {} as WebSocket;
    const nativeFactory = vi.fn();
    const sockJsFactory = vi.fn(() => socket);

    expect(createStompTransport(true, location, nativeFactory, sockJsFactory)).toBe(socket);
    expect(sockJsFactory).toHaveBeenCalledWith(
      "https://ambari.example:8443/api/stomp/v1",
      SOCKJS_TRANSPORTS,
    );
    expect(nativeFactory).not.toHaveBeenCalled();
  });
});
