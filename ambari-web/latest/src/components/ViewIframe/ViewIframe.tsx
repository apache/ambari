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

import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Button } from "react-bootstrap";
import { buildViewIframeSrc } from "../../Utils/viewUtils";
import {
  calculateViewIframeHeight,
  VIEW_IFRAME_LOAD_TIMEOUT_MS,
} from "./viewIframeUtils";

type ViewIframeProps = {
  contextPath: string;
  title: string;
  viewPath?: string;
};

export default function ViewIframe({ contextPath, title, viewPath = "" }: ViewIframeProps) {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const loadTimeoutRef = useRef<{ id: number; requestKey: string } | null>(null);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const src = buildViewIframeSrc(window.location.origin, contextPath, viewPath);
  const requestKey = `${src}:${loadAttempt}`;
  const [loadState, setLoadState] = useState({
    requestKey,
    phase: "loading" as "loading" | "loaded" | "error",
  });
  const phase = loadState.requestKey === requestKey ? loadState.phase : "loading";

  const clearLoadTimeout = useCallback((expectedRequestKey?: string) => {
    if (
      loadTimeoutRef.current
      && (!expectedRequestKey || loadTimeoutRef.current.requestKey === expectedRequestKey)
    ) {
      window.clearTimeout(loadTimeoutRef.current.id);
      loadTimeoutRef.current = null;
    }
  }, []);

  const resizeIframe = useCallback(() => {
    const iframe = iframeRef.current;
    if (!iframe) return;

    const previousHeight = iframe.style.height;
    const pageScrollX = window.scrollX;
    const pageScrollY = window.scrollY;
    const scrollContainer = iframe.closest<HTMLElement>("[data-view-scroll-container]");
    const containerScrollLeft = scrollContainer?.scrollLeft;
    const containerScrollTop = scrollContainer?.scrollTop;
    try {
      const bodyHeight = document.body.offsetHeight;
      const headerHeight = document.getElementById("top-nav")?.offsetHeight || 0;
      const footerHeight = document.querySelector("footer")?.offsetHeight || 0;
      iframe.style.height = "auto";
      const contentHeight = iframe.contentWindow?.document?.body?.scrollHeight || 0;
      iframe.style.height = `${calculateViewIframeHeight(
        contentHeight,
        bodyHeight,
        headerHeight,
        footerHeight,
      )}px`;
    } catch {
      iframe.style.height = previousHeight;
    } finally {
      window.scrollTo(pageScrollX, pageScrollY);
      if (scrollContainer) {
        scrollContainer.scrollLeft = containerScrollLeft || 0;
        scrollContainer.scrollTop = containerScrollTop || 0;
      }
    }
  }, []);

  useEffect(() => {
    setLoadState({ requestKey, phase: "loading" });
    clearLoadTimeout();
    const timeoutId = window.setTimeout(() => {
      setLoadState((current) => current.requestKey === requestKey && current.phase === "loading"
        ? { ...current, phase: "error" }
        : current);
      if (loadTimeoutRef.current?.id === timeoutId) {
        loadTimeoutRef.current = null;
      }
    }, VIEW_IFRAME_LOAD_TIMEOUT_MS);
    loadTimeoutRef.current = { id: timeoutId, requestKey };

    return () => clearLoadTimeout(requestKey);
  }, [clearLoadTimeout, requestKey]);

  useEffect(() => {
    resizeIframe();
    const intervalId = window.setInterval(resizeIframe, 5000);
    window.addEventListener("resize", resizeIframe);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("resize", resizeIframe);
    };
  }, [resizeIframe]);

  useEffect(() => {
    if (phase === "loaded") {
      resizeIframe();
    }
  }, [phase, resizeIframe]);

  const handleLoad = () => {
    clearLoadTimeout(requestKey);
    setLoadState((current) => (
      current.requestKey === requestKey && current.phase === "loading"
        ? { ...current, phase: "loaded" }
        : current
    ));
  };

  const handleError = () => {
    clearLoadTimeout(requestKey);
    setLoadState((current) => current.requestKey === requestKey
      ? { ...current, phase: "error" }
      : current);
  };

  return (
    <div className="d-flex flex-column w-100">
      {phase === "loading" ? (
        <div className="d-flex justify-content-center align-items-center p-4">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : null}
      {phase === "error" ? (
        <Alert variant="danger" className="m-3">
          <Alert.Heading>Unable to load View</Alert.Heading>
          <p>The View did not respond. Check the View deployment and try again.</p>
          <Button variant="outline-danger" onClick={() => setLoadAttempt((value) => value + 1)}>
            Retry
          </Button>
        </Alert>
      ) : null}
      <iframe
        key={requestKey}
        ref={iframeRef}
        src={src}
        className={`d-block w-100 border-0 ${phase === "loaded" ? "" : "d-none"}`}
        style={{ height: "100vh" }}
        seamless
        allowFullScreen
        onLoad={handleLoad}
        onError={handleError}
        onErrorCapture={handleError}
        title={title}
      />
    </div>
  );
}
