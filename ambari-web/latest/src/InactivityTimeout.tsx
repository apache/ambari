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

import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Modal from "./components/Modal";
import { AppContext } from "./store/context";
import { useAuth } from "./hooks/useAuth";

const WARNING_SECONDS = 60;
const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = ["mousemove", "keypress", "click"];

export function inactivityTimeoutSeconds(
  properties: Record<string, unknown> | undefined,
  admin: boolean,
): number {
  const value = Number(properties?.[
    admin
      ? "user.inactivity.timeout.default"
      : "user.inactivity.timeout.role.readonly.default"
  ] || 0);
  return Number.isFinite(value) && value > 0 ? value : 0;
}

export function isInactivityRouteExcluded(pathname: string): boolean {
  return pathname.includes("/step") || pathname.includes("/stack/upgrade");
}

export function remainingInactivitySeconds(
  timeoutSeconds: number,
  lastActivityTime: number,
  currentTime: number,
): number {
  return timeoutSeconds - Math.floor((currentTime - lastActivityTime) / 1000);
}

export default function InactivityTimeout() {
  const { ambariProperties } = useContext(AppContext);
  const { isAdmin, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const lastActivity = useRef(Date.now());
  const warningVisible = useRef(false);
  const signingOut = useRef(false);

  const timeoutSeconds = inactivityTimeoutSeconds(ambariProperties, isAdmin());
  const isExcludedRoute = isInactivityRouteExcluded(location.pathname);

  const updateRemainingSeconds = useCallback((value: number | null) => {
    warningVisible.current = value !== null;
    setRemainingSeconds(value);
  }, []);

  const signOut = useCallback(async () => {
    if (signingOut.current) {
      return;
    }
    signingOut.current = true;
    await logout();
    navigate("/login", { replace: true });
  }, [logout, navigate]);

  useEffect(() => {
    if (timeoutSeconds <= 0) {
      return;
    }

    const markActive = () => {
      if (!warningVisible.current) {
        lastActivity.current = Date.now();
      }
    };
    const bindFrame = (frame: HTMLIFrameElement, shouldBind: boolean) => {
      try {
        ACTIVITY_EVENTS.forEach((eventName) => {
          if (shouldBind) {
            frame.contentWindow?.addEventListener(eventName, markActive);
          } else {
            frame.contentWindow?.removeEventListener(eventName, markActive);
          }
        });
      } catch {
        // Cross-origin View iframes cannot be observed by the parent application.
      }
    };
    const trackedFrames = new Map<HTMLIFrameElement, () => void>();
    const trackFrame = (frame: HTMLIFrameElement) => {
      if (trackedFrames.has(frame)) {
        return;
      }
      const bindLoadedFrame = () => bindFrame(frame, true);
      trackedFrames.set(frame, bindLoadedFrame);
      frame.addEventListener("load", bindLoadedFrame);
      bindLoadedFrame();
    };
    const untrackFrame = (frame: HTMLIFrameElement) => {
      const bindLoadedFrame = trackedFrames.get(frame);
      if (!bindLoadedFrame) {
        return;
      }
      bindFrame(frame, false);
      frame.removeEventListener("load", bindLoadedFrame);
      trackedFrames.delete(frame);
    };
    const visitFrames = (node: Node, callback: (frame: HTMLIFrameElement) => void) => {
      if (!(node instanceof Element)) {
        return;
      }
      if (node instanceof HTMLIFrameElement) {
        callback(node);
      }
      node.querySelectorAll("iframe").forEach(callback);
    };

    ACTIVITY_EVENTS.forEach((eventName) => window.addEventListener(eventName, markActive));
    document.querySelectorAll("iframe").forEach(trackFrame);
    const frameObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => visitFrames(node, trackFrame));
        mutation.removedNodes.forEach((node) => visitFrames(node, untrackFrame));
      });
    });
    frameObserver.observe(document.body, { childList: true, subtree: true });
    const intervalId = window.setInterval(() => {
      if (isExcludedRoute) {
        lastActivity.current = Date.now();
        updateRemainingSeconds(null);
        return;
      }

      const remaining = remainingInactivitySeconds(
        timeoutSeconds,
        lastActivity.current,
        Date.now(),
      );
      if (remaining <= 0) {
        void signOut();
      } else if (remaining <= WARNING_SECONDS) {
        updateRemainingSeconds(remaining);
      }
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
      frameObserver.disconnect();
      ACTIVITY_EVENTS.forEach((eventName) => window.removeEventListener(eventName, markActive));
      Array.from(trackedFrames.keys()).forEach(untrackFrame);
    };
  }, [isExcludedRoute, signOut, timeoutSeconds, updateRemainingSeconds]);

  if (remainingSeconds === null) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={() => undefined}
      modalTitle="Session Expiration"
      modalBody={(
        <p>
          Your session will expire in <strong>{remainingSeconds}</strong> seconds due to inactivity.
        </p>
      )}
      successCallback={() => {
        lastActivity.current = Date.now();
        updateRemainingSeconds(null);
      }}
      options={{
        cancelableViaIcon: false,
        okButtonText: "Continue Session",
        cancelButtonText: "Sign Out",
        extraButtons: [{ text: "Sign Out", onClick: () => void signOut(), variant: "secondary" }],
        cancelableViaBtn: false,
      }}
    />
  );
}
