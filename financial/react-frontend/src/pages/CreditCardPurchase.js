import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
import { MapContainer, TileLayer, Marker, Popup, useMapEvent } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

// Fix default marker icons in Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const PageContainer = styled.div`
  background-color: ${bankerBg};
  color: ${bankerText};
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  overflow-y: auto;
`;

const MapWrapper = styled.div`
  width: 100%;
  height: 400px;
  margin-bottom: 20px;
`;

const Form = styled.form`
  width: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
  margin-bottom: 20px;
  color: ${bankerText};
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: ${bankerText};
`;

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
`;

const Button = styled.button`
  padding: 10px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;

  &:hover {
    background-color: ${bankerBg};
  }
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-bottom: 20px;
`;

const ToggleButton = styled.button`
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;
  font-weight: bold;

  &:hover {
    background-color: ${bankerBg};
  }
`;

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px;
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%;
`;

const NotebookWrapper = styled.div`
  width: 100%;
  height: 600px;
  margin-top: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  overflow: hidden;
`;

const TwoColumnContainer = styled.div`
  display: flex;
  gap: 32px;
  width: 100%;
  @media (max-width: 900px) {
    flex-direction: column;
    gap: 0;
  }
`;

const LeftColumn = styled.div`
  flex: 1;
  min-width: 320px;
`;

const RightColumn = styled.div`
  flex: 1;
  min-width: 320px;
  background: ${bankerPanel};
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  padding: 20px;
  color: ${bankerText};
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  font-size: 0.98rem;
  white-space: pre-wrap;
  overflow-x: auto;
`;

const CodeTitle = styled.div`
  font-weight: bold;
  color: ${bankerAccent};
  margin-bottom: 12px;
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: ${bankerPanel};
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  overflow: hidden;
`;

const TableHeader = styled.th`
  background-color: ${bankerAccent};
  color: ${bankerText};
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid ${bankerAccent};
  font-weight: bold;
`;

const TableCell = styled.td`
  padding: 12px;
  border-bottom: 1px solid rgba(88, 132, 167, 0.3);
  color: ${bankerText};
  vertical-align: top;
`;

const TableVideoContainer = styled.div`
  display: flex;
  gap: 32px;
  margin-top: 40px;
  width: 100%;
  
  @media (max-width: 1200px) {
    flex-direction: column;
    gap: 20px;
  }
`;

const TableContainer = styled.div`
  flex: 1;
  min-width: 0;
`;

const VideoContainer = styled.div`
  flex: 2;
  min-width: 500px;
  
  @media (max-width: 1200px) {
    min-width: 100%;
  }
`;

const ResultMessage = styled.div`
  margin-top: 16px;
  padding: 16px;
  border-radius: 8px;
  font-weight: bold;
  text-align: center;
  border: 2px solid;
  
  ${props => {
    if (props.message.includes('Anomaly detected')) {
      return `
        background-color: rgba(220, 53, 69, 0.1);
        border-color: #dc3545;
        color: #dc3545;
      `;
    } else if (props.message.includes('No anomaly detected')) {
      return `
        background-color: rgba(40, 167, 69, 0.1);
        border-color: #28a745;
        color: #28a745;
      `;
    } else {
      return `
        background-color: rgba(255, 193, 7, 0.1);
        border-color: #ffc107;
        color: #ffc107;
      `;
    }
  }}
