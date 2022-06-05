import React from 'react';
import WorkersTable from '../components/WorkersTable';
import WorkflowsTable from '../components/WorkflowsTable';
import PageWrapper from '../generic-components/PageWrapper';

function NodesPage() {
  return (
    <PageWrapper>
      <WorkersTable className="col-4 wh-1" />
      <WorkflowsTable className="col-8 wh-1" />
    </PageWrapper>
  );
}

export default NodesPage;
