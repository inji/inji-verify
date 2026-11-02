import { createSlice } from "@reduxjs/toolkit";
import { defaultLanguage, selected_language } from "../../../utils/i18n";

const initialState = {
  language: selected_language || defaultLanguage,
};

export const commonSlice = createSlice({
  name: "common",
  initialState,
  reducers: {
    storeLanguage: (state, action) => {
      state.language = action.payload.language;
    },
  },
});

export const { storeLanguage } = commonSlice.actions;

export default commonSlice.reducer;
