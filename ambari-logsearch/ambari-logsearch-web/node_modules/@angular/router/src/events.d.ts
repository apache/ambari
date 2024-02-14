/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Route } from './config';
import { RouterStateSnapshot } from './router_state';
/**
 * @whatItDoes Represents an event triggered when a navigation starts.
 *
 * @stable
 */
export declare class NavigationStart {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string);
    /** @docsNotRequired */
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered when a navigation ends successfully.
 *
 * @stable
 */
export declare class NavigationEnd {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string);
    /** @docsNotRequired */
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered when a navigation is canceled.
 *
 * @stable
 */
export declare class NavigationCancel {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    reason: string;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        reason: string);
    /** @docsNotRequired */
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered when a navigation fails due to an unexpected error.
 *
 * @stable
 */
export declare class NavigationError {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    error: any;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        error: any);
    /** @docsNotRequired */
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered when routes are recognized.
 *
 * @stable
 */
export declare class RoutesRecognized {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    /** @docsNotRequired */
    state: RouterStateSnapshot;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string, 
        /** @docsNotRequired */
        state: RouterStateSnapshot);
    /** @docsNotRequired */
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered before lazy loading a route config.
 *
 * @experimental
 */
export declare class RouteConfigLoadStart {
    route: Route;
    constructor(route: Route);
    toString(): string;
}
/**
 * @whatItDoes Represents an event triggered when a route has been lazy loaded.
 *
 * @experimental
 */
export declare class RouteConfigLoadEnd {
    route: Route;
    constructor(route: Route);
    toString(): string;
}
/**
 * @whatItDoes Represents the start of the Guard phase of routing.
 *
 * @experimental
 */
export declare class GuardsCheckStart {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    /** @docsNotRequired */
    state: RouterStateSnapshot;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string, 
        /** @docsNotRequired */
        state: RouterStateSnapshot);
    toString(): string;
}
/**
 * @whatItDoes Represents the end of the Guard phase of routing.
 *
 * @experimental
 */
export declare class GuardsCheckEnd {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    /** @docsNotRequired */
    state: RouterStateSnapshot;
    /** @docsNotRequired */
    shouldActivate: boolean;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string, 
        /** @docsNotRequired */
        state: RouterStateSnapshot, 
        /** @docsNotRequired */
        shouldActivate: boolean);
    toString(): string;
}
/**
 * @whatItDoes Represents the start of the Resolve phase of routing. The timing of this
 * event may change, thus it's experimental. In the current iteration it will run
 * in the "resolve" phase whether there's things to resolve or not. In the future this
 * behavior may change to only run when there are things to be resolved.
 *
 * @experimental
 */
export declare class ResolveStart {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    /** @docsNotRequired */
    state: RouterStateSnapshot;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string, 
        /** @docsNotRequired */
        state: RouterStateSnapshot);
    toString(): string;
}
/**
 * @whatItDoes Represents the end of the Resolve phase of routing. See note on
 * {@link ResolveStart} for use of this experimental API.
 *
 * @experimental
 */
export declare class ResolveEnd {
    /** @docsNotRequired */
    id: number;
    /** @docsNotRequired */
    url: string;
    /** @docsNotRequired */
    urlAfterRedirects: string;
    /** @docsNotRequired */
    state: RouterStateSnapshot;
    constructor(
        /** @docsNotRequired */
        id: number, 
        /** @docsNotRequired */
        url: string, 
        /** @docsNotRequired */
        urlAfterRedirects: string, 
        /** @docsNotRequired */
        state: RouterStateSnapshot);
    toString(): string;
}
/**
 * @whatItDoes Represents a router event, allowing you to track the lifecycle of the router.
 *
 * The sequence of router events is:
 *
 * - {@link NavigationStart},
 * - {@link RouteConfigLoadStart},
 * - {@link RouteConfigLoadEnd},
 * - {@link RoutesRecognized},
 * - {@link GuardsCheckStart},
 * - {@link GuardsCheckEnd},
 * - {@link ResolveStart},
 * - {@link ResolveEnd},
 * - {@link NavigationEnd},
 * - {@link NavigationCancel},
 * - {@link NavigationError}
 *
 * @stable
 */
export declare type Event = NavigationStart | NavigationEnd | NavigationCancel | NavigationError | RoutesRecognized | RouteConfigLoadStart | RouteConfigLoadEnd | GuardsCheckStart | GuardsCheckEnd | ResolveStart | ResolveEnd;
