from graphviz import Digraph

# To use this: `brew install Graphviz` and pip install Graphviz

# Create a new directed graph
dot = Digraph()

# Define each table and its fields
tables = {
    "ACCOUNTS": [
        "ACCOUNT_ID (PK)", "ACCOUNT_BALANCE", "CUSTOMER_ID", "ACCOUNT_NAME",
        "ACCOUNT_OPENED_DATE", "ACCOUNT_OTHER_DETAILS", "ACCOUNT_TYPE"
    ],
    "JOURNAL": [
        "JOURNAL_ID (PK)", "ACCOUNT_ID (FK)", "JOURNAL_AMOUNT",
        "JOURNAL_TYPE", "LRA_ID", "LRA_STATE"
    ],
    "LOCATIONS": [
        "LOCATION_ID (PK)", "OWNER", "LON", "LAT"
    ],
    "TRANSACTIONS": [
        "TRANS_ID (PK)", "LOCATION_ID (FK)", "TRANS_DATE",
        "CUST_ID (FK -> ACCOUNT_ID)", "TRANS_EPOCH_DATE"
    ],
    "TRANSACTION_LABELS": [
        "TRANS_ID (FK)", "LABEL"
    ]
}

# Add each table as a node
for table, fields in tables.items():
    label = f"{table}\n" + "\n".join(fields)
    dot.node(table, label=label, shape="box")

# Add relationships
dot.edge("ACCOUNTS", "JOURNAL", label="ACCOUNT_ID")
dot.edge("ACCOUNTS", "TRANSACTIONS", label="ACCOUNT_ID = CUST_ID")
dot.edge("LOCATIONS", "TRANSACTIONS", label="LOCATION_ID")
dot.edge("TRANSACTIONS", "TRANSACTION_LABELS", label="TRANS_ID")

# Render and display
dot.render('er_diagram', format='png', cleanup=False)
'er_diagram.png'