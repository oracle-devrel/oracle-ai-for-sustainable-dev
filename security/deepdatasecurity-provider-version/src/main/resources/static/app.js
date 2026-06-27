(async () => {
  const statusOutput = document.querySelector("#statusOutput");
  const resultOutput = document.querySelector("#resultOutput");
  const signInButton = document.querySelector("#signInButton");
  const queryButton = document.querySelector("#queryButton");
  const signOutButton = document.querySelector("#signOutButton");

  let msalClient;
  let config;

  const write = (element, value) => {
    element.textContent = typeof value === "string" ? value : JSON.stringify(value, null, 2);
  };

  const setSignedIn = (account) => {
    queryButton.disabled = !account;
    signOutButton.disabled = !account;
    signInButton.disabled = !!account;
  };

  const setUnavailable = () => {
    signInButton.disabled = true;
    queryButton.disabled = true;
    signOutButton.disabled = true;
  };

  const currentAccount = () => {
    const active = msalClient.getActiveAccount();
    if (active) {
      return active;
    }
    const accounts = msalClient.getAllAccounts();
    if (accounts.length > 0) {
      msalClient.setActiveAccount(accounts[0]);
      return accounts[0];
    }
    return null;
  };

  const getAccessToken = async () => {
    const account = currentAccount();
    if (!account) {
      throw new Error("Sign in first.");
    }

    const request = {
      account,
      scopes: [config.scope]
    };

    try {
      const response = await msalClient.acquireTokenSilent(request);
      return response.accessToken;
    } catch (error) {
      const response = await msalClient.acquireTokenPopup(request);
      return response.accessToken;
    }
  };

  const callJson = async (path, token) => {
    const response = await fetch(path, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(`${response.status} ${response.statusText}: ${JSON.stringify(body)}`);
    }
    return body;
  };

  const refreshStatus = async () => {
    const account = currentAccount();
    setSignedIn(account);
    const health = await callJson("/deepsec/health");
    write(statusOutput, {
      health,
      signedInAs: account ? account.username : null
    });
  };

  signInButton.addEventListener("click", async () => {
    try {
      if (!msalClient) {
        throw new Error("Browser authentication is not ready.");
      }
      const response = await msalClient.loginPopup({ scopes: [config.scope] });
      msalClient.setActiveAccount(response.account);
      await refreshStatus();
      write(resultOutput, "Signed in.");
    } catch (error) {
      write(resultOutput, error.message);
    }
  });

  signOutButton.addEventListener("click", async () => {
    const account = currentAccount();
    if (account) {
      await msalClient.logoutPopup({ account });
    }
    write(resultOutput, "Signed out.");
    await refreshStatus();
  });

  queryButton.addEventListener("click", async () => {
    try {
      const token = await getAccessToken();
      const whoami = await callJson("/deepsec/whoami", token);
      const query = await callJson("/deepsec/query", token);
      write(resultOutput, { whoami, query });
    } catch (error) {
      write(resultOutput, error.message);
    }
  });

  try {
    if (!window.msal) {
      throw new Error("MSAL browser library did not load. Check browser network access to the configured script.");
    }
    config = await callJson("/deepsec/browser-config");
    if (!config.tenantId || !config.clientId || !config.scope) {
      throw new Error("Browser auth config is incomplete.");
    }
    msalClient = new msal.PublicClientApplication({
      auth: {
        clientId: config.clientId,
        authority: `https://login.microsoftonline.com/${config.tenantId}`,
        redirectUri: window.location.origin
      },
      cache: {
        cacheLocation: "sessionStorage"
      }
    });
    await msalClient.initialize();
    await refreshStatus();
  } catch (error) {
    setUnavailable();
    write(statusOutput, error.message);
  }
})();
