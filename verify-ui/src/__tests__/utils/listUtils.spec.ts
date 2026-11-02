import { describe, test, expect } from "@jest/globals";
import { getKeyedItems } from "../../utils/listUtils";

describe("list identities", () => {
  test("preserves identities when items are reordered", () => {
    const first = getKeyedItems(["first", "second"], (value) => value);
    const reordered = getKeyedItems(["second", "first"], (value) => value);
    expect(reordered.map(({ key }) => key)).toEqual([first[1].key, first[0].key]);
  });

  test("assigns distinct identities to duplicate content", () => {
    const items = getKeyedItems(["same", "same", "other"], (value) => value);
    expect(new Set(items.map(({ key }) => key)).size).toBe(3);
    expect(items.map(({ item }) => item)).toEqual(["same", "same", "other"]);
  });
});
