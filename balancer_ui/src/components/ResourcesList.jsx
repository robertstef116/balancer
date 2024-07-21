import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TreeList from '../generic-components/TreeList';
import { getResources } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

function ResourcesList({ classname, onSelectionChanged }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const [resourcesData, setResourcesData] = useState([]);
  const workflows = useSelector((state) => state.workflows);

  const onRefresh = () => {
    actionWrapper({ action: getResources, reload: true });
  };

  useEffect(() => {
    actionWrapper({ action: getResources });
  }, []);

  useEffect(() => {
    if (workflows !== null) {
      setResourcesData(workflows.map((w) => {
        const children = [];
        for (const [path, port] of Object.entries(w.pathsMapping)) {
          children.push({ id: path, name: `${path} : ${port}` });
        }
        return { id: w.id, name: w.image, children };
      }));
    }
  }, [workflows]);

  return (
    <TreeList classname={classname} data={resourcesData} onSelectionChanged={onSelectionChanged} onRefresh={onRefresh} widgetProps={widgetProps} />
  );
}

export default ResourcesList;
