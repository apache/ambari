import { Inject, Injectable, Injector, Optional } from '@angular/core';
import { BaseResponseOptions, BrowserXhr, Headers, ReadyState, RequestMethod, Response, ResponseOptions, URLSearchParams, XHRBackend, XSRFStrategy } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/delay';
import { STATUS, STATUS_CODE_INFO } from './http-status-codes';
////////////  HELPERS ///////////
/**
 * Create an error Response from an HTTP status code and error message
 */
export function createErrorResponse(req, status, message) {
    return new ResponseOptions({
        body: { 'error': "" + message },
        url: req.url,
        headers: new Headers({ 'Content-Type': 'application/json' }),
        status: status
    });
}
/**
 * Create an Observable response from response options.
 */
export function createObservableResponse(req, resOptions) {
    return new Observable(function (responseObserver) {
        emitResponse(responseObserver, req, resOptions);
        return function () { }; // unsubscribe function
    });
}
/**
 * Create a response from response options
 * and tell "ResponseObserver" (an `Observer<Response>`) to emit it.
 * The observer's observable is either completed or in error state after call.
 */
export function emitResponse(responseObserver, req, resOptions) {
    resOptions.url = resOptions.url || req.url; // make sure url is set
    resOptions = setStatusText(resOptions);
    var res = new Response(resOptions);
    if (isSuccess(res.status)) {
        responseObserver.next(res);
        responseObserver.complete();
    }
    else {
        responseObserver.error(res);
    }
}
/**
* Interface for a class that creates an in-memory database
*
* Its `createDb` method creates a hash of named collections that represents the database
*
* For maximum flexibility, the service may define HTTP method overrides.
* Such methods must match the spelling of an HTTP method in lower case (e.g, "get").
* If a request has a matching method, it will be called as in
* `get(info: requestInfo, db: {})` where `db` is the database object described above.
*/
export var InMemoryDbService = (function () {
    function InMemoryDbService() {
    }
    return InMemoryDbService;
}());
/**
* Interface for InMemoryBackend configuration options
*/
export var InMemoryBackendConfigArgs = (function () {
    function InMemoryBackendConfigArgs() {
    }
    return InMemoryBackendConfigArgs;
}());
export function removeTrailingSlash(path) {
    return path.replace(/\/$/, '');
}
/////////////////////////////////
/**
*  InMemoryBackendService configuration options
*  Usage:
*    InMemoryWebApiModule.forRoot(InMemHeroService, {delay: 600})
*
*  or if providing separately:
*    provide(InMemoryBackendConfig, {useValue: {delay: 600}}),
*/
export var InMemoryBackendConfig = (function () {
    function InMemoryBackendConfig(config) {
        if (config === void 0) { config = {}; }
        Object.assign(this, {
            // default config:
            caseSensitiveSearch: false,
            defaultResponseOptions: new BaseResponseOptions(),
            delay: 500,
            delete404: false,
            passThruUnknownUrl: false,
            post204: true,
            put204: true,
            apiBase: undefined,
            host: undefined,
            rootPath: undefined // default value is actually set in InMemoryBackendService ctor
        }, config);
    }
    InMemoryBackendConfig.decorators = [
        { type: Injectable },
    ];
    /** @nocollapse */
    InMemoryBackendConfig.ctorParameters = function () { return [
        { type: InMemoryBackendConfigArgs, },
    ]; };
    return InMemoryBackendConfig;
}());
/**
 * Returns true if the the Http Status Code is 200-299 (success)
 */
export function isSuccess(status) { return status >= 200 && status < 300; }
;
/**
 * Set the status text in a response:
 */
export function setStatusText(options) {
    try {
        var statusCode = STATUS_CODE_INFO[options.status];
        options['statusText'] = statusCode ? statusCode.text : 'Unknown Status';
        return options;
    }
    catch (err) {
        return new ResponseOptions({
            status: STATUS.INTERNAL_SERVER_ERROR,
            statusText: 'Invalid Server Operation'
        });
    }
}
////////////  InMemoryBackendService ///////////
/**
 * Simulate the behavior of a RESTy web api
 * backed by the simple in-memory data store provided by the injected InMemoryDataService service.
 * Conforms mostly to behavior described here:
 * http://www.restapitutorial.com/lessons/httpmethods.html
 *
 * ### Usage
 *
 * Create `InMemoryDataService` class that implements `InMemoryDataService`.
 * Call `forRoot` static method with this service class and optional configuration object:
 * ```
 * // other imports
 * import { HttpModule }           from '@angular/http';
 * import { InMemoryWebApiModule } from 'angular-in-memory-web-api';
 *
 * import { InMemHeroService, inMemConfig } from '../api/in-memory-hero.service';
 * @NgModule({
 *  imports: [
 *    HttpModule,
 *    InMemoryWebApiModule.forRoot(InMemHeroService, inMemConfig),
 *    ...
 *  ],
 *  ...
 * })
 * export class AppModule { ... }
 * ```
 */
