import React from 'react';
import UsageChart from '../components/UsageChart';

function HomePage() {
  return (
    <div className="row mx-0 mx-md-1 mx-xl-3 mx-xxl-5">
      <UsageChart classname="col-6 wh-1" />
    </div>
  );
}

export default HomePage;
