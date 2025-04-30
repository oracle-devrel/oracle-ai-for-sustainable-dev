Write up a demo script using the format and style in the attached docx file but for the following workshop.... 
"
This financial application and its corresponding workshop are aimed at both financial line of business experts and developers.
It is intended to allow the two personas to have a common ground and shared understanding of the possibilities and finer details of both the business solutions and the development architecture, features, etc. involved in them.

Each lab/part of the application describes various aspects including... 
- business process
- dev/tech involved
- existing companies that use such solutions
- differentiators
- low level details of code, tests, and comparisons with other solutions/vendors
- brief video walkthrough

The workshop in addition goes into further details on...
- migration
- scaling and sizing
- expert contacts in product management, etc. 

Introduction
The application used is a full-stack, microservices based architecture, using all of the latest and most popular developer technologies.
The frontend is written predominantly in React.
The mid-tier is written in various languages and platforms such as Java and Spring Boot, React and Node, Python, .NET, Go, Rust, and WASM
Oracle Database serves as the backend, however, Oracle database is far from just a storage mechanism as you will see as you go through the labs.
A number or Oracle and Oracle database technologies are used.

Lab 1 
Process: FinTech/Bank APIs: Access/use financial data or processes from APIs.  Display the largest changes in portfolio
Tech Used: Oracle Rest Data Services (ORDS)
Reference: Bank Of India
Differentiators: Create APIs from data an processes in under a minute
Low-level details: Comparison of speed with other API creation methods as well as the advantage of ORDS

Lab 2 
Process: DevOps: Kubernetes, Microservices, and Observability
Tech Used: Oracle Backend For Microservices and AI, OpenTelemetry, Grafan
Reference: LOLC
Differentiators: Simplified management of Kubernetes and microservices, one of a kind trace exporter in the database, giving the ability to trace *into* the database that no other vendor has, as well as metrics and log exports - all exporters accept SQL for the most advanced querying of data.
Low-level details: Realize the amount of architecture that is automated and the convenience, and time saving time-to-mark advantages

Lab 3 
Process: Create and Query Accounts: Create with MongoDB API, query with SQL
Tech Used: MongoDB API adapter, JSON Duality
Reference: Santander
Differentiators: Use JSON Duality for seamless SQL querying of the same data. No other database can do this.
Low-level details: Instigate crash and notice transactionality of Oracle Database for JSON and relational.

Lab 4
Process: Transfer funds between banks
Tech Used: Spring Boot, MicroTx, Lock-free reservations
Reference: U of Naples
Differentiators: The only database that provides auto-compensating sagas (microservice transactions) and highest throughput for hotspots. Simplified development (~80% less code)
Low-level details: Instigate crash and notice automatic recovery that is possible and the huge amount of error-prone code that would be required otherwise.

Lab 5 
Process: Credit card purchases, fraud, and money laundering
Tech Used: Credit card purchases are conducted using Oracle Globally Distributed Database. 
Fraud detection and visualization is conducted using OML4Py (Python) and Spatial.  
Money Laundering is detected using Oracle Graph. 
Events are sent using Knative Eventing and CloudEvents.
Reference: AMEX
Differentiators: 
Low-level details:

Lab 6 
Process: Transfer to brokerage accounts
Tech Used: Kafka and TxEventQ
Reference: FSGUB
Differentiators: 
Low-level details: Instigate crash and notice message duplication, message loss, data duplication, and additional code required when using Kafka with Postgres and MongoDB that is automatically and transactionally handled when using Kafka with Oracle Database.

Lab 7 
Process: Stock ticker and buy/sell stock
Tech Used: TrueCache, Polyglot (Java, JS, Python, .NET, Go, Rust, PL/SQL)
Reference: NYSE
Differentiators: Unlike Redis, True Cache uses SQL, not a proprietary API
Low-level details:

Lab 8 
Process: Personalized Financial Insights
Tech Used: Vector Search, AI Agents, and MCP
Reference: Merrill Lynch
Differentiators: Access data securely from Oracle Database hub. Even using JavaScript and Java from within the database to make MCP AI Agent calls
Low-level details:

Lab 9 Speak with your financial data
Process: Access/use financial data or processes from APIs.  Display the largest changes in portfolio
Tech Used: NL2SQL/Select AI, Vector Search, Oracle AI Explorer, Speech AI
Reference: various call centers
Differentiators:
Low-level details:
""
