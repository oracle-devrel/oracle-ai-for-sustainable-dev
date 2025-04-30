import React from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px;
`;

const IframesContainer = styled.div`
  display: flex;
  flex-direction: column; /* Stack elements vertically */
  width: 100%;
  height: 100%; /* Use full height */
  gap: 20px; /* Space between the elements */
`;

const Image = styled.img`
  width: 100%; /* Make the image take the full width */
  max-height: 50%; /* Limit the height to half the container */
  object-fit: contain; /* Maintain aspect ratio */
  border: none;
`;

const TwitchEmbed = styled.div`
  flex: 1; /* Allow the Twitch iframe to take the remaining space */
  display: flex;
  align-items: center;
  justify-content: center;
`;

const SpeakWithData = () => {
  return (
    <PageContainer>
      <h1>Speak with your financial data</h1>
      <IframesContainer>
        {/* Image */}
        <Image src="/images/aiholopage.png" alt="SpeakWithData" />

        {/* Twitch Embed */}
        <TwitchEmbed>
          <iframe
            src="https://player.twitch.tv/?channel=aiholo&parent=localhost"
            style={{ height: '100%', width: '100%' }} /* Ensure iframe fills its container */
            frameBorder="0"
            allowFullScreen={true}
            title="Twitch Stream"
          ></iframe>
        </TwitchEmbed>
      </IframesContainer>
    </PageContainer>
  );
};

export default SpeakWithData;
