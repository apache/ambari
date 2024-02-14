import { Injector } from '@angular/core';
import { Connection, ConnectionBackend, Headers, Request, Response, ResponseOptions, URLSearchParams } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import 'rxjs/add/operator/delay';
/**
 * Create an error Response from an HTTP status code and error message
 */
export declare function createErrorResponse(req: Request, status: number, message: string): ResponseOptions;
/**
 * Create an Observable response from response options.
 */
export declare function createObservableResponse(req: Request, resOptions: ResponseOptions): Observable<Response>;
/**
 * Create a response from response options
 * and tell "ResponseObserver" (an `Observer<Response>`) to emit it.
 * The observer's observable is either completed or in error state after call.
 */
export declare function emitResponse(responseObserver: Observer<Response>, req: Request, resOptions: ResponseOptions): void;
/**
* Interface for object passed to an HTTP method override method
*/
export interface HttpMethodInterceptorArgs {
    requestInfo: RequestInfo;
    db: Object;
    config: InMemoryBackendConfigArgs;
    passThruBackend: ConnectionBackend;
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
export declare abstract class InMemoryDbService {
    /**
    * Creates a "database" hash whose keys are collection names
    * and whose values are arrays of collection objects to return or update.
    *
    * This method must be safe to call repeatedly.
    * Each time it should return a new object with new arrays containing new item objects.
    * This condition allows InMemoryBackendService to morph the arrays and objects
    * without touching the original source data.
    */
    abstract createDb(): {};
}
/**
* Interface for InMemoryBackend configuration options
*/
export declare abstract class InMemoryBackendConfigArgs {
    /**
     * false (default) if search match should be case insensitive
     */
    caseSensitiveSearch?: boolean;
    /**
     * default response options
     */
    defaultResponseOptions?: ResponseOptions;
    /**
     * delay (in ms) to simulate latency
     */
    delay?: number;
    /**
     * false (default) if ok when object-to-delete not found; else 404
     */
    delete404?: boolean;
    /**
     * false (default) if should pass unrecognized request URL through to original backend; else 404
     */
    passThruUnknownUrl?: boolean;
    /**
     * true (default) should NOT return the entity (204) after a POST. false: return the entity (200).
     */
    post204?: boolean;
    /**
     * true (default) should NOT return the entity (204) after a PUT. false: return the entity (200).
     */
    put204?: boolean;
    /**
     * The base path to the api, e.g, 'api/'.
     * If not specified than `parseUrl` assumes it is the first path segment in the request.
     */
    apiBase?: string;
    /**
     * host for this service, e.g., 'localhost'
     */
    host?: string;
    /**
     * root path _before_ any API call, e.g., ''
     */
    rootPath?: string;
}
export declare function removeTrailingSlash(path: string): string;
/**
*  InMemoryBackendService configuration options
*  Usage:
*    InMemoryWebApiModule.forRoot(InMemHeroService, {delay: 600})
*
*  or if providing separately:
*    provide(InMemoryBackendConfig, {useValue: {delay: 600}}),
*/
export declare class InMemoryBackendConfig implements InMemoryBackendConfigArgs {
    constructor(config?: InMemoryBackendConfigArgs);
}
/**
 * Returns true if the the Http Status Code is 200-299 (success)
 */
export declare function isSuccess(status: number): boolean;
/**
* Interface for object w/ info about the current request url
* extracted from an Http Request
*/
export interface RequestInfo {
    req: Request;
    base: string;
    collection: any[];
    collectionName: string;
    headers: Headers;
    id: any;
    query: URLSearchParams;
    resourceUrl: string;
}
/**
 * Provide a `responseInterceptor` method of this type in your `inMemDbService` to
 * morph the response options created in the `collectionHandler`.
 */
export declare type ResponseInterceptor = (res: ResponseOptions, ri: RequestInfo) => ResponseOptions;
/**
 * Set the status text in a response:
 */
export declare function setStatusText(options: ResponseOptions): ResponseOptions;
/**
 *
 * Interface for the result of the parseUrl method:
 *   Given URL "http://localhost:8080/api/customers/42?foo=1 the default implementation returns
 *     base: 'api/'
 *     collectionName: 'customers'
 *     id: '42'
 *     query: new URLSearchParams('foo=1')
 *     resourceUrl: 'http://localhost/api/customers/'
 */
export interface ParsedUrl {
    base: string;
    collectionName: string;
    id: string;
    query: URLSearchParams;
    resourceUrl: string;
}
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
export declare class InMemoryBackendService {
    private injector;
    private inMemDbService;
    protected passThruBackend: ConnectionBackend;
    protected config: InMemoryBackendConfigArgs;
    protected db: Object;
    constructor(injector: Injector, inMemDbService: InMemoryDbService, config: InMemoryBackendConfigArgs);
    createConnection(req: Request): Connection;
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
    protected handleRequest(req: Request): Observable<Response>;
    /**
     * Add configured delay to response observable unless delay === 0
     */
    protected addDelay(response: Observable<Response>): Observable<Response>;
    /**
     * Apply query/search parameters as a filter over the collection
     * This impl only supports RegExp queries on string properties of the collection
     * ANDs the conditions together
     */
    protected applyQuery(collection: any[], query: URLSearchParams): any[];
    protected clone(data: any): any;
    protected collectionHandler(reqInfo: RequestInfo): Observable<Response>;
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
    protected commands(reqInfo: RequestInfo): Observable<Response>;
    protected delete({id, collection, collectionName, headers, req}: RequestInfo): ResponseOptions;
    protected findById(collection: any[], id: number | string): any;
    protected genId(collection: any): any;
    protected get({id, query, collection, collectionName, headers, req}: RequestInfo): ResponseOptions;
    protected getLocation(href: string): {
        host: any;
        protocol: any;
        port: any;
        pathname: any;
        search: string;
    };
    protected parseuri(str: string): any;
    protected indexOf(collection: any[], id: number): number;
    protected parseId(collection: {
        id: any;
    }[], id: string): any;
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
    protected parseUrl(url: string): ParsedUrl;
    protected post({collection, headers, id, req, resourceUrl}: RequestInfo): ResponseOptions;
    protected put({id, collection, collectionName, headers, req}: RequestInfo): ResponseOptions;
    protected removeById(collection: any[], id: number): boolean;
    /**
     * Reset the "database" to its original state
     */
    protected resetDb(): void;
    protected setPassThruBackend(): void;
}
