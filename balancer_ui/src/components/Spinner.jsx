import React from 'react';
import { BeatLoader, HashLoader } from 'react-spinners';
import classNames from 'classnames';

function Spinner({ visible, small, transparent }) {
  return visible
    ? (
      <div className={classNames('spinner-overlay', {
        'bg-transparent': transparent,
      })}
      >
        {small
          ? <BeatLoader color="#f97f05" />
          : <HashLoader color="#f97f05" /> }
      </div>
    )
    : null;
}

export default Spinner;
