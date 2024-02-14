import { PlatformRef, Provider } from '@angular/core';
export * from './private_export';
export { VERSION } from './version';
/**
 * @experimental
 */
export declare const RESOURCE_CACHE_PROVIDER: Provider[];
/**
 * @stable
 */
export declare const platformBrowserDynamic: (extraProviders?: Provider[] | undefined) => PlatformRef;
