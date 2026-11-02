import { Storage } from "../../utils/storage";

describe("storage helpers", () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  test("stores and reads selected language", () => {
    Storage.setItem(Storage.SELECTED_LANGUAGE, "fr");
    expect(Storage.getItem(Storage.SELECTED_LANGUAGE)).toBe("fr");
  });

  test("adds essential claims without duplicates", () => {
    const claim = { type: "Type1", name: "Credential" };
    Storage.setItem(Storage.ESSENTIAL_CLAIM, claim);
    Storage.setItem(Storage.ESSENTIAL_CLAIM, claim);
    expect(Storage.getItem(Storage.ESSENTIAL_CLAIM)).toEqual([claim]);
  });

  test("ignores unsupported keys and missing values", () => {
    Storage.setItem("other", "value");
    expect(Storage.getItem("other")).toBeNull();
    expect(Storage.getItem(Storage.SELECTED_LANGUAGE)).toBeNull();
  });
});
