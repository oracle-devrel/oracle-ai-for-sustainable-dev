const express = require('express');
const https = require('https');
const fs = require('fs');
const path = require('path');
const app = express();

const PORT = 4884; // Port number for your HTTPS server
const options = {
  key: fs.readFileSync('C:\\aiholo-app\\localhost-key.pem'),  // Path to your private key
  cert: fs.readFileSync('C:\\aiholo-app\\localhost.pem')       // Path to your certificate
};

// Serve static files from the React build directory
app.use(express.static(path.join(__dirname, 'build')));

// All routes should redirect to the index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build', 'index.html'));
});

https.createServer(options, app).listen(PORT, () => {
  console.log(`Server running on https://130.61.51.75:${PORT}`);
});
