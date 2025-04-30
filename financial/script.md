Financial Application Workshop - Presenter Script

Author: [Your Name Here]Date: April 2025, Version [1.0]

Copyright © 2025, Oracle and/or its affiliatesConfidential – Oracle Internal

Presenter Script

Introduction

"Welcome! Today we'll be exploring a comprehensive financial application built on a full-stack microservices architecture. It uses the most popular development technologies including React for the frontend and multiple backend languages like Java, Node.js, Python, .NET, Go, Rust, and even WASM. Oracle Database is at the core, not just as storage, but as a powerhouse driving APIs, microservices, and AI. Let's dive into the labs to see how this all comes together."

Lab 1: FinTech / Bank APIs

"We'll start with how easily we can expose financial data as APIs using Oracle REST Data Services, or ORDS. 
There are a large variety of Fintech APIs for data and processes. 
These APIs include Payments, Banking and Open Banking, Trading and Investment, Credit and Lending, 
Know Your Customer and Identity Verification, Financial Data & Analytics, Insurance, Cryptocurrency, 
Accounting and Reconciliation, etc.
In less than a minute, you can create APIs from existing data or processes. We'll also compare this to traditional API creation methods to see the massive time savings."
Simple example

Lab 2: DevOps: Kubernetes, Microservices, and Observability

"Next, we look at managing microservices with Oracle Backend for Microservices and OpenTelemetry. What’s unique here is our ability to trace into the database itself, something no other vendor can offer. We'll see how organizations like LOLC simplify Kubernetes management and gain powerful SQL-based querying of logs, traces, and metrics."

Lab 3: Account Management

"Now let's create and query financial accounts. We'll create accounts using a MongoDB API and query them instantly with SQL, thanks to Oracle's JSON Duality. Santander leverages this to offer the flexibility of NoSQL with the consistency of SQL. We'll even simulate a crash to show Oracle's superior transaction handling."

Lab 4: Transfer to External Bank

"We'll now simulate transferring funds between banks. Using Spring Boot and MicroTx, we'll show how Oracle's lock-free reservations and auto-compensating sagas make transfers reliable and code-light. Inspired by work at the University of Naples, this shows about 80% less code and automatic recovery even during failures."

Lab 5: Credit Card Purchases, Fraud, and Money Laundering

"This lab focuses on fraud detection and money laundering prevention. Credit card purchases use Oracle's Globally Distributed Database. Fraud patterns are detected with Python and Oracle Spatial, while money laundering detection leverages Oracle Graph. Events are coordinated in real-time using Knative Eventing and CloudEvents, as seen at AMEX."

Lab 6: Transfer to Brokerage Accounts

"In this lab, we’ll look at event-driven architecture using Kafka. When Kafka is used with Postgres or MongoDB, crashes can cause data loss or duplication. With Oracle’s TxEventQ, we’ll show how Oracle ensures transactional event delivery without needing extra code. FSGUB benefits from this reliability daily."

Lab 7: Stock Ticker and Buy/Sell Stock

"Next, we'll experience high-speed stock ticker updates and transactions using TrueCache. Unlike Redis, TrueCache uses SQL directly, not a proprietary API, making it simpler and more secure. We'll showcase cross-language support including Java, JavaScript, Python, .NET, Go, Rust, and PL/SQL, inspired by NYSE-grade requirements."

Lab 8: Personalized Financial Insights

"Now we move into AI-driven financial insights. We'll use Oracle's Vector Search, AI Agents, and Modular Chain Processing (MCP) to personalize insights for users. Merrill Lynch uses similar setups to securely connect backend Oracle databases to AI agent calls, even from within Java and JavaScript inside the database."

Lab 9: Speak with Your Financial Data

"Finally, we’ll bring everything together with conversational AI. By using NL2SQL/Select AI, Oracle AI Explorer, and Speech AI, we'll allow users to literally ‘speak’ to their financial data. Whether querying portfolio changes or transaction details, this replicates real-world use cases like those in financial call centers."

Conclusion

"Today we saw how Oracle uniquely combines APIs, microservices, data consistency, event-driven architecture, caching, vector search, and conversational AI into one seamless financial application. Thank you for joining — we hope this inspires new innovations in your organizations!"

