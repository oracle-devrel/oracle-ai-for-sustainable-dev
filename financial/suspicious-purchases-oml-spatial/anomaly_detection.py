import oracledb
import geopandas as gpd
import shapely
import folium 
import pandas as pd
import numpy as np
from st_dbscan import ST_DBSCAN
oracledb.defaults.fetch_lobs = False

connection = None
gdf = None
gdfAnomaly = None
    
def create_connection():
    
    global connection
    my_pwd = open('./my-pwd.txt','r').readline().strip()
    my_dsn = open('./my-dsn.txt','r').readline().strip()
    connection = oracledb.connect(user="admin", password=my_pwd, dsn=my_dsn)
     
def get_cluster_centroids(cust):
    
    global gdf
    cursor = connection.cursor()
    cursor.execute("TRUNCATE TABLE transaction_labels")
    cursor.execute("""
     SELECT a.cust_id,  a.trans_id, a.trans_epoch_date, 
           (lonlat_to_proj_geom(b.lon,b.lat)).get_wkt() 
     FROM transactions a, locations b
     WHERE a.location_id=b.location_id
     AND cust_id=:cust""", cust=cust)
    gdf = gpd.GeoDataFrame(cursor.fetchall(), columns = ['cust_id', 'trans_id', 'epoch_date', 'geometry'])
    gdf['geometry'] = shapely.from_wkt(gdf['geometry'])

    # first convert to pandas dataframe
    df = pd.DataFrame(data={'time': gdf.epoch_date, 'x': gdf.geometry.x, 'y': gdf.geometry.y, 'trans_id':  gdf.trans_id, 'cust_id':gdf.cust_id})

    # then convert to numpy array
    data = df.values
    data = np.int_(data)

    st_cluster = ST_DBSCAN(eps1 = 5000, eps2 = 3000000, min_samples = 5)
    st_cluster.fit(data)

    df = pd.DataFrame(data={'trans_id': df.trans_id, 'label': st_cluster.labels})

    cursor.executemany("""
     INSERT INTO transaction_labels 
     VALUES (:1, :2)""", 
     list(df[['trans_id','label']].itertuples(index=False, name=None)))
    
    connection.commit()

    # st cluster centroids for customer
    cursor = connection.cursor()
    cursor.execute("""
     SELECT label, min(trans_epoch_date) as min_time, max(trans_epoch_date) as max_time,
             SDO_AGGR_CENTROID(
              SDOAGGRTYPE(lonlat_to_proj_geom(b.lon,b.lat), 0.005)).get_wkt() as geometry, 
             count(*) as trans_count
     FROM transactions a, locations b, transaction_labels c
     WHERE a.location_id=b.location_id
     AND a.trans_id=c.trans_id
     AND c.label != -1
     GROUP BY label
           """)
    gdf = gpd.GeoDataFrame(cursor.fetchall(), columns = ['label','min_time','max_time','geometry','trans_count'])
    gdf['geometry'] = shapely.from_wkt(gdf['geometry'])
    gdf = gdf.set_crs(3857)
    
    return gdf

def get_anomalies(cust):
  
    global gdfAnomaly;
    cursor = connection.cursor()
    cursor.execute("""
    WITH 
       x as (
           SELECT a.cust_id, a.location_id, a.trans_id, a.trans_epoch_date, 
                  lonlat_to_proj_geom(b.lon,b.lat) as proj_geom, c.label
           FROM transactions a, locations b, transaction_labels c
           WHERE a.location_id=b.location_id
           AND a.trans_id=c.trans_id ),
       y as (
           SELECT label, min(trans_epoch_date) as min_time, max(trans_epoch_date) as max_time,
                  SDO_AGGR_CENTROID(
                      SDOAGGRTYPE(lonlat_to_proj_geom(b.lon,b.lat), 0.005)) as proj_geom, 
                  count(*) as trans_count
           FROM transactions a, locations b, transaction_labels c
           WHERE a.location_id=b.location_id
           AND a.trans_id=c.trans_id
           AND c.label != -1
           GROUP BY label)
     SELECT x.cust_id, x.trans_epoch_date, (x.proj_geom).get_wkt(), x.trans_id, x.label, y.label,
            round(sdo_geom.sdo_distance(x.proj_geom, y.proj_geom, 0.05, 'unit=KM'))
     FROM x, y
     WHERE x.trans_epoch_date between y.min_time and y.max_time
     AND x.label!=y.label
     AND x.label=-1
     AND sdo_within_distance(x.proj_geom, y.proj_geom, 'distance=500 unit=KM') = 'FALSE'
           """)
    gdfAnomaly = gpd.GeoDataFrame(cursor.fetchall(), columns = ['cust_id','trans_epoch_date','geometry', 'trans_id','label','outlier_to_label','distance'])
    gdfAnomaly['geometry'] = shapely.from_wkt(gdfAnomaly['geometry'])
    gdfAnomaly = gdfAnomaly.set_crs(3857)
    return gdfAnomaly

def get_map():
    
    m = gdf.explore(tiles="CartoDB positron", marker_type='circle_marker',marker_kwds={"radius":"5"}, 
                style_kwds={"color":"blue","fillColor":"blue", "fillOpacity":"1"})
    m = gdfAnomaly.explore(m=m, marker_type='circle_marker', marker_kwds={"radius":"5"}, 
                       style_kwds={"color":"red","fillColor":"red", "fillOpacity":"1"} )
    return m