import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
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

const Image = styled.img`
  display: block;
  max-width: 90%;
  height: auto;
  margin: 20px auto;
  border: 1px solid #444;
  border-radius: 8px;
`;

const CreditCardPurchase = () => {
  const [formData, setFormData] = useState({
    cardNumber: '',
    amount: '',
    description: '',
    longitude: '',
    latitude: '',
  });

  const [accountIds, setAccountIds] = useState([]);
  const [isCollapsed, setIsCollapsed] = useState(false);

  useEffect(() => {
    // Fetch account IDs for the dropdown
    const fetchAccountIds = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/ACCOUNT_DETAIL/'
        );
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAccountIds(data.items || []); // Assuming the data is in the `items` array
      } catch (error) {
        console.error('Error fetching account IDs:', error);
      }
    };

    fetchAccountIds();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert(`Transaction submitted successfully! Data: ${JSON.stringify(formData)}`);
  };

  // Generate 100 random coordinates clustered around Denver, Houston, New Orleans, and Washington D.C.
  const generateCoordinates = () => {
    const clusters = [
      { lat: 39.7392, lng: -104.9903, name: 'Denver' },
      { lat: 29.7604, lng: -95.3698, name: 'Houston' },
      { lat: 29.9511, lng: -90.0715, name: 'New Orleans' },
      { lat: 38.9072, lng: -77.0369, name: 'Washington D.C.' },
    ];

    const coordinates = [];
    for (let i = 0; i < 100; i++) {
      const cluster = clusters[Math.floor(Math.random() * clusters.length)];
      const lat = cluster.lat + (Math.random() - 0.5) * 0.5; // Randomize within ~0.5 degrees
      const lng = cluster.lng + (Math.random() - 0.5) * 0.5;
      coordinates.push({
        lat,
        lng,
        description: `trans_id: ${Math.floor(Math.random() * 1000)}, location_id: ${Math.floor(
          Math.random() * 5000
        )}, trans_epoch_date: ${Math.floor(Date.now() / 1000)}`,
      });
    }
    return coordinates;
  };

  const coordinates = generateCoordinates();

  return (
    <PageContainer>
      <h2>Process: Make purchases and visualize fraud</h2>
      <h2>Tech: Globally Distributed DB, OML, Spatial</h2>
      <h2>Reference: AMEX</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
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
            <option key={account.account_id} value={account.account_id}>
              {account.account_id}
            </option>
          ))}
        </Select>

        <Label htmlFor="amount">Amount</Label>
        <Input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          placeholder="Enter amount"
          required
        />

        <Label htmlFor="description">Description</Label>
        <Input
          type="text"
          id="description"
          name="description"
          value={formData.description}
          onChange={handleChange}
          placeholder="Enter transaction description"
        />

        <Label htmlFor="longitude">Longitude</Label>
        <Input
          type="text"
          id="longitude"
          name="longitude"
          value={formData.longitude}
          onChange={handleChange}
          placeholder="Enter longitude"
          required
        />

        <Label htmlFor="latitude">Latitude</Label>
        <Input
          type="text"
          id="latitude"
          name="latitude"
          value={formData.latitude}
          onChange={handleChange}
          placeholder="Enter latitude"
          required
        />

        <Button type="submit">Submit</Button>
      </Form>

      {/* Map Section */}
      <MapWrapper>
        <MapContainer center={[39.7392, -104.9903]} zoom={5} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />
          {coordinates.map((coord, index) => (
            <Marker key={index} position={[coord.lat, coord.lng]}>
              <Popup>{coord.description}</Popup>
            </Marker>
          ))}
        </MapContainer>
      </MapWrapper>

      {/* Notebook Section */}
      <NotebookWrapper>
        <iframe
          src="http://localhost:8888/notebooks/prebuilt-notebook.ipynb"
          title="Jupyter Notebook"
          width="100%"
          height="100%"
          style={{ border: 'none' }}
        ></iframe>
      </NotebookWrapper>

      {/* Images */}
      {/* <Image src="/images/spatial-suspicious.png" alt="Spatial Suspicious Transactions" /> */}
      {/* <Image src="/images/spatial-agg-03.png" alt="Spatial Aggregated Data" /> */}
    </PageContainer>
  );
};

export default CreditCardPurchase;
