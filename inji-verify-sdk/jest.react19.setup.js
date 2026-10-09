const React = require("react");

if (!React.version.startsWith("19.")) {
  throw new Error(`React 19 test expected React 19, but loaded ${React.version}.`);
}
