/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  rootDir: "./",
  testPathIgnorePatterns: ["<rootDir>/node_modules"],
  testMatch: [
    "<rootDir>/src/__tests__/**/*.spec.ts",
    "<rootDir>/src/__tests__/**/*.spec.tsx",
    "<rootDir>/src/__tests__/**/*.spec.js",
  ],
  setupFilesAfterEnv: [
    "<rootDir>/src/setupTests.ts",
    "<rootDir>/jest.setup.js",
  ],
  extensionsToTreatAsEsm: [".ts", ".tsx"],
  moduleNameMapper: {
    "\\.(svg)$": "<rootDir>/__mocks__/svgMock.js",
    "\\.(css|less|scss)$": "identity-obj-proxy",
    "\\.(jpg|jpeg|png|gif|webp|svg)$": "<rootDir>/__mocks__/svgMock.js",
  },
  transform: {
    "^.+\\.(ts|tsx|js)$": "ts-jest",
  },
  transformIgnorePatterns: ["/node_modules/(?!iso-639-3/)"],
};
