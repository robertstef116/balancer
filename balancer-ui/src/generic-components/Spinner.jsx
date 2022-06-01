import React from 'react';
import { HashLoader } from 'react-spinners';

function Spinner({ visible }) {
  return visible
    ? (
      <div className="spinner-overlay">
        <HashLoader color="#f97f05" />
      </div>
    )
    : null;
}

export default Spinner;
