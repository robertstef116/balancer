import React from 'react';
import WorkersTable from "../components/WorkersTable";
import WorkflowsTable from "../components/WorkflowsTable";

const NodesPage = () => {
  return (
    <>
      <div className='row mx-0 mx-md-1 mx-xl-3 mx-xxl-5'>
        <WorkersTable className='col-4 wh-1'/>
        <WorkflowsTable className='col-8 wh-1'/>
      </div>
    </>
  );
};

export default NodesPage;
