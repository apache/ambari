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

import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ViewIframe from "./ViewIframe";
import {
  calculateViewIframeHeight,
  VIEW_IFRAME_LOAD_TIMEOUT_MS,
} from "./viewIframeUtils";

describe("View iframe lifecycle", () => {
  beforeEach(() => {
    vi.spyOn(window, "scrollTo").mockImplementation(() => undefined);
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("calculates the larger of the View content and available page height", () => {
    expect(calculateViewIframeHeight(900, 1000, 80, 40)).toBe(900);
    expect(calculateViewIframeHeight(300, 1000, 80, 40)).toBe(880);
    expect(calculateViewIframeHeight(0, 0, 0, 0)).toBe(0);
  });

  it("uses the server context path without sandboxing the View", () => {
    render(
      <ViewIframe
        contextPath="/gateway/default/views/TEZ/1.0/INSTANCE"
        title="Tez View"
        viewPath="#/tez-app/application_1"
      />,
    );

    const iframe = screen.getByTitle("Tez View") as HTMLIFrameElement;
    expect(iframe.getAttribute("src")).toBe(
      "http://localhost:3000/gateway/default/views/TEZ/1.0/INSTANCE/#/tez-app/application_1",
    );
    expect(iframe.hasAttribute("sandbox")).toBe(false);
    expect(iframe.hasAttribute("allowfullscreen")).toBe(true);
  });

  it("returns to loading when route data changes and clears its resize timer", () => {
    const clearInterval = vi.spyOn(window, "clearInterval");
    const clearTimeout = vi.spyOn(window, "clearTimeout");
    const rendered = render(
      <ViewIframe contextPath="/views/TEZ/1.0/A" title="Tez View" />,
    );

    const firstIframe = screen.getByTitle("Tez View");
    expect(screen.getByRole("status")).toBeTruthy();
    fireEvent.load(firstIframe);
    expect(screen.queryByRole("status")).toBeNull();

    rendered.rerender(
      <ViewIframe contextPath="/views/TEZ/2.0/B" title="Tez View" />,
    );
    expect(screen.getByRole("status")).toBeTruthy();
    expect(screen.getByTitle("Tez View").getAttribute("src")).toBe(
      "http://localhost:3000/views/TEZ/2.0/B/",
    );
    fireEvent.load(firstIframe);
    expect(screen.getByRole("status")).toBeTruthy();

    rendered.unmount();
    expect(clearInterval).toHaveBeenCalled();
    expect(clearTimeout).toHaveBeenCalled();
  });

  it("uses exact page chrome heights, shrinks content, and restores both scroll owners", () => {
    vi.useFakeTimers();
    const rendered = render(
      <div data-view-scroll-container>
        <div id="top-nav" />
        <iframe title="Unrelated View" style={{ height: 321 }} />
        <ViewIframe contextPath="/views/TEZ/1.0/A" title="Tez View" />
        <footer />
      </div>,
    );

    const iframe = screen.getByTitle("Tez View") as HTMLIFrameElement;
    const unrelatedIframe = screen.getByTitle("Unrelated View");
    const header = document.getElementById("top-nav") as HTMLElement;
    const footer = document.querySelector("footer") as HTMLElement;
    const scrollContainer = iframe.closest<HTMLElement>("[data-view-scroll-container]")!;
    let contentHeight = 900;

    vi.spyOn(document.body, "offsetHeight", "get").mockReturnValue(1000);
    vi.spyOn(header, "offsetHeight", "get").mockReturnValue(80);
    vi.spyOn(footer, "offsetHeight", "get").mockReturnValue(40);
    vi.spyOn(iframe, "contentWindow", "get").mockReturnValue({
      document: {
        body: {
          get scrollHeight() {
            expect(iframe.classList.contains("d-none")).toBe(false);
            expect(iframe.style.height).toBe("auto");
            return contentHeight;
          },
        },
      },
    } as Window);
    vi.spyOn(window, "scrollX", "get").mockReturnValue(12);
    vi.spyOn(window, "scrollY", "get").mockReturnValue(34);
    scrollContainer.scrollLeft = 21;
    scrollContainer.scrollTop = 55;

    fireEvent.load(iframe);
    expect(iframe.style.height).toBe("900px");
    expect(unrelatedIframe.style.height).toBe("321px");
    expect(window.scrollTo).toHaveBeenLastCalledWith(12, 34);
    expect(scrollContainer.scrollLeft).toBe(21);
    expect(scrollContainer.scrollTop).toBe(55);

    contentHeight = 300;
    act(() => vi.advanceTimersByTime(5000));
    expect(iframe.style.height).toBe("880px");

    rendered.unmount();
  });

  it("restores the stable height when cross-origin content cannot be measured", () => {
    render(<ViewIframe contextPath="/views/TEZ/1.0/A" title="Tez View" />);
    const iframe = screen.getByTitle("Tez View") as HTMLIFrameElement;
    iframe.style.height = "640px";
    vi.spyOn(iframe, "contentWindow", "get").mockImplementation(() => {
      throw new DOMException("Blocked by same-origin policy", "SecurityError");
    });

    expect(() => fireEvent.load(iframe)).not.toThrow();
    expect(iframe.style.height).toBe("640px");
    expect(window.scrollTo).toHaveBeenCalled();
  });

  it("shows timeout and navigation errors and retries with a fresh iframe", () => {
    vi.useFakeTimers();
    render(<ViewIframe contextPath="/views/TEZ/1.0/A" title="Tez View" />);

    const firstIframe = screen.getByTitle("Tez View");
    act(() => vi.advanceTimersByTime(VIEW_IFRAME_LOAD_TIMEOUT_MS - 1));
    expect(screen.queryByText("Unable to load View")).toBeNull();
    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByText("Unable to load View")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    const retriedIframe = screen.getByTitle("Tez View");
    expect(retriedIframe).not.toBe(firstIframe);
    expect(retriedIframe.getAttribute("src")).toBe(firstIframe.getAttribute("src"));
    expect(screen.getByRole("status")).toBeTruthy();

    fireEvent.error(retriedIframe);
    expect(screen.getByText("Unable to load View")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    const successfulIframe = screen.getByTitle("Tez View");
    fireEvent.load(successfulIframe);
    expect(screen.queryByRole("status")).toBeNull();
    act(() => vi.advanceTimersByTime(VIEW_IFRAME_LOAD_TIMEOUT_MS));
    expect(screen.queryByText("Unable to load View")).toBeNull();
  });
});
