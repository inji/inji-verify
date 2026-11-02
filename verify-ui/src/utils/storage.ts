export class storage {
  static readonly SELECTED_LANGUAGE = "selected_language";
  static readonly ESSENTIAL_CLAIM = "essential_claim";
  static readonly setItem = (key: string, value: any) => {
    switch (key) {
      case storage.SELECTED_LANGUAGE:
        sessionStorage.setItem(key, JSON.stringify(value));
        break;
      case storage.ESSENTIAL_CLAIM: {
        const storedClaims = JSON.parse(localStorage.getItem(key) || "[]");
        if (!storedClaims.some((claim: { type: any }) => claim.type === value.type)) {
            storedClaims.push(value);
            localStorage.setItem(key, JSON.stringify(storedClaims));
          }
        break;
      }
      default:
        break;
    }
  };

  static readonly getItem = (key: string) => {
    let data = key === storage.SELECTED_LANGUAGE
      ? sessionStorage.getItem(key)
      : localStorage.getItem(key);
    if (data) {
      data = JSON.parse(data);
    }
    return data;
  };
}