`;

const CreditCardPurchase = () => {
  const [formData, setFormData] = useState({
    cardNumber: '',
    firstLocation: { longitude: '', latitude: '' },
    secondLocation: { longitude: '', latitude: '' },
  });

  const [accountIds, setAccountIds] = useState([]);
  const [coordinates, setCoordinates] = useState([]);
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [nextLocation, setNextLocation] = useState('first');
  const [locationMarkers, setLocationMarkers] = useState({
    first: null,
    second: null,
  });
  const [resultMessage, setResultMessage] = useState('');
  const mapRef = useRef();

  useEffect(() => {
    const fetchAccountIds = async () => {
      try {
        const response = await fetch('https://oracleai-financial.org/accounts/accounts');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAccountIds(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error('Error fetching account IDs:', error);
      }
    };

    const fetchCoordinates = async () => {
      try {
        const response = await fetch('https://oracleai-financial.org/financial/locations/coordinates');
        const data = await response.json();
        setCoordinates(data);
      } catch (error) {
        console.error('Error fetching coordinates:', error);
      }
    };

    fetchAccountIds();
    fetchCoordinates();
  }, []);

  useEffect(() => {
    const mapContainer = document.querySelector('.leaflet-container');
    if (mapContainer) {
      const preventContextMenu = (e) => e.preventDefault();
      mapContainer.addEventListener('contextmenu', preventContextMenu);
      return () => {
        mapContainer.removeEventListener('contextmenu', preventContextMenu);
      };
    }
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'cardNumber') {
      setFormData({ ...formData, cardNumber: value });
    } else if (name.startsWith('firstLocation') || name.startsWith('secondLocation')) {
      const [location, field] = name.split('.');
      setFormData({
        ...formData,
        [location]: {
          ...formData[location],
          [field]: value,
        },
      });
      if (field === 'latitude' || field === 'longitude') {
        setLocationMarkers(prev => ({
          ...prev,
          [location === 'firstLocation' ? 'first' : 'second']:
            (location === 'firstLocation' && formData.firstLocation.longitude && formData.firstLocation.latitude) ||
            (location === 'secondLocation' && formData.secondLocation.longitude && formData.secondLocation.latitude)
              ? {
                  lat: location === 'firstLocation'
                    ? (field === 'latitude' ? value : formData.firstLocation.latitude)
                    : formData.secondLocation.latitude,
                  lng: location === 'firstLocation'
                    ? (field === 'longitude' ? value : formData.firstLocation.longitude)
                    : formData.secondLocation.longitude,
                }
              : null,
        }));
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const payload = {
      firstLocation: {
        latitude: formData.firstLocation.latitude,
        longitude: formData.firstLocation.longitude,
      },
      secondLocation: {
        latitude: formData.secondLocation.latitude,
        longitude: formData.secondLocation.longitude,
      },
    };

    try {
      const response = await fetch('https://oracleai-financial.org/financial/locations/check-distance', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const isFar = await response.json();
      if (isFar) {
        setResultMessage('🚨 Anomaly detected! Distance between the two locations is greater than 500km.');
      } else {
        setResultMessage('✅ No anomaly detected. The distance between the two locations is NOT greater than 500km.');
      }
    } catch (error) {
      setResultMessage('❌ Error checking distance: ' + error.message);
    }
  };

  const handleMapRightClick = (e) => {
    const { lat, lng } = e.latlng;
    if (nextLocation === 'first') {
      setFormData(prev => ({
        ...prev,
        firstLocation: {
          latitude: lat.toFixed(6),
          longitude: lng.toFixed(6),
        },
      }));
      setLocationMarkers(prev => ({
        ...prev,
        first: { lat: parseFloat(lat.toFixed(6)), lng: parseFloat(lng.toFixed(6)) },
      }));
      setNextLocation('second');
    } else {
      setFormData(prev => ({
        ...prev,
        secondLocation: {
          latitude: lat.toFixed(6),
          longitude: lng.toFixed(6),
        },
      }));
      setLocationMarkers(prev => ({
        ...prev,
        second: { lat: parseFloat(lat.toFixed(6)), lng: parseFloat(lng.toFixed(6)) },
      }));
      setNextLocation('first');
    }
  };

  return (
    <PageContainer>
      <h2>Process: Make purchases, detect and visualize fraud/anomalies</h2>
      <h2>
        Tech:
        <br />
        <span style={{ display: 'block', marginLeft: 24 }}>
          • Globally Distributed DB is used to manage (insert, etc.) purchases
        </span>
        <span style={{ display: 'block', marginLeft: 24 }}>
          • OML and Spatial are used to detect anomalies and visualize data
        </span>
      </h2>
      <h2>Reference: AMEX</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <CollapsibleContent>
            <TextContent>
              <div>
                <a
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier/index.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Leverage Oracle Spatial for advanced visualization and analysis</li>
                <li>Use OML4Py (Python, Jupyter, etc.) for machine learning</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Globally distributed database offers multi-region strong consistency, SQL, JSON, RAC, Data Guard, Sharding, RAFT</li> 
                <li>Use OML4Py and notebooks locally or in execution environment as part of database</li>
                <li>Spatial queries, JSON, graph, and AI with no plugins required nor scale trade-offs</li>
              </ul>
              <div style={{ marginTop: '16px' }}>
                <a
                  href="http://localhost:8888/notebooks/prebuilt-notebook.ipynb"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none', display: 'block', marginBottom: '8px' }}
                >
                  Open Jupyter Notebook
                </a>
                <a
                  href="https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/oml/index.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none', display: 'block' }}
                >
                  Open OML/OML4Py Notebook
                </a>
              </div>
            </TextContent>
            <VideoWrapper>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/qHVYXagpAC0?start=546&autoplay=0"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
              ></iframe>
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      {/* Form and Map Section */}
      <TwoColumnContainer>
        <LeftColumn>
          <Form onSubmit={handleSubmit}>
            <Label htmlFor="cardNumber">Card/Account Number</Label>
            <Select
              id="cardNumber"
              name="cardNumber"
              value={formData.cardNumber}
              onChange={handleChange}
              required
            >
              <option value="" disabled>
                Select an account
              </option>
              {accountIds.map((account) => (
                <option key={account._id} value={account._id}>
                  {account._id}
                </option>
              ))}
            </Select>

            <Label>First Location</Label>
            <Input
              type="text"
              id="firstLocation-longitude"
              name="firstLocation.longitude"
              value={formData.firstLocation.longitude}
              onChange={handleChange}
              placeholder="Longitude"
              required
            />
            <Input
              type="text"
              id="firstLocation-latitude"
              name="firstLocation.latitude"
              value={formData.firstLocation.latitude}
              onChange={handleChange}
              placeholder="Latitude"
              required
            />

            <Label>Second Location</Label>
            <Input
              type="text"
              id="secondLocation-longitude"
              name="secondLocation.longitude"
              value={formData.secondLocation.longitude}
              onChange={handleChange}
              placeholder="Longitude"
              required
            />
            <Input
              type="text"
              id="secondLocation-latitude"
              name="secondLocation.latitude"
              value={formData.secondLocation.latitude}
              onChange={handleChange}
              placeholder="Latitude"
              required
            />

            <Button type="submit">Submit Purchases At These Locations</Button>
          </Form>
          <div style={{ margin: '16px 0', fontWeight: 'bold', color: '#fff' }}>
            Right-click the map in two locations to make two purchases (form will populate) and click Submit Purchases
          </div>
          <MapWrapper>
            <MapContainer
              center={[39.7392, -104.9903]}
              zoom={5}
              scrollWheelZoom={false}
              style={{ height: '100%', width: '100%' }}
              whenCreated={mapInstance => { mapRef.current = mapInstance; }}
            >
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              />
              {locationMarkers.first && (
                <Marker position={[locationMarkers.first.lat, locationMarkers.first.lng]}>
                  <Popup>First Location</Popup>
                </Marker>
              )}
              {locationMarkers.second && (
                <Marker position={[locationMarkers.second.lat, locationMarkers.second.lng]}>
                  <Popup>Second Location</Popup>
                </Marker>
              )}
              {coordinates.map((coord, index) => (
                <Marker key={index + 1000} position={[coord.lat, coord.lng]}>
                  <Popup>{coord.description}</Popup>
                </Marker>
              ))}
              <MapRightClickHandler onRightClick={handleMapRightClick} />
            </MapContainer>
          </MapWrapper>
          {resultMessage && (
            <ResultMessage message={resultMessage}>
              {resultMessage}
            </ResultMessage>
          )}
        </LeftColumn>
        <RightColumn>
          <CodeTitle>Globally Distributed Database Connection Example</CodeTitle>
          <code>
{`// Shards can be automatically managed or programmatically with simple configuration

