import { createGlobalStyle } from 'styled-components';

const GlobalStyle = createGlobalStyle`
  body {
    background-color: #121212; /* Dark background */
    color: #ffffff; /* Light text */
    margin: 0;
    font-family: Arial, sans-serif;
  }

  a {
    color: #bb86fc; /* Accent color for links */
  }
`;

export default GlobalStyle;