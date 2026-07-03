(async () => {
  const statusOutput = document.querySelector("#statusOutput");
  const resultOutput = document.querySelector("#resultOutput");
  const signInButton = document.querySelector("#signInButton");
  const queryButton = document.querySelector("#queryButton");
  const signOutButton = document.querySelector("#signOutButton");

  const write = (element, value) => {
    element.textContent = typeof value === "string" ? value : JSON.stringify(value, null, 2);
  };

  const callJson = async (path) => {
    const response = await fetch(path, {
      headers: { Accept: "application/json" }
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const error = new Error(`${response.status} ${response.statusText}: ${JSON.stringify(body)}`);
      error.status = response.status;
      throw error;
    }
    return body;
  };

  const setSignedIn = (signedIn) => {
    signInButton.disabled = signedIn;
    queryButton.disabled = !signedIn;
    signOutButton.disabled = !signedIn;
  };

  const refreshStatus = async () => {
    const health = await callJson("/deepsec/health");
    try {
      const whoami = await callJson("/deepsec/whoami");
      setSignedIn(true);
      write(statusOutput, {
        health,
        signedInAs: whoami.username
      });
    } catch (error) {
      if (error.status !== 401) {
        throw error;
      }
      setSignedIn(false);
      write(statusOutput, {
        health,
        signedInAs: null
      });
    }
  };

  signInButton.addEventListener("click", () => {
    window.location.assign("/oauth2/authorization/entra");
  });

  signOutButton.addEventListener("click", () => {
    window.location.assign("/logout");
  });

  queryButton.addEventListener("click", async () => {
    try {
      const whoami = await callJson("/deepsec/whoami");
      const query = await callJson("/deepsec/query");
      write(resultOutput, { whoami, query });
      await refreshStatus();
    } catch (error) {
      if (error.status === 401) {
        window.location.assign("/oauth2/authorization/entra");
        return;
      }
      write(resultOutput, error.message);
    }
  });

  try {
    await refreshStatus();
  } catch (error) {
    setSignedIn(false);
    write(statusOutput, error.message);
  }
})();
