from pymongo import MongoClient
import os

# Set this to True to simulate crash between updates
simulate_crash = True

# Connect to MongoDB
client = MongoClient("mongodb://[financial:Welcome12345@]IJ1TYZIR3WPWLPE-FINANCIALDB.adb.eu-frankfurt-1.oraclecloudapps.com:27017/[financial]?authMechanism=PLAIN&authSource=$external&ssl=true&retryWrites=false&loadBalanced=tru")
db = client["testdb"]
collection = db["testcollection"]

# Start session and transaction
session = client.start_session()
session.start_transaction()

try:
    # First update
    collection.update_one({"_id": 1}, {"$set": {"balance": 500}}, session=session)
    print("First update done.")

    # Simulate crash
    if simulate_crash:
        print("Simulating crash...")
        os._exit(1)  # Force hard crash

    # Second update
    collection.update_one({"_id": 2}, {"$set": {"balance": 300}}, session=session)
    print("Second update done.")

    # Commit transaction
    session.commit_transaction()
    print("Transaction committed.")

except Exception as e:
    print("Exception occurred:", str(e))
    session.abort_transaction()
    print("Transaction aborted.")

finally:
    session.end_session()
    client.close()