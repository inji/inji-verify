import React from "react";
import AlertMessage from "../../../components/commons/AlertMessage";
import { render } from "@testing-library/react";
import { Provider } from "react-redux";
import configureMockStore from "redux-mock-store";
import { PreloadedState } from "../../../redux/features/verification/verification.slice";
import { AlertMessages } from "../../../utils/config";

const mockStore = configureMockStore();
const store = mockStore({
  ...PreloadedState,
  alert: AlertMessages().qrUploadSuccess,
});

describe("AlertMessage", () => {
  test("renders alert content element", () => {
    const { container } = render(
      <Provider store={store}>
        <AlertMessage />
      </Provider>
    );

    expect(container.querySelector("#alert-message")).toBeInTheDocument();
  });
});
