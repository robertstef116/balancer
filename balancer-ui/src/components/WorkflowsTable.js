import React, {useEffect, useRef} from 'react';
import TableWidget from "../generic-components/TableWidget";
import {useSelector} from "react-redux";
import {getWorkflows} from "../redux/actions";
import useWidgetUtils from "../utils/useWidgetUtils";

const WorkflowsTable = ({className}) => {
  const {widgetProps, actionWrapper} = useWidgetUtils();
  const workflows = useSelector(state => state.workflows);
  const workflowsRef = useRef(workflows);

  useEffect(() => {
    if (!workflowsRef.current) {
      actionWrapper({action: getWorkflows})
    }
  }, [actionWrapper]);

  return (
    <TableWidget className={className} {...widgetProps}
                 cols={[
                   {header: 'Image', key: 'image', maxWidth: '200px'},
                   {header: 'Memory Limit(b)', key: 'memoryLimit'},
                   {header: 'Min', key: 'minDeployments'},
                   {header: 'Max', key: 'maxDeployments'},
                   {header: 'Algorithm', key: 'algorithm'},
                   {header: 'Mapping',  key: 'mapping', type: 'InfoIcon'}]}
                 rows={workflows}/>
  );
};

export default WorkflowsTable;
