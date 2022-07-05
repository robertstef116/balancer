import React from 'react';
import WorkersTable from '../components/WorkersTable';
import WorkflowsTable from '../components/WorkflowsTable';
import PageWrapper from '../generic-components/PageWrapper';

function NodesPage() {
  return (
    <PageWrapper>
      <div className="row justify-content-center">
        <WorkersTable className="widget-4 wh-1" />
        <WorkflowsTable className="widget-8 wh-1" />
      </div>
    </PageWrapper>
  );
}

export default NodesPage;
