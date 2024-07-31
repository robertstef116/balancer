import React, { useState } from 'react';
import ScalingDataChart from '../components/ScalingDataChart';
import ResourcesList from '../components/ResourcesList';
import PageWrapper from '../generic-components/PageWrapper';
import BalancingHistoryChart from '../components/BalancingHistoryChart';

function HomePage() {
  const [workflowId, setWorkflowId] = useState(null);
  // eslint-disable-next-line no-unused-vars
  const [path, setPath] = useState(null);

  const selectedResourcePathChanged = (selection) => {
    if (selection[0]) {
      setWorkflowId(selection[0].id);
    } else {
      setWorkflowId(null);
    }
    if (selection[1]) {
      setPath(selection[1].path);
    } else {
      setPath(null);
    }
  };

  return (
    <PageWrapper>
      <div className="row">
        <ResourcesList classname="widget-4 wh-2" onSelectionChanged={selectedResourcePathChanged} />
        <div className="widgets-container">
          <BalancingHistoryChart classNames="wh-1" workflowId={workflowId} path={path} />
          <ScalingDataChart classname="wh-1" title="Scaling cpu data" metric="avg_cpu" workflowId={workflowId} />
        </div>
      </div>
      <div className="row flex-xl-row-reverse">
        <ScalingDataChart
          classname="widget-8 wh-1"
          title="Scaling memory data"
          metric="avg_memory"
          workflowId={workflowId}
        />
        <ScalingDataChart
          classname="widget-4 wh-1"
          title="Scaling replicas data"
          metric="replicas"
          workflowId={workflowId}
        />
      </div>
    </PageWrapper>
  );
}

export default HomePage;
