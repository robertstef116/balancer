import React, { useState } from 'react';
import UsageChart from '../components/UsageChart';
import ResourcesList from '../components/ResourcesList';
import PageWrapper from '../generic-components/PageWrapper';

function HomePage() {
  const [workerId, setWorkerId] = useState(null);
  const [workflowId, setWorkflowId] = useState(null);
  const [deploymentId, setDeploymentId] = useState(null);

  const selectedResourcePathChanged = (path) => {
    if (path[0]) {
      setWorkerId(path[0].id);
    } else {
      setWorkerId(null);
    }

    if (path[1]) {
      setWorkflowId(path[1].id);
    } else {
      setWorkflowId(null);
    }

    if (path[2]) {
      setDeploymentId(path[2].id);
    } else {
      setDeploymentId(null);
    }
  };

  return (
    <PageWrapper>
      <ResourcesList classname="col-4 wh-2" onSelectionChanged={selectedResourcePathChanged} />
      <UsageChart classname="col-8 wh-1" workerId={workerId} workflowId={workflowId} deploymentId={deploymentId} />
    </PageWrapper>
  );
}

export default HomePage;
