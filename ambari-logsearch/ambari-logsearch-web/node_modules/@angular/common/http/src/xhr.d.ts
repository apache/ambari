import { Observable } from 'rxjs/Observable';
import { HttpBackend } from './backend';
import { HttpRequest } from './request';
import { HttpEvent } from './response';
/**
 * A wrapper around the `XMLHttpRequest` constructor.
 *
 * @experimental
 */
export declare abstract class XhrFactory {
    abstract build(): XMLHttpRequest;
}
/**
 * A factory for @{link HttpXhrBackend} that uses the `XMLHttpRequest` browser API.
 *
 * @experimental
 */
export declare class BrowserXhr implements XhrFactory {
    constructor();
    build(): any;
}
/**
 * An `HttpBackend` which uses the XMLHttpRequest API to send
 * requests to a backend server.
 *
 * @experimental
 */
export declare class HttpXhrBackend implements HttpBackend {
    private xhrFactory;
    constructor(xhrFactory: XhrFactory);
    /**
     * Process a request and return a stream of response events.
     */
    handle(req: HttpRequest<any>): Observable<HttpEvent<any>>;
}
