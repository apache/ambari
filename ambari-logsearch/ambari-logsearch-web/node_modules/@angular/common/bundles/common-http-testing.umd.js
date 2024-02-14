/**
 * @license Angular v4.4.3
 * (c) 2010-2017 Google, Inc. https://angular.io/
 * License: MIT
 */
(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@angular/common/http'), require('@angular/core'), require('rxjs/Observable')) :
	typeof define === 'function' && define.amd ? define(['exports', '@angular/common/http', '@angular/core', 'rxjs/Observable'], factory) :
	(factory((global.ng = global.ng || {}, global.ng.common = global.ng.common || {}, global.ng.common.http = global.ng.common.http || {}, global.ng.common.http.testing = global.ng.common.http.testing || {}),global.ng.common.http,global.ng.core,global.Rx));
}(this, (function (exports,_angular_common_http,_angular_core,rxjs_Observable) { 'use strict';

/**
 * @license Angular v4.4.3
 * (c) 2010-2017 Google, Inc. https://angular.io/
 * License: MIT
 */
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * Controller to be injected into tests, that allows for mocking and flushing
 * of requests.
 *
 * \@experimental
 * @abstract
 */
var HttpTestingController = (function () {
    function HttpTestingController() {
    }
    /**
     * Search for requests that match the given parameter, without any expectations.
     * @abstract
     * @param {?} match
     * @return {?}
     */
    HttpTestingController.prototype.match = function (match) { };
    /**
     * Expect that a single request has been made which matches the given URL, and return its
     * mock.
     *
     * If no such request has been made, or more than one such request has been made, fail with an
     * error message including the given request description, if any.
     * @abstract
     * @param {?} url
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectOne = function (url, description) { };
    /**
     * Expect that a single request has been made which matches the given parameters, and return
     * its mock.
     *
     * If no such request has been made, or more than one such request has been made, fail with an
     * error message including the given request description, if any.
     * @abstract
     * @param {?} params
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectOne = function (params, description) { };
    /**
     * Expect that a single request has been made which matches the given predicate function, and
     * return its mock.
     *
     * If no such request has been made, or more than one such request has been made, fail with an
     * error message including the given request description, if any.
     * @abstract
     * @param {?} matchFn
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectOne = function (matchFn, description) { };
    /**
     * Expect that a single request has been made which matches the given condition, and return
     * its mock.
     *
     * If no such request has been made, or more than one such request has been made, fail with an
     * error message including the given request description, if any.
     * @abstract
     * @param {?} match
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectOne = function (match, description) { };
    /**
     * Expect that no requests have been made which match the given URL.
     *
     * If a matching request has been made, fail with an error message including the given request
     * description, if any.
     * @abstract
     * @param {?} url
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectNone = function (url, description) { };
    /**
     * Expect that no requests have been made which match the given parameters.
     *
     * If a matching request has been made, fail with an error message including the given request
     * description, if any.
     * @abstract
     * @param {?} params
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectNone = function (params, description) { };
    /**
     * Expect that no requests have been made which match the given predicate function.
     *
     * If a matching request has been made, fail with an error message including the given request
     * description, if any.
     * @abstract
     * @param {?} matchFn
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectNone = function (matchFn, description) { };
    /**
     * Expect that no requests have been made which match the given condition.
     *
     * If a matching request has been made, fail with an error message including the given request
     * description, if any.
     * @abstract
     * @param {?} match
     * @param {?=} description
     * @return {?}
     */
    HttpTestingController.prototype.expectNone = function (match, description) { };
    /**
     * Verify that no unmatched requests are outstanding.
     *
     * If any requests are outstanding, fail with an error message indicating which requests were not
     * handled.
     *
     * If `ignoreCancelled` is not set (the default), `verify()` will also fail if cancelled requests
     * were not explicitly matched.
     * @abstract
     * @param {?=} opts
     * @return {?}
     */
    HttpTestingController.prototype.verify = function (opts) { };
    return HttpTestingController;
}());
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * A mock requests that was received and is ready to be answered.
 *
 * This interface allows access to the underlying `HttpRequest`, and allows
 * responding with `HttpEvent`s or `HttpErrorResponse`s.
 *
 * \@experimental
 */
var TestRequest = (function () {
    /**
     * @param {?} request
     * @param {?} observer
     */
    function TestRequest(request, observer) {
        this.request = request;
        this.observer = observer;
        /**
         * \@internal set by `HttpClientTestingBackend`
         */
        this._cancelled = false;
    }
    Object.defineProperty(TestRequest.prototype, "cancelled", {
        /**
         * Whether the request was cancelled after it was sent.
         * @return {?}
         */
        get: function () { return this._cancelled; },
        enumerable: true,
        configurable: true
    });
    /**
     * Resolve the request by returning a body plus additional HTTP information (such as response
     * headers) if provided.
     *
     * Both successful and unsuccessful responses can be delivered via `flush()`.
     * @param {?} body
     * @param {?=} opts
     * @return {?}
     */
    TestRequest.prototype.flush = function (body, opts) {
        if (opts === void 0) { opts = {}; }
        if (this.cancelled) {
            throw new Error("Cannot flush a cancelled request.");
        }
        var /** @type {?} */ url = this.request.urlWithParams;
        var /** @type {?} */ headers = (opts.headers instanceof _angular_common_http.HttpHeaders) ? opts.headers : new _angular_common_http.HttpHeaders(opts.headers);
        body = _maybeConvertBody(this.request.responseType, body);
        var /** @type {?} */ statusText = opts.statusText;
        var /** @type {?} */ status = opts.status !== undefined ? opts.status : 200;
        if (opts.status === undefined) {
            if (body === null) {
                status = 204;
                statusText = statusText || 'No Content';
            }
            else {
                statusText = statusText || 'OK';
            }
        }
        if (statusText === undefined) {
            throw new Error('statusText is required when setting a custom status.');
        }
        if (status >= 200 && status < 300) {
            this.observer.next(new _angular_common_http.HttpResponse({ body: body, headers: headers, status: status, statusText: statusText, url: url }));
            this.observer.complete();
        }
        else {
            this.observer.error(new _angular_common_http.HttpErrorResponse({ error: body, headers: headers, status: status, statusText: statusText, url: url }));
        }
    };
    /**
     * Resolve the request by returning an `ErrorEvent` (e.g. simulating a network failure).
     * @param {?} error
     * @param {?=} opts
     * @return {?}
     */
    TestRequest.prototype.error = function (error, opts) {
        if (opts === void 0) { opts = {}; }
        if (this.cancelled) {
            throw new Error("Cannot return an error for a cancelled request.");
        }
        if (opts.status && opts.status >= 200 && opts.status < 300) {
            throw new Error("error() called with a successful status.");
        }
        var /** @type {?} */ headers = (opts.headers instanceof _angular_common_http.HttpHeaders) ? opts.headers : new _angular_common_http.HttpHeaders(opts.headers);
        this.observer.error(new _angular_common_http.HttpErrorResponse({
            error: error,
            headers: headers,
            status: opts.status || 0,
            statusText: opts.statusText || '',
            url: this.request.urlWithParams,
        }));
    };
    /**
     * Deliver an arbitrary `HttpEvent` (such as a progress event) on the response stream for this
     * request.
     * @param {?} event
     * @return {?}
     */
    TestRequest.prototype.event = function (event) {
        if (this.cancelled) {
            throw new Error("Cannot send events to a cancelled request.");
        }
        this.observer.next(event);
    };
    return TestRequest;
}());
/**
 * Helper function to convert a response body to an ArrayBuffer.
 * @param {?} body
 * @return {?}
 */
function _toArrayBufferBody(body) {
    if (typeof ArrayBuffer === 'undefined') {
        throw new Error('ArrayBuffer responses are not supported on this platform.');
    }
    if (body instanceof ArrayBuffer) {
        return body;
    }
    throw new Error('Automatic conversion to ArrayBuffer is not supported for response type.');
}
/**
 * Helper function to convert a response body to a Blob.
 * @param {?} body
 * @return {?}
 */
function _toBlob(body) {
    if (typeof Blob === 'undefined') {
        throw new Error('Blob responses are not supported on this platform.');
    }
    if (body instanceof Blob) {
        return body;
    }
    if (ArrayBuffer && body instanceof ArrayBuffer) {
        return new Blob([body]);
    }
    throw new Error('Automatic conversion to Blob is not supported for response type.');
}
/**
 * Helper function to convert a response body to JSON data.
 * @param {?} body
 * @param {?=} format
 * @return {?}
 */
function _toJsonBody(body, format) {
    if (format === void 0) { format = 'JSON'; }
    if (typeof ArrayBuffer !== 'undefined' && body instanceof ArrayBuffer) {
        throw new Error("Automatic conversion to " + format + " is not supported for ArrayBuffers.");
    }
    if (typeof Blob !== 'undefined' && body instanceof Blob) {
        throw new Error("Automatic conversion to " + format + " is not supported for Blobs.");
    }
    if (typeof body === 'string' || typeof body === 'number' || typeof body === 'object' ||
        Array.isArray(body)) {
        return body;
    }
    throw new Error("Automatic conversion to " + format + " is not supported for response type.");
}
/**
 * Helper function to convert a response body to a string.
 * @param {?} body
 * @return {?}
 */
function _toTextBody(body) {
    if (typeof body === 'string') {
        return body;
    }
    if (typeof ArrayBuffer !== 'undefined' && body instanceof ArrayBuffer) {
        throw new Error('Automatic conversion to text is not supported for ArrayBuffers.');
    }
    if (typeof Blob !== 'undefined' && body instanceof Blob) {
        throw new Error('Automatic conversion to text is not supported for Blobs.');
    }
    return JSON.stringify(_toJsonBody(body, 'text'));
}
/**
 * Convert a response body to the requested type.
 * @param {?} responseType
 * @param {?} body
 * @return {?}
 */
function _maybeConvertBody(responseType, body) {
    switch (responseType) {
        case 'arraybuffer':
            if (body === null) {
                return null;
            }
            return _toArrayBufferBody(body);
        case 'blob':
            if (body === null) {
                return null;
            }
            return _toBlob(body);
        case 'json':
            if (body === null) {
                return 'null';
            }
            return _toJsonBody(body);
        case 'text':
            if (body === null) {
                return null;
            }
            return _toTextBody(body);
        default:
            throw new Error("Unsupported responseType: " + responseType);
    }
}
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * A testing backend for `HttpClient` which both acts as an `HttpBackend`
 * and as the `HttpTestingController`.
 *
 * `HttpClientTestingBackend` works by keeping a list of all open requests.
 * As requests come in, they're added to the list. Users can assert that specific
 * requests were made and then flush them. In the end, a verify() method asserts
 * that no unexpected requests were made.
 *
 * \@experimental
 */
var HttpClientTestingBackend = (function () {
    function HttpClientTestingBackend() {
        /**
         * List of pending requests which have not yet been expected.
         */
        this.open = [];
    }
    /**
     * Handle an incoming request by queueing it in the list of open requests.
     * @param {?} req
     * @return {?}
     */
    HttpClientTestingBackend.prototype.handle = function (req) {
        var _this = this;
        return new rxjs_Observable.Observable(function (observer) {
            var /** @type {?} */ testReq = new TestRequest(req, observer);
            _this.open.push(testReq);
            observer.next(/** @type {?} */ ({ type: _angular_common_http.HttpEventType.Sent }));
            return function () { testReq._cancelled = true; };
        });
    };
    /**
     * Helper function to search for requests in the list of open requests.
     * @param {?} match
     * @return {?}
     */
    HttpClientTestingBackend.prototype._match = function (match) {
        if (typeof match === 'string') {
            return this.open.filter(function (testReq) { return testReq.request.urlWithParams === match; });
        }
        else if (typeof match === 'function') {
            return this.open.filter(function (testReq) { return match(testReq.request); });
        }
        else {
            return this.open.filter(function (testReq) { return (!match.method || testReq.request.method === match.method.toUpperCase()) &&
                (!match.url || testReq.request.urlWithParams === match.url); });
        }
    };
    /**
     * Search for requests in the list of open requests, and return all that match
     * without asserting anything about the number of matches.
     * @param {?} match
     * @return {?}
     */
    HttpClientTestingBackend.prototype.match = function (match) {
        var _this = this;
        var /** @type {?} */ results = this._match(match);
        results.forEach(function (result) {
            var /** @type {?} */ index = _this.open.indexOf(result);
            if (index !== -1) {
                _this.open.splice(index, 1);
            }
        });
        return results;
    };
    /**
     * Expect that a single outstanding request matches the given matcher, and return
     * it.
     *
     * Requests returned through this API will no longer be in the list of open requests,
     * and thus will not match twice.
     * @param {?} match
     * @param {?=} description
     * @return {?}
     */
    HttpClientTestingBackend.prototype.expectOne = function (match, description) {
        description = description || this.descriptionFromMatcher(match);
        var /** @type {?} */ matches = this.match(match);
        if (matches.length > 1) {
            throw new Error("Expected one matching request for criteria \"" + description + "\", found " + matches.length + " requests.");
        }
        if (matches.length === 0) {
            throw new Error("Expected one matching request for criteria \"" + description + "\", found none.");
        }
        return matches[0];
    };
    /**
     * Expect that no outstanding requests match the given matcher, and throw an error
     * if any do.
     * @param {?} match
     * @param {?=} description
     * @return {?}
     */
    HttpClientTestingBackend.prototype.expectNone = function (match, description) {
        description = description || this.descriptionFromMatcher(match);
        var /** @type {?} */ matches = this.match(match);
        if (matches.length > 0) {
            throw new Error("Expected zero matching requests for criteria \"" + description + "\", found " + matches.length + ".");
        }
    };
    /**
     * Validate that there are no outstanding requests.
     * @param {?=} opts
     * @return {?}
     */
    HttpClientTestingBackend.prototype.verify = function (opts) {
        if (opts === void 0) { opts = {}; }
        var /** @type {?} */ open = this.open;
        // It's possible that some requests may be cancelled, and this is expected.
        // The user can ask to ignore open requests which have been cancelled.
        if (opts.ignoreCancelled) {
            open = open.filter(function (testReq) { return !testReq.cancelled; });
        }
        if (open.length > 0) {
            // Show the methods and URLs of open requests in the error, for convenience.
            var /** @type {?} */ requests = open.map(function (testReq) {
                var /** @type {?} */ url = testReq.request.urlWithParams.split('?')[0];
                var /** @type {?} */ method = testReq.request.method;
                return method + " " + url;
            })
                .join(', ');
            throw new Error("Expected no open requests, found " + open.length + ": " + requests);
        }
    };
    /**
     * @param {?} matcher
     * @return {?}
     */
    HttpClientTestingBackend.prototype.descriptionFromMatcher = function (matcher) {
        if (typeof matcher === 'string') {
            return "Match URL: " + matcher;
        }
        else if (typeof matcher === 'object') {
            var /** @type {?} */ method = matcher.method || '(any)';
            var /** @type {?} */ url = matcher.url || '(any)';
            return "Match method: " + method + ", URL: " + url;
        }
        else {
            return "Match by function: " + matcher.name;
        }
    };
    return HttpClientTestingBackend;
}());
HttpClientTestingBackend.decorators = [
    { type: _angular_core.Injectable },
];
/**
 * @nocollapse
 */
HttpClientTestingBackend.ctorParameters = function () { return []; };
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * Configures `HttpClientTestingBackend` as the `HttpBackend` used by `HttpClient`.
 *
 * Inject `HttpTestingController` to expect and flush requests in your tests.
 *
 * \@experimental
 */
var HttpClientTestingModule = (function () {
    function HttpClientTestingModule() {
    }
    return HttpClientTestingModule;
}());
HttpClientTestingModule.decorators = [
    { type: _angular_core.NgModule, args: [{
                imports: [
                    _angular_common_http.HttpClientModule,
                ],
                providers: [
                    HttpClientTestingBackend,
                    { provide: _angular_common_http.HttpBackend, useExisting: HttpClientTestingBackend },
                    { provide: HttpTestingController, useExisting: HttpClientTestingBackend },
                ],
            },] },
];
/**
 * @nocollapse
 */
HttpClientTestingModule.ctorParameters = function () { return []; };

exports.HttpTestingController = HttpTestingController;
exports.HttpClientTestingModule = HttpClientTestingModule;
exports.TestRequest = TestRequest;
exports.ɵa = HttpClientTestingBackend;

Object.defineProperty(exports, '__esModule', { value: true });

})));
//# sourceMappingURL=common-http-testing.umd.js.map
