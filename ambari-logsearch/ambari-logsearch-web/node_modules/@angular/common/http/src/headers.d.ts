/**
 * Immutable set of Http headers, with lazy parsing.
 * @experimental
 */
export declare class HttpHeaders {
    /**
     * Internal map of lowercase header names to values.
     */
    private headers;
    /**
     * Internal map of lowercased header names to the normalized
     * form of the name (the form seen first).
     */
    private normalizedNames;
    /**
     * Complete the lazy initialization of this object (needed before reading).
     */
    private lazyInit;
    /**
     * Queued updates to be materialized the next initialization.
     */
    private lazyUpdate;
    constructor(headers?: string | {
        [name: string]: string | string[];
    });
    /**
     * Checks for existence of header by given name.
     */
    has(name: string): boolean;
    /**
     * Returns first header that matches given name.
     */
    get(name: string): string | null;
    /**
     * Returns the names of the headers
     */
    keys(): string[];
    /**
     * Returns list of header values for a given name.
     */
    getAll(name: string): string[] | null;
    append(name: string, value: string | string[]): HttpHeaders;
    set(name: string, value: string | string[]): HttpHeaders;
    delete(name: string, value?: string | string[]): HttpHeaders;
    private maybeSetNormalizedName(name, lcName);
    private init();
    private copyFrom(other);
    private clone(update);
    private applyUpdate(update);
}
