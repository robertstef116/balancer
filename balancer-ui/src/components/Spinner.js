import React from 'react';
import {HashLoader} from "react-spinners";

const Spinner = ({visible}) => {

  return visible ?
    <div className='spinner-overlay'>
      <HashLoader color='#f97f05'/>
    </div> :
    <></>
}

export default Spinner;
