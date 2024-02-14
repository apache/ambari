import { ErrorHandler, ModuleWithProviders, PlatformRef, Provider } from '@angular/core';
export declare const INTERNAL_BROWSER_PLATFORM_PROVIDERS: Provider[];
/**
 * @security Replacing built-in sanitization providers exposes the application to XSS risks.
 * Attacker-controlled data introduced by an unsanitized provider could expose your
 * application to XSS risks. For more detail, see the [Security Guide](http://g.co/ng/security).
 * @experimental
 */
export declare const BROWSER_SANITIZATION_PROVIDERS: Array<any>;
/**
 * @stable
 */
export declare const platformBrowser: (extraProviders?: Provider[]) => PlatformRef;
export declare function initDomAdapter(): void;
export declare function errorHandler(): ErrorHandler;
export declare function _document(): any;
/**
 * The ng module for the browser.
 *
 * @stable
 */
export declare class BrowserModule {
    constructor(parentModule: BrowserModule);
    /**
     * Configures a browser-based application to transition from a server-rendered app, if
     * one is present on the page. The specified parameters must include an application id,
     * which must match between the client and server applications.
     *
     * @experimental
     */
    static withServerTransition(params: {
        appId: string;
    }): ModuleWithProviders;
}
