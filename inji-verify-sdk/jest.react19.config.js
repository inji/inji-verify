const baseConfig = require("./jest.config");

const react19Modules = "<rootDir>/node_modules/.peer-test-react19/node_modules";

module.exports = {
  ...baseConfig,
  moduleNameMapper: {
    "^react$": `${react19Modules}/react`,
    "^react/(.*)$": `${react19Modules}/react/$1`,
    "^react-dom$": `${react19Modules}/react-dom`,
    "^react-dom/(.*)$": `${react19Modules}/react-dom/$1`,
    ...baseConfig.moduleNameMapper,
  },
  setupFilesAfterEnv: ["<rootDir>/jest.react19.setup.js"],
};