export var InMemoryBackendService = (function () {
    function InMemoryBackendService(injector, inMemDbService, config) {
        this.injector = injector;
        this.inMemDbService = inMemDbService;
        this.config = new InMemoryBackendConfig();
        this.resetDb();
        var loc = this.getLocation('/');
        this.config.host = loc.host; // default to app web server host
        this.config.rootPath = loc.pathname; // default to path when app is served (e.g.'/')
        Object.assign(this.config, config || {});
        this.setPassThruBackend();
    }
    InMemoryBackendService.prototype.createConnection = function (req) {
        var response;
        try {
            response = this.handleRequest(req);
        }
        catch (error) {
            var err = error.message || error;
            var options = createErrorResponse(req, STATUS.INTERNAL_SERVER_ERROR, "" + err);
            response = this.addDelay(createObservableResponse(req, options));
        }
        return {
            readyState: ReadyState.Done,
            request: req,
            response: response
        };
    };
    ////  protected /////
    /**
     * Process Request and return an Observable of Http Response object
     * in the manner of a RESTy web api.
     *
     * Expect URI pattern in the form :base/:collectionName/:id?
     * Examples:
     *   // for store with a 'customers' collection
     *   GET api/customers          // all customers
     *   GET api/customers/42       // the character with id=42
     *   GET api/customers?name=^j  // 'j' is a regex; returns customers whose name starts with 'j' or 'J'
     *   GET api/customers.json/42  // ignores the ".json"
     *
     * Also accepts direct commands to the service in which the last segment of the apiBase is the word "commands"
     * Examples:
     *     POST commands/resetDb,
     *     GET/POST commands/config - get or (re)set the config
     *
     *   HTTP overrides:
     *     If the injected inMemDbService defines an HTTP method (lowercase)
     *     The request is forwarded to that method as in
     *     `inMemDbService.get(httpMethodInterceptorArgs)`
     *     which must return an `Observable<Response>`
     */
    InMemoryBackendService.prototype.handleRequest = function (req) {
        var parsed = this.inMemDbService['parseUrl'] ?
            // parse with override method
            this.inMemDbService['parseUrl'](req.url) :
            // parse with default url parser
            this.parseUrl(req.url);
        var base = parsed.base, collectionName = parsed.collectionName, id = parsed.id, query = parsed.query, resourceUrl = parsed.resourceUrl;
        var collection = this.db[collectionName];
        var reqInfo = {
            req: req,
            base: base,
            collection: collection,
            collectionName: collectionName,
            headers: new Headers({ 'Content-Type': 'application/json' }),
            id: this.parseId(collection, id),
            query: query,
            resourceUrl: resourceUrl
        };
        var reqMethodName = RequestMethod[req.method || 0].toLowerCase();
        var resOptions;
        if (/commands\/$/i.test(reqInfo.base)) {
            return this.commands(reqInfo);
        }
        else if (this.inMemDbService[reqMethodName]) {
            // InMemoryDbService has an overriding interceptor for this HTTP method; call it
            // The interceptor result must be an Observable<Response>
            var interceptorArgs = {
                requestInfo: reqInfo,
                db: this.db,
                config: this.config,
                passThruBackend: this.passThruBackend
            };
            var interceptorResponse = this.inMemDbService[reqMethodName](interceptorArgs);
            return this.addDelay(interceptorResponse);
        }
        else if (reqInfo.collection) {
            // request is for a collection created by the InMemoryDbService
            return this.addDelay(this.collectionHandler(reqInfo));
        }
        else if (this.passThruBackend) {
            // Passes request thru to a "real" backend which returns an Observable<Response>
            // BAIL OUT with this Observable<Response>
            return this.passThruBackend.createConnection(req).response;
        }
        else {
            // can't handle this request
            resOptions = createErrorResponse(req, STATUS.NOT_FOUND, "Collection '" + collectionName + "' not found");
            return this.addDelay(createObservableResponse(req, resOptions));
        }
    };
    /**
     * Add configured delay to response observable unless delay === 0
     */
    InMemoryBackendService.prototype.addDelay = function (response) {
        var delay = this.config.delay;
        return delay === 0 ? response : response.delay(delay || 500);
    };
    /**
     * Apply query/search parameters as a filter over the collection
     * This impl only supports RegExp queries on string properties of the collection
     * ANDs the conditions together
     */
    InMemoryBackendService.prototype.applyQuery = function (collection, query) {
        // extract filtering conditions - {propertyName, RegExps) - from query/search parameters
        var conditions = [];
        var caseSensitive = this.config.caseSensitiveSearch ? undefined : 'i';
        query.paramsMap.forEach(function (value, name) {
            value.forEach(function (v) { return conditions.push({ name: name, rx: new RegExp(decodeURI(v), caseSensitive) }); });
        });
        var len = conditions.length;
        if (!len) {
            return collection;
        }
        // AND the RegExp conditions
        return collection.filter(function (row) {
            var ok = true;
            var i = len;
            while (ok && i) {
                i -= 1;
                var cond = conditions[i];
                ok = cond.rx.test(row[cond.name]);
            }
            return ok;
        });
    };
    InMemoryBackendService.prototype.clone = function (data) {
        return JSON.parse(JSON.stringify(data));
    };
    InMemoryBackendService.prototype.collectionHandler = function (reqInfo) {
        var _this = this;
        var req = reqInfo.req;
        return new Observable(function (responseObserver) {
            var resOptions;
            switch (req.method) {
                case RequestMethod.Get:
                    resOptions = _this.get(reqInfo);
                    break;
                case RequestMethod.Post:
                    resOptions = _this.post(reqInfo);
                    break;
                case RequestMethod.Put:
                    resOptions = _this.put(reqInfo);
                    break;
                case RequestMethod.Delete:
                    resOptions = _this.delete(reqInfo);
                    break;
                default:
                    resOptions = createErrorResponse(req, STATUS.METHOD_NOT_ALLOWED, 'Method not allowed');
                    break;
            }
            // If `inMemDbService.responseInterceptor` exists, let it morph the response options
            if (_this.inMemDbService['responseInterceptor']) {
                resOptions = _this.inMemDbService['responseInterceptor'](resOptions, reqInfo);
            }
            emitResponse(responseObserver, reqInfo.req, resOptions);
            return function () { }; // unsubscribe function
        });
    };
    /**
     * When the last segment of the `base` path is "commands", the `collectionName` is the command
     * Example URLs:
     *   commands/resetdb   // Reset the "database" to its original state
     *   commands/config (GET) // Return this service's config object
     *   commands/config (!GET) // Update the config (e.g. delay)
     *
     * Commands are "hot", meaning they are always executed immediately
     * whether or not someone subscribes to the returned observable
     *
     * Usage:
     *   http.post('commands/resetdb', undefined);
     *   http.get('commands/config');
     *   http.post('commands/config', '{"delay":1000}');
     */
    InMemoryBackendService.prototype.commands = function (reqInfo) {
        var command = reqInfo.collectionName.toLowerCase();
        var method = reqInfo.req.method;
        var resOptions;
        switch (command) {
            case 'resetdb':
                this.resetDb();
                resOptions = new ResponseOptions({ status: STATUS.OK });
                break;
            case 'config':
                if (method === RequestMethod.Get) {
                    resOptions = new ResponseOptions({
                        body: this.clone(this.config),
                        status: STATUS.OK
                    });
                }
                else {
                    // Be nice ... any other method is a config update
                    var body = JSON.parse(reqInfo.req.text() || '{}');
                    Object.assign(this.config, body);
                    this.setPassThruBackend();
                    resOptions = new ResponseOptions({ status: STATUS.NO_CONTENT });
                }
                break;
            default:
                resOptions = createErrorResponse(reqInfo.req, STATUS.INTERNAL_SERVER_ERROR, "Unknown command \"" + command + "\"");
        }
        return createObservableResponse(reqInfo.req, resOptions);
    };
    InMemoryBackendService.prototype.delete = function (_a) {
        var id = _a.id, collection = _a.collection, collectionName = _a.collectionName, headers = _a.headers, req = _a.req;
        // tslint:disable-next-line:triple-equals
        if (id == undefined) {
            return createErrorResponse(req, STATUS.NOT_FOUND, "Missing \"" + collectionName + "\" id");
        }
        var exists = this.removeById(collection, id);
        return new ResponseOptions({
            headers: headers,
            status: (exists || !this.config.delete404) ? STATUS.NO_CONTENT : STATUS.NOT_FOUND
        });
    };
    InMemoryBackendService.prototype.findById = function (collection, id) {
        return collection.find(function (item) { return item.id === id; });
    };
    InMemoryBackendService.prototype.genId = function (collection) {
        // assumes numeric ids
        var maxId = 0;
        collection.reduce(function (prev, item) {
            maxId = Math.max(maxId, typeof item.id === 'number' ? item.id : maxId);
        }, undefined);
        return maxId + 1;
    };
    InMemoryBackendService.prototype.get = function (_a) {
        var id = _a.id, query = _a.query, collection = _a.collection, collectionName = _a.collectionName, headers = _a.headers, req = _a.req;
        var data = collection;
        // tslint:disable-next-line:triple-equals
        if (id != undefined && id !== '') {
            data = this.findById(collection, id);
        }
        else if (query) {
            data = this.applyQuery(collection, query);
        }
        if (!data) {
            return createErrorResponse(req, STATUS.NOT_FOUND, "'" + collectionName + "' with id='" + id + "' not found");
        }
        return new ResponseOptions({
            body: { data: this.clone(data) },
            headers: headers,
            status: STATUS.OK
        });
    };
    InMemoryBackendService.prototype.getLocation = function (href) {
        if (!href.startsWith('http')) {
            // get the document iff running in browser
            var doc = (typeof document === 'undefined') ? undefined : document;
            // add host info to url before parsing.  Use a fake host when not in browser.
            var base = doc ? doc.location.protocol + '//' + doc.location.host : 'http://fake';
            href = href.startsWith('/') ? base + href : base + '/' + href;
        }
        var uri = this.parseuri(href);
        var loc = {
            host: uri.host,
            protocol: uri.protocol,
            port: uri.port,
            pathname: uri.path,
            search: uri.query ? '?' + uri.query : ''
        };
        return loc;
    };
    ;
    // Adapted from parseuri package - http://blog.stevenlevithan.com/archives/parseuri
    InMemoryBackendService.prototype.parseuri = function (str) {
        // tslint:disable-next-line:max-line-length
        var URL_REGEX = /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/;
        var key = ['source', 'protocol', 'authority', 'userInfo', 'user', 'password', 'host', 'port',
            'relative', 'path', 'directory', 'file', 'query', 'anchor'];
        var m = URL_REGEX.exec(str);
        var uri = {};
        var i = 14;
        while (i--) {
            uri[key[i]] = m[i] || '';
        }
        return uri;
    };
    InMemoryBackendService.prototype.indexOf = function (collection, id) {
        return collection.findIndex(function (item) { return item.id === id; });
    };
    // tries to parse id as number if collection item.id is a number.
    // returns the original param id otherwise.
    InMemoryBackendService.prototype.parseId = function (collection, id) {
        // tslint:disable-next-line:triple-equals
        if (!collection || id == undefined) {
            return undefined;
        }
        var isNumberId = collection[0] && typeof collection[0].id === 'number';
        if (isNumberId) {
            var idNum = parseFloat(id);
            return isNaN(idNum) ? id : idNum;
        }
        return id;
    };
    /**
     * Parses the request URL into a `ParsedUrl` object.
     * Parsing depends upon certain values of `config`: `apiBase`, `host`, and `urlRoot`.
     *
     * Configuring the `apiBase` yields the most interesting changes to `parseUrl` behavior:
     *   When apiBase=undefined and url='http://localhost/api/collection/42'
     *     {base: 'api/', collectionName: 'collection', id: '42', ...}
     *   When apiBase='some/api/root/' and url='http://localhost/some/api/root/collection'
     *     {base: 'some/api/root/', collectionName: 'collection', id: undefined, ...}
     *   When apiBase='/' and url='http://localhost/collection'
     *     {base: '/', collectionName: 'collection', id: undefined, ...}
     *
     * The actual api base segment values are ignored. Only the number of segments matters.
     * The following api base strings are considered identical: 'a/b' ~ 'some/api/' ~ `two/segments'
     *
     * To replace this default method, assign your alternative to your InMemDbService['parseUrl']
     */
    InMemoryBackendService.prototype.parseUrl = function (url) {
        try {
            var loc = this.getLocation(url);
            var drop = this.config.rootPath.length;
            var urlRoot = '';
            if (loc.host !== this.config.host) {
                // url for a server on a different host!
                // assume it's collection is actually here too.
                drop = 1; // the leading slash
                urlRoot = loc.protocol + '//' + loc.host + '/';
            }
            var path = loc.pathname.substring(drop);
            var pathSegments = path.split('/');
            var segmentIx = 0;
            // apiBase: the front part of the path devoted to getting to the api route
            // Assumes first path segment if no config.apiBase
            // else ignores as many path segments as are in config.apiBase
            // Does NOT care what the api base chars actually are.
            var apiBase = void 0;
            // tslint:disable-next-line:triple-equals
            if (this.config.apiBase == undefined) {
                apiBase = pathSegments[segmentIx++];
            }
            else {
                apiBase = removeTrailingSlash(this.config.apiBase.trim());
                if (apiBase) {
                    segmentIx = apiBase.split('/').length;
                }
                else {
                    segmentIx = 0; // no api base at all; unwise but allowed.
                }
            }
            apiBase = apiBase + '/';
            var collectionName = pathSegments[segmentIx++];
            // ignore anything after a '.' (e.g.,the "json" in "customers.json")
            collectionName = collectionName && collectionName.split('.')[0];
            var id = pathSegments[segmentIx++];
            var query = loc.search && new URLSearchParams(loc.search.substr(1));
            var resourceUrl = urlRoot + apiBase + collectionName + '/';
            return { base: apiBase, collectionName: collectionName, id: id, query: query, resourceUrl: resourceUrl };
        }
        catch (err) {
            var msg = "unable to parse url '" + url + "'; original error: " + err.message;
            throw new Error(msg);
        }
    };
    InMemoryBackendService.prototype.post = function (_a) {
        var collection = _a.collection, headers = _a.headers, id = _a.id, req = _a.req, resourceUrl = _a.resourceUrl;
        var item = JSON.parse(req.text());
        // tslint:disable-next-line:triple-equals
        if (item.id == undefined) {
            item.id = id || this.genId(collection);
        }
        // ignore the request id, if any. Alternatively,
        // could reject request if id differs from item.id
        id = item.id;
        var existingIx = this.indexOf(collection, id);
        var body = { data: this.clone(item) };
        if (existingIx > -1) {
            collection[existingIx] = item;
            var res = this.config.post204 ?
                { headers: headers, status: STATUS.NO_CONTENT } :
                { headers: headers, body: body, status: STATUS.OK }; // successful; return entity
            return new ResponseOptions(res);
        }
        else {
            collection.push(item);
            headers.set('Location', resourceUrl + '/' + id);
            return new ResponseOptions({ headers: headers, body: body, status: STATUS.CREATED });
        }
    };
    InMemoryBackendService.prototype.put = function (_a) {
        var id = _a.id, collection = _a.collection, collectionName = _a.collectionName, headers = _a.headers, req = _a.req;
        var item = JSON.parse(req.text());
        // tslint:disable-next-line:triple-equals
        if (item.id == undefined) {
            return createErrorResponse(req, STATUS.NOT_FOUND, "Missing '" + collectionName + "' id");
        }
        // tslint:disable-next-line:triple-equals
        if (id != item.id) {
            return createErrorResponse(req, STATUS.BAD_REQUEST, "\"" + collectionName + "\" id does not match item.id");
        }
        var existingIx = this.indexOf(collection, id);
        var body = { data: this.clone(item) };
        if (existingIx > -1) {
            collection[existingIx] = item;
            var res = this.config.put204 ?
                { headers: headers, status: STATUS.NO_CONTENT } :
                { headers: headers, body: body, status: STATUS.OK }; // successful; return entity
            return new ResponseOptions(res);
        }
        else {
            collection.push(item);
            return new ResponseOptions({ headers: headers, body: body, status: STATUS.CREATED });
        }
    };
    InMemoryBackendService.prototype.removeById = function (collection, id) {
        var ix = this.indexOf(collection, id);
        if (ix > -1) {
            collection.splice(ix, 1);
            return true;
        }
        return false;
    };
    /**
     * Reset the "database" to its original state
     */
    InMemoryBackendService.prototype.resetDb = function () {
        this.db = this.inMemDbService.createDb();
    };
    InMemoryBackendService.prototype.setPassThruBackend = function () {
        this.passThruBackend = undefined;
        if (this.config.passThruUnknownUrl) {
            try {
                // copied from @angular/http/backends/xhr_backend
                var browserXhr = this.injector.get(BrowserXhr);
                var baseResponseOptions = this.injector.get(ResponseOptions);
                var xsrfStrategy = this.injector.get(XSRFStrategy);
                this.passThruBackend = new XHRBackend(browserXhr, baseResponseOptions, xsrfStrategy);
            }
            catch (ex) {
                ex.message = 'Cannot create passThru404 backend; ' + (ex.message || '');
                throw ex;
            }
        }
    };
    InMemoryBackendService.decorators = [
        { type: Injectable },
    ];
    /** @nocollapse */
    InMemoryBackendService.ctorParameters = function () { return [
        { type: Injector, },
        { type: InMemoryDbService, },
        { type: InMemoryBackendConfigArgs, decorators: [{ type: Inject, args: [InMemoryBackendConfig,] }, { type: Optional },] },
    ]; };
    return InMemoryBackendService;
}());
//# sourceMappingURL=in-memory-backend.service.js.map