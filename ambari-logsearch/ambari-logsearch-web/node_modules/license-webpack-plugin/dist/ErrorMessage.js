"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ErrorMessage;
(function (ErrorMessage) {
    ErrorMessage["NO_PATTERN"] = "Please specify a regular expression as the pattern property on the plugin options.";
    ErrorMessage["UNACCEPTABLE_PATTERN_NOT_REGEX"] = "The unacceptablePattern should be a regular expression";
    ErrorMessage["NO_PROJECT_ROOT"] = "No project root found! Is your webpack config located underneath the same directory as your project?";
    ErrorMessage["MULTIPLE_LICENSE_AMBIGUITY"] = "Package {0} contains multiple licenses, defaulting to first one: {1}. Use the licenseTypeOverrides option to specify a specific license for this module.";
    ErrorMessage["UNNACEPTABLE_LICENSE"] = "Package {0} contains an unacceptable license: {1}";
    ErrorMessage["OUTPUT_TEMPLATE_NOT_EXIST"] = "Output template file does not exist: {0}";
    ErrorMessage["NO_LICENSE_OVERRIDE_FILE_FOUND"] = "No license override found for {0} with name {1}";
    ErrorMessage["NO_LICENSE_FILE"] = "Could not find a license file for {0}, defaulting to license name found in package.json: {1}";
})(ErrorMessage || (ErrorMessage = {}));
exports.ErrorMessage = ErrorMessage;
