interface Options {
    pattern: RegExp;
    unacceptablePattern?: RegExp;
    abortOnUnacceptableLicense?: boolean;
    perChunkOutput?: boolean;
    licenseFilenames?: string[];
    licenseTemplateDir?: string;
    licenseFileOverrides?: {
        [key: string]: string;
    };
    licenseTypeOverrides?: {
        [key: string]: string;
    };
    outputTemplate?: string;
    outputFilename?: string;
    suppressErrors?: boolean;
    includePackagesWithoutLicense?: boolean;
    addBanner?: boolean;
    bannerTemplate?: string;
    includedChunks?: string[];
    excludedChunks?: string[];
    additionalPackages?: string[];
}
export { Options };
