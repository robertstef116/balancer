import React, { useState } from 'react';
import ScalingDataChart from '../components/ScalingDataChart';
import ResourcesList from '../components/ResourcesList';
import PageWrapper from '../generic-components/PageWrapper';
import BalancingHistoryChart from '../components/BalancingHistoryChart';

function HomePage() {
  const [workflowId, setWorkflowId] = useState(null);
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
      <div className="row m-0">
        <ResourcesList classname="widget-4 wh-2" onSelectionChanged={selectedResourcePathChanged} />
        <div className="widgets-container">
          <BalancingHistoryChart
            classNames="wh-1"
            workflowId={workflowId}
            path={path}
            title="Average response time ms"
            metric="avg_response_time"
          />
          <BalancingHistoryChart
            classNames="wh-1"
            workflowId={workflowId}
            path={path}
            title="Balanced requests count"
            metric="requests_count"
          />
        </div>
      </div>
      <div className="row m-0">
        <ScalingDataChart
          classname="widget-4 wh-1"
          title="Replicas"
          metric="replicas"
          workflowId={workflowId}
        />
        <ScalingDataChart
          classname="widget-4 wh-1"
          title="Average CPU usage"
          metric="avg_cpu"
          workflowId={workflowId}
        />
        <ScalingDataChart
          classname="widget-4 wh-1"
          title="Average memory usage"
          metric="avg_memory"
          workflowId={workflowId}
        />
      </div>
    </PageWrapper>
  );
}

export default HomePage;
