# Example Client for the Realtime Speech Web SDK

This is an example client that demonstrates how to connect to the OCI Realtime Speech service, and how to handle results. 

This has 2 components, a server and a react client. The server has access to OCI credentials/auth providers, and makes a call to the speech service to fetch a token. This token is passed to the web client to securely connect directly to the realtime speech service.

Follow these instructions to build/test the client.

## Parameter Changes

You'll need to fill in the following parameters. 

In `index.ts`:

- `compartmentId`: The target compartment ID in which you want to use the realtime speec service. If you're using customizations, make they are in the same compartment.
- `region`: Your target region e.g. "us-ashburn-1"
- `provider`: Your authentication details provider.

In `react-client/src/App.tsx`:

- `realtimeClientParameters`: This is the place to declare partial/final silence thresholds, language, model domain (generic/medical), customizations, etc.

For customizations, populate the array with the OCIDs of valid, active customizations.

See this for more: https://docs.oracle.com/en-us/iaas/api/#/en/speech/20220101/datatypes/RealtimeParameters


## Build Instructions

You can run the following to build both the server and client:
```bash
npm run setup
```

If you want to buld the client the client/server separately, feel free to run `npm install` in their respective directories.


## Run Instructions

You can do the following to run the example. It is recommended to run both the server and client simultaneously.

```bash
npm start
```

There are a few more start scripts available:

- `start:server`: Starts just the server
- `start:dev`: Starts in dev mode
- `start:client`: Starts just the web (React) client

Both the build/run commands should be run from the content root. 

Once you start both the server and the react client, you should be able to load the client on your browser at `http://localhost:4884`. The server is started at `http://localhost:8448`

Press "Start Session" to begin the transcription. You should be able to see the results on the screen.

"Request Final Results" is a command that can be sent to the speech service to return final results instantly. Based on the partial/final silence thresholds defined in the realtime parameters in the react client, results may take time to arrive. This is a way of receiving the results if you need to close the client, without having to wait for the stabilized final results to arrive.