if (isAutomaticSharding) {
    dataSource.setConnectionProperty("oracle.jdbc.useShardingDriverConnection", "true");
    dataSource.setConnectionProperty("oracle.jdbc.allowSingleShardTransactionSupport", "true");
    connection = (OracleConnection) dataSource.getConnection();
} else {
    shardKey = dataSource.createShardingKeyBuilder()
            .subkey(accountId, OracleType.NUMBER)
            .build();
    connection = (OracleConnection) dataSource.createConnectionBuilder()
            .shardingKey(shardKey)
            .build();
}
`}
          </code>
        </RightColumn>
      </TwoColumnContainer>

      {/* Full Width Video Section */}
      <div style={{ 
        marginTop: '40px', 
        textAlign: 'center',
        padding: '20px',
        background: bankerPanel,
        borderRadius: '8px',
        border: `1px solid ${bankerAccent}`
      }}>
        <h4 style={{ color: bankerAccent, marginBottom: '16px' }}>Demo Video:</h4>
        <iframe
          width="100%"
          height="1000"
          src="https://www.youtube.com/embed/qHVYXagpAC0?start=546&autoplay=0"
          title="Raft vs Data Guard Demo"
          frameBorder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
          style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
        ></iframe>
      </div>
    </PageContainer>
  );
};

function MapRightClickHandler({ onRightClick }) {
  useMapEvent('contextmenu', (e) => {
    onRightClick(e);
  });
  return null;
}

export default CreditCardPurchase;
