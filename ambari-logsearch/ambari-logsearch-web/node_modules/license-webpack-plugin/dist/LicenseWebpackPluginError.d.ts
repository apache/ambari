import { ErrorMessage } from './ErrorMessage';
declare class LicenseWebpackPluginError extends Error {
    constructor(message: ErrorMessage, ...params: string[]);
}
export { LicenseWebpackPluginError };
