/**
 * NOTE:
 * The legacy verification saga has been removed in favour of the new
 * `@injistack/react-inji-verify-sdk` based flow. This BDD-style saga test
 * is therefore no longer meaningful and is kept only as a skipped placeholder
 * to avoid hard Jest failures from outdated imports.
 */

describe.skip("Verification Saga (deprecated)", () => {
    test("placeholder", () => {
        expect(true).toBe(true);
    });
});
