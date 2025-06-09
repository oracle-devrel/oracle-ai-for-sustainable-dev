import React from 'react';
import ReactDOM from 'react-dom/client'; // Use 'react-dom/client' for React 18
import App from './App';
import { AuthProvider } from './AuthContext';

const root = ReactDOM.createRoot(document.getElementById('root')); // Create a root
root.render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
);

