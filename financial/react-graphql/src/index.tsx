import React from 'react';
import { render } from 'react-dom';
import App from './App';

import {
    ApolloClient,
    InMemoryCache,
    ApolloProvider
} from "@apollo/client";

const client = new ApolloClient({
    uri: 'http://localhost:8080/apis/graphql',
    cache: new InMemoryCache()
});

render(
    <ApolloProvider client={client}>
        <App />
    </ApolloProvider>,
    document.getElementById('root'),
);


