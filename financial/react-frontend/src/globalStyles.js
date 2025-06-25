import { createGlobalStyle } from 'styled-components';

const GlobalStyle = createGlobalStyle`
  body {
    background-color: #354F64; /* Banker blue background */
    color: #F9F9F9; /* Light text */
    margin: 0;
    font-family: Helvetica, Arial, sans-serif;
  }

  a {
    color: #5884A7; /* Accent color for links */
  }
`;

export default GlobalStyle;