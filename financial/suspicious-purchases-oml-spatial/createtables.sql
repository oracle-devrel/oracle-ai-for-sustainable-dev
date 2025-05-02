# Create table for locations data
cursor.execute("""
 CREATE TABLE locations (
           location_id INTEGER,
           owner VARCHAR2(100),
           lon NUMBER,
           lat NUMBER)""")


# Load the locations data
import csv
BATCH_SIZE = 1000
with connection.cursor() as cursor:
    with open('locations.csv', 'r') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        #skip header
        next(csv_reader)
        #load data
        sql = "INSERT INTO locations VALUES (:1, :2, :3, :4)"
        data = []
        for line in csv_reader:
            data.append((line[0], line[1], line[2], line[3]))
            if len(data) % BATCH_SIZE == 0:
                cursor.executemany(sql, data)
                data = []
        if data:
            cursor.executemany(sql, data)
        connection.commit()

# Preview locations data
cursor = connection.cursor()
cursor.execute("SELECT * FROM locations")
for row in cursor.fetchmany(size=10):
    print(row)


# Create table for transactions data
cursor.execute("""
   CREATE TABLE transactions (
                  trans_id INTEGER,
                  location_id INTEGER,
                  trans_date DATE,
                  cust_id INTEGER)""")

# Load the transactions data
BATCH_SIZE = 1000
with connection.cursor() as cursor:
    with open('transactions.csv', 'r') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        #skip header
        next(csv_reader)
        #load data
        sql = "INSERT INTO transactions VALUES (:1, :2, TO_DATE(:3,'YYYY-MM-DD:HH24:MI:SS'), :4)"
        data = []
        for line in csv_reader:
            data.append((line[0], line[1], line[2], line[3]))
            if len(data) % BATCH_SIZE == 0:
                cursor.executemany(sql, data)
                data = []
        if data:
            cursor.executemany(sql, data)
        connection.commit()


# Preview transactions data
cursor = connection.cursor()
cursor.execute("SELECT * FROM transactions")
for row in cursor.fetchmany(size=10):
    print(row)

# Customer ID's
cursor = connection.cursor()
cursor.execute("SELECT DISTINCT cust_id FROM transactions ORDER BY cust_id")
for row in cursor.fetchall():
    print(row[0])


