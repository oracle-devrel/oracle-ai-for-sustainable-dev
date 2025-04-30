import React from 'react';
import styled from 'styled-components';

const IframeContainer = styled.div`
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

const Iframe = styled.iframe`
  width: 100%;
  height: calc(100vh - 20px); // Adjust height to fit the layout
  border: none;
`;

const Investments = () => {
  return (
    <div>
   
      <IframeContainer>
        <Iframe src="http://141.148.204.74:8080" title="Investments" />
      </IframeContainer>
    </div>
  );
};

export default Investments;
