import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import FinancialStoryBoard from './pages/FinancialStoryBoard';
import Dashboard from './pages/Dashboard';
import Accounts from './pages/Accounts';
import ATM from './pages/ATM';
import Transactions from './pages/Transactions';
import Investments from './pages/Investments';
import Messaging from './pages/Messaging'; 
import StockTicker from './pages/StockTicker';
import APIs from './pages/APIs';
import SpeakWithData from './pages/SpeakWithData';
import Login from './pages/Login';
import Graph from './pages/Graph'; 
import CreditCardPurchase from './pages/CreditCardPurchase'; 
import ProtectedRoute from './ProtectedRoute';
import styled from 'styled-components';
import GlobalStyle from './globalStyles';

const MainContent = styled.div`
  margin-left: 350px; /* Match the sidebar width */
  padding: 20px;
`;

function App() {
  return (
    <>
      <GlobalStyle />
      <Router>
        <Sidebar />
        <MainContent>
          <Routes>
            <Route path="/" element={<Login />} />
            <Route
              path="/financialstoryboard"
              element={
                <ProtectedRoute>
                  <FinancialStoryBoard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/accounts"
              element={
                <ProtectedRoute>
                  <Accounts />
                </ProtectedRoute>
              }
            />
            <Route
              path="/atm"
              element={
                <ProtectedRoute>
                  <ATM />
                </ProtectedRoute>
              }
            />
            <Route
              path="/transactions"
              element={
                <ProtectedRoute>
                  <Transactions />
                </ProtectedRoute>
              }
            />
            <Route
              path="/messaging"
              element={
                <ProtectedRoute>
                  <Messaging />
                </ProtectedRoute>
              }
            />
            <Route
              path="/stockTicker"
              element={
                <ProtectedRoute>
                  <StockTicker />
                </ProtectedRoute>
              }
            />
            <Route
              path="/creditcardpurchase"
              element={
                <ProtectedRoute>
                  <CreditCardPurchase />
                </ProtectedRoute>
              }
            />
            <Route
              path="/investments"
              element={
                <ProtectedRoute>
                  <Investments />
                </ProtectedRoute>
              }
            />
            <Route
              path="/stockticker"
              element={
                <ProtectedRoute>
                  <StockTicker />
                </ProtectedRoute>
              }
            />
            <Route
              path="/apis"
              element={
                <ProtectedRoute>
                  <APIs />
                </ProtectedRoute>
              }
            />
            <Route
              path="/speakwithdata"
              element={
                <ProtectedRoute>
                  <SpeakWithData />
                </ProtectedRoute>
              }
            />
            <Route
              path="/graph"
              element={
                <ProtectedRoute>
                  <Graph />
                </ProtectedRoute>
              }
            />
          </Routes>
        </MainContent>
      </Router>
    </>
  );
}

export default App;
