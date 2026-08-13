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

import { LocalStorageOps } from "./LocalStorageOps";

const PREFERRED_PATH_KEY = "lastVisitedURL";
const REDIRECT_COUNT_KEY = "ambari.jwtRedirectCount";
const MAX_EXTERNAL_REDIRECTS = 3;
export const SESSION_EXPIRED_EVENT = "ambari:session-expired";

export const LOGIN_PATH = "/login";
export const LOCAL_LOGIN_PATH = "/login/local";

export function currentHashPath(location: Pick<Location, "hash"> = window.location): string {
  const value = location.hash.startsWith("#") ? location.hash.slice(1) : location.hash;
  return value || "/";
}

export function normalizeInternalPath(value?: string | null): string | null {
  if (!value) {
    return null;
  }

  let candidate = value.trim();
  if (candidate.startsWith("/#")) {
    candidate = candidate.slice(2);
  } else if (candidate.startsWith("#")) {
    candidate = candidate.slice(1);
  }

  if (!candidate.startsWith("/") || candidate.startsWith("//")) {
    return null;
  }

  const pathOnly = candidate.split("?")[0].split("#")[0];
  if (pathOnly === "/" || pathOnly === LOGIN_PATH || pathOnly === LOCAL_LOGIN_PATH) {
    return null;
  }

  return candidate;
}

export function savePreferredPath(path: string): void {
  const normalized = normalizeInternalPath(path);
  if (normalized) {
    LocalStorageOps.setItem(PREFERRED_PATH_KEY, normalized);
  }
}

export function consumePreferredPath(): string | null {
  const normalized = normalizeInternalPath(LocalStorageOps.getItem(PREFERRED_PATH_KEY));
  localStorage.removeItem(PREFERRED_PATH_KEY);
  return normalized;
}

export function peekPreferredPath(): string | null {
  return normalizeInternalPath(LocalStorageOps.getItem(PREFERRED_PATH_KEY));
}

export function resetExternalRedirectCount(): void {
  sessionStorage.removeItem(REDIRECT_COUNT_KEY);
}

export function getExternalLoginUrl(
  jwtProviderUrl: unknown,
  returnUrl: string,
  isLocalLogin: boolean,
): string | null {
  if (isLocalLogin || typeof jwtProviderUrl !== "string" || !jwtProviderUrl) {
    return null;
  }

  const redirectCount = Number(sessionStorage.getItem(REDIRECT_COUNT_KEY) || "0");
  if (!Number.isFinite(redirectCount) || redirectCount >= MAX_EXTERNAL_REDIRECTS) {
    return null;
  }

  sessionStorage.setItem(REDIRECT_COUNT_KEY, String(redirectCount + 1));
  return jwtProviderUrl + encodeURIComponent(returnUrl);
}

export function hasExceededExternalRedirectLimit(): boolean {
  return Number(sessionStorage.getItem(REDIRECT_COUNT_KEY) || "0") >= MAX_EXTERNAL_REDIRECTS;
}

export function redirectToLocalLogin(): void {
  const currentPath = currentHashPath();
  if (currentPath !== LOGIN_PATH && currentPath !== LOCAL_LOGIN_PATH) {
    savePreferredPath(currentPath);
  }
  window.location.hash = LOGIN_PATH;
}

function expireClientSession(): void {
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}

export function handleAuthenticationError(response: {
  status?: number;
  data?: { jwtProviderUrl?: string };
}): boolean {
  if (response.status !== 401 && response.status !== 403) {
    return false;
  }

  // A plain 403 is an authorization failure, not evidence that the session expired.
  if (response.status === 403 && !response.data?.jwtProviderUrl) {
    return false;
  }

  expireClientSession();

  const currentPath = currentHashPath();
  const isLocalLogin = currentPath.split("?")[0] === LOCAL_LOGIN_PATH;
  const externalUrl = getExternalLoginUrl(
    response.data?.jwtProviderUrl,
    window.location.href,
    isLocalLogin,
  );

  if (externalUrl) {
    window.location.assign(externalUrl);
    return true;
  }

  if (hasExceededExternalRedirectLimit() && !isLocalLogin) {
    window.location.hash = `${LOCAL_LOGIN_PATH}?redirectError=true`;
    return true;
  }

  if (currentPath !== LOGIN_PATH && !isLocalLogin) {
    redirectToLocalLogin();
    return true;
  }

  return false;
}
