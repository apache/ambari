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

import SockJS from "sockjs-client";

export const SOCKJS_TRANSPORTS = [
  "eventsource",
  "xhr-polling",
  "iframe-xhr-polling",
  "jsonp-polling",
];

export function shouldFallbackToSockJs(
  useSockJs: boolean,
  hasConnected: boolean,
): boolean {
  return !useSockJs && !hasConnected;
}

type BrowserLocation = Pick<Location, "host" | "origin" | "protocol">;
type NativeFactory = (url: string) => WebSocket;
type SockJsFactory = (url: string, transports: string[]) => WebSocket;

export function createStompTransport(
  useSockJs: boolean,
  location: BrowserLocation,
  nativeFactory: NativeFactory = (url) => new WebSocket(url),
  sockJsFactory: SockJsFactory = (url, transports) => new SockJS(
    url,
    undefined,
    { transports },
  ) as unknown as WebSocket,
): WebSocket {
  if (useSockJs) {
    return sockJsFactory(`${location.origin}/api/stomp/v1`, SOCKJS_TRANSPORTS);
  }
  const protocol = location.protocol === "https:" ? "wss:" : "ws:";
  return nativeFactory(`${protocol}//${location.host}/api/stomp/v1/websocket`);
}
