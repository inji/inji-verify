import { storage } from "../../utils/storage";

describe("storage helpers", () => {
  beforeEach(() => localStorage.clear());

  test("stores and reads selected language", () => {
    storage.setItem(storage.SELECTED_LANGUAGE, "fr");
    expect(storage.getItem(storage.SELECTED_LANGUAGE)).toBe("fr");
  });

  test("adds essential claims without duplicates", () => {
    const claim = { type: "Type1", name: "Credential" };
    storage.setItem(storage.ESSENTIAL_CLAIM, claim);
    storage.setItem(storage.ESSENTIAL_CLAIM, claim);
    expect(storage.getItem(storage.ESSENTIAL_CLAIM)).toEqual([claim]);
  });

  test("ignores unsupported keys and missing values", () => {
    storage.setItem("other", "value");
    expect(storage.getItem("other")).toBeNull();
    expect(storage.getItem(storage.SELECTED_LANGUAGE)).toBeNull();
  });
});
