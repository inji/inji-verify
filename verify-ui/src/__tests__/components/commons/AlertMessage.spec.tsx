import React from "react";
import AlertMessage from "../../../components/commons/AlertMessage";
import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import configureMockStore from "redux-mock-store";
import { PreloadedState } from "../../../redux/features/verification/verification.slice";
import { AlertMessages } from "../../../utils/config";
import "../../../utils/i18n";

const mockStore = configureMockStore();
const store = mockStore({
  verification: PreloadedState,
  alert: { ...AlertMessages().qrUploadSuccess, open: true },
});

describe("AlertMessage", () => {
  test("renders alert message content", () => {
    render(
      <Provider store={store}>
        <AlertMessage />
      </Provider>
    );

    expect(
      screen.getByText(AlertMessages().qrUploadSuccess.message ?? "")
    ).toBeInTheDocument();
  });
});
