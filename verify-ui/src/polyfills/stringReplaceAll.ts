/**
 * Shim for environments without String.prototype.replaceAll (e.g. Node 14, older browsers).
 * Keeps application code able to use replaceAll for static analysis / reliability rules.
 */
/* eslint-disable @typescript-eslint/no-explicit-any, no-extend-native */
if (!String.prototype.replaceAll) {
  Object.defineProperty(String.prototype, "replaceAll", {
    value: function (
      this: string,
      searchValue: string | RegExp,
      replaceValue: string | ((substring: string, ...args: any[]) => string),
    ): string {
      if (this == null) {
        throw new TypeError(
          "String.prototype.replaceAll called on null or undefined",
        );
      }

      const str = String(this);

      if (searchValue instanceof RegExp) {
        if (!searchValue.global) {
          throw new TypeError(
            "String.prototype.replaceAll called with a non-global RegExp argument",
          );
        }
        return str.replace(searchValue, replaceValue as any);
      }

      const search = String(searchValue);

      if (search === "") {
        throw new TypeError(
          "replaceAll must not be called with an empty string",
        );
      }

      if (typeof replaceValue === "function") {
        throw new TypeError(
          "This polyfill does not support function replacer with string search",
        );
      }

      return str.split(search).join(String(replaceValue));
    },
    writable: true,
    configurable: true,
  });
}

export {};
