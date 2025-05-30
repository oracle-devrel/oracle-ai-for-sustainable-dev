import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
import { MapContainer, TileLayer, Marker, Popup, useMapEvent } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icons in Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const PageContainer = styled.div`
  background-color: #121212;
  color: #ffffff;
  width: 100%;
  height: 100vh;
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
  border: 1px solid #444;
  border-radius: 8px;
  background-color: #1e1e1e;
  margin-bottom: 20px;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #ffffff;
`;

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2c2c2c;
  color: #ffffff;
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2c2c2c;
  color: #ffffff;
`;

const Button = styled.button`
  padding: 10px;
  background-color: #1abc9c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background-color: #16a085;
  }
`;

const SidePanel = styled.div`
  border: 1px solid #444;
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e;
  color: #ffffff;
  margin-bottom: 20px;
`;

const ToggleButton = styled.button`
  background-color: #1abc9c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;

  &:hover {
    background-color: #16a085;
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
  height: 600px; /* Set a fixed height for the iframe */
  margin-top: 20px; /* Add spacing above the iframe */
  border: 1px solid #444; /* Optional border for better visibility */
  border-radius: 8px;
  overflow: hidden;
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
  const [nextLocation, setNextLocation] = useState('first'); // alternates between 'first' and 'second'
  const [locationMarkers, setLocationMarkers] = useState({
    first: null,
    second: null,
  });
  const mapRef = useRef();

  useEffect(() => {
    // Fetch account IDs for the dropdown
    const fetchAccountIds = async () => {
      try {
        const response = await fetch('https://oracleai-financial.org/accounts/accounts');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAccountIds(Array.isArray(data) ? data : []); // If the endpoint returns an array
      } catch (error) {
        console.error('Error fetching account IDs:', error);
      }
    };

    // Fetch coordinates for the map
    const fetchCoordinates = async () => {
      try {
        // const response = await fetch('http://globallydistributeddatabase.financial:8080/financial/locations/coordinates');    https://oracleai-financial.org/transfer
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
      // Also update marker if lat/lng fields are changed manually
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

    // Prepare payload for backend
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
        alert('Anomaly detected! Distance between the two locations is greater than 500km.');
      } else {
        alert('No anomaly detected.  The distance between the two locations is NOT greater than 500km.');
      }
    } catch (error) {
      alert('Error checking distance: ' + error.message);
    }
  };

  // Alternate right-clicks between firstLocation and secondLocation, and update markers
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
      <h2>Process: Make purchases and visualize fraud</h2>
      <h2>Tech: Globally Distributed DB, OML, Spatial</h2>
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
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
              <h4>Financial Process:</h4>
              <ul>
                <li>Manage credit card transactions with Globally Distributed Database</li>
                <li>Detect suspicious credit card transactions using ML/AI and spatial</li>
              </ul>
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
              {/* Notebook Section moved here */}
              <NotebookWrapper>
                <iframe
                  src="http://localhost:8888/notebooks/prebuilt-notebook.ipynb"
                  title="Jupyter Notebook"
                  width="100%"
                  height="100%"
                  style={{ border: 'none' }}
                ></iframe>
              </NotebookWrapper>
            </TextContent>
            <VideoWrapper>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/E1pOaCkd_PM"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              ></iframe>
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      {/* Form Section */}
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

      {/* Instruction above the map */}
      <div style={{ margin: '16px 0', fontWeight: 'bold', color: '#1abc9c' }}>
        Right-click the map in two locations to make two purchases (form will populate) and click Submit Purchases
      </div>

      {/* Map Section */}
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
          {/* Show markers for first and second locations if set */}
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
          {/* Show any other coordinates from backend */}
          {coordinates.map((coord, index) => (
            <Marker key={index + 1000} position={[coord.lat, coord.lng]}>
              <Popup>{coord.description}</Popup>
            </Marker>
          ))}
          <MapRightClickHandler onRightClick={handleMapRightClick} />
        </MapContainer>
      </MapWrapper>

      {/* Images */}
      {/* <Image src="/images/spatial-suspicious.png" alt="Spatial Suspicious Transactions" /> */}
      {/* <Image src="/images/spatial-agg-03.png" alt="Spatial Aggregated Data" /> */}
    </PageContainer>
  );
};

// Custom right-click handler for the map
function MapRightClickHandler({ onRightClick }) {
  useMapEvent('contextmenu', (e) => {
    onRightClick(e);
  });
  return null;
}

export default CreditCardPurchase;
