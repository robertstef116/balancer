import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TreeList from '../generic-components/TreeList';
import { getResources } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

function ResourcesList({ classname, onSelectionChanged }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const [resourcesData, setResourcesData] = useState([]);
  const workers = useSelector((state) => state.workers);
  const workflows = useSelector((state) => state.workflows);
  const deployments = useSelector((state) => state.deployments);

  const convertToArray = (data) => {
    const converted = [];
    for (const [key, value] of Object.entries(data)) {
      converted.push({
        ...value,
        id: key,
        ...(value.children && { children: convertToArray(value.children) }),
      });
    }
    return converted;
  };

  const onRefresh = () => {
    actionWrapper({ action: getResources, reload: true });
  };

  useEffect(() => {
    actionWrapper({ action: getResources });
  }, []);

  useEffect(() => {
    if (workers !== null && workflows !== null && deployments !== null) {
      const workerDataMap = { };
      for (const worker of workers) {
        workerDataMap[worker.id] = { name: worker.alias, children: {} };
      }

      const workflowsDataMap = {};
      for (const workflow of workflows) {
        workflowsDataMap[workflow.id] = { name: workflow.image, children: {} };
      }

      for (const deployment of deployments) {
        const worker = workerDataMap[deployment.workerId];
        if (!worker) {
          continue; // worker not exists
        }

        let workflow = worker.children[deployment.workflowId];
        if (!workflow) {
          const workflowData = workflowsDataMap[deployment.workflowId];
          if (!workflowData) {
            continue; // workflow not exists
          }

          workflow = { ...workflowData };
          worker.children[deployment.workflowId] = workflow;
        }
        workflow.children[deployment.id] = { name: deployment.containerId.substring(0, 10) };
      }
      setResourcesData(convertToArray(workerDataMap));
    }
  }, [workers, workflows, deployments]);

  return (
    <TreeList classname={classname} data={resourcesData} onSelectionChanged={onSelectionChanged} onRefresh={onRefresh} {...widgetProps} />
  );
}

export default ResourcesList;
