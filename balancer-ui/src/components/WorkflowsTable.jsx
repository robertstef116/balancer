import React, { useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TableWidget from '../generic-components/TableWidget';
import { addWorkflow, deleteWorkflow, getWorkflows, updateWorkflow } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import { algorithms, Icons, ModalFormModes } from '../constants';
import { SimpleModal, SimpleModalTypes } from './SimpleModal';
import FormBuilder from '../generic-components/FormBuilder';
import ModalWrapper from '../generic-components/ModalWrapper';
import { convertFormMapToMap } from '../utils';

const defaultFormModalConfig = { title: '', fields: [], submit: null };

const imageValidator = (data) => data.image && /[a-z][a-z0-9]+:[a-z0-9]+/.test(data.image);
const memoryValidator = (data) => {
  if (data.memoryLimit) {
    const memoryLimit = parseInt(data.memoryLimit, 10);
    return (typeof data.memoryLimit !== 'string' || `${memoryLimit}` === data.memoryLimit) && memoryLimit >= 10000;
  }
  return true;
};
const algorithmValidator = (data) => !!data.algorithm;
const minDeploymentsValidator = (data) => {
  if (data.minDeployments) {
    const minDeployments = parseInt(data.minDeployments, 10);
    return (typeof data.minDeployments !== 'string' || `${minDeployments}` === data.minDeployments) && minDeployments >= 1;
  }
  return true;
};
const maxDeploymentsValidator = (data) => {
  if (data.maxDeployments) {
    const minDeployments = parseInt(data.minDeployments, 10) || 1;
    const maxDeployments = parseInt(data.maxDeployments, 10);
    return (typeof data.maxDeployments !== 'string' || `${maxDeployments}` === data.maxDeployments) && maxDeployments >= minDeployments;
  }
  return true;
};
const pathMappingValidator = (data) => {
  if (!data.pathMapping || !data.pathMapping.length) {
    return false;
  }
  for (const path of data.pathMapping) {
    if (!/\/[a-zA-Z0-9_-]+/.test(path.key)) {
      return false;
    }
    const port = parseInt(path.value, 10);
    if (`${port}` !== `${path.value}` || port <= 0 || port >= 65535) {
      return false;
    }
  }
  return true;
};

const imageConfig = { key: 'image', label: 'Image', info: 'Docker image name ran by the workflow (image:tag)', validator: imageValidator };
const memoryConfig = { key: 'memoryLimit', label: 'Memory limit', type: 'number', info: 'Maximum memory in b allowed to be used', validator: memoryValidator };
const algorithmConfig = { key: 'algorithm', label: 'Algorithm', type: 'dropdown', options: algorithms, info: 'Algorithm used for load balancing', validator: algorithmValidator };
const minDeploymentsConfig = { key: 'minDeployments', label: 'Minimum scaling', type: 'number', info: 'Minimum number of deployments', validator: minDeploymentsValidator };
const maxDeploymentsConfig = { key: 'maxDeployments', label: 'Maximum scaling', type: 'number', info: 'Maximum number of deployments', validator: maxDeploymentsValidator };
const pathMappingConfig = { key: 'pathMapping', label: 'Mapping', type: 'map', mapValueType: 'number', info: 'Resource mapping (path -> port)', validator: pathMappingValidator };

function WorkflowsTable({ className }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const workflows = useSelector((state) => state.workflows);
  const [modalFormMode, setModalFormMode] = useState(null);
  const [valid, setValid] = useState(false);
  const [modalData, setModalData] = useState({});
  const [selectedWorkflowId, setSelectedWorkflowId] = useState(null);
  const [modalNotificationConfigs, setModalNotificationConfigs] = useState({});
  const [modalFormConfig, setModalFormConfig] = useState(defaultFormModalConfig);

  const getSelectedWorkflow = () => workflows.find((w) => w.id === selectedWorkflowId);

  const dismissModalNotification = () => {
    setModalNotificationConfigs({});
  };

  const ensureWorkflowSelected = (title) => {
    if (!selectedWorkflowId) {
      setModalNotificationConfigs({ title, description: 'Please select a workflow first!' });
      return false;
    }
    return true;
  };

  const onRefresh = () => {
    actionWrapper({ action: getWorkflows, reload: true });
  };

  const onAddClick = () => {
    setValid(false);
    setModalFormMode(ModalFormModes.ADD);
  };

  const onUpdateClick = () => {
    if (ensureWorkflowSelected('Update workflow')) {
      const workflow = getSelectedWorkflow();
      setModalData(workflow);
      setValid(false);
      setModalFormMode(ModalFormModes.UPDATE);
    }
  };

  const onSave = useCallback(() => {
    const convertedModalData = { ...modalData };
    convertedModalData.pathMapping = convertFormMapToMap(convertedModalData.pathMapping);
    actionWrapper({
      action: addWorkflow,
      params: convertedModalData,
      cb: () => {
        setModalFormMode(null);
      },
    });
  }, [modalData]);

  const onUpdate = useCallback(() => {
    actionWrapper({
      action: updateWorkflow,
      params: modalData,
      cb: () => {
        setModalFormMode(null);
      },
    });
  }, [modalData]);

  const onDelete = () => {
    const title = 'Delete workflow';
    if (ensureWorkflowSelected(title)) {
      const workflow = getSelectedWorkflow();
      setModalNotificationConfigs({
        title,
        description: `Are you sure you want to remove the workflow "${workflow.image || ''}", this action is permanent?`,
        type: SimpleModalTypes.CONFIRMATION,
        dismiss: (res) => {
          if (res) {
            actionWrapper({ action: deleteWorkflow, params: { id: selectedWorkflowId } });
            setSelectedWorkflowId(null);
          }
          dismissModalNotification();
        },
      });
    }
  };

  useEffect(() => {
    actionWrapper({ action: getWorkflows });
  }, []);

  useEffect(() => {
    switch (modalFormMode) {
      case ModalFormModes.ADD:
        return setModalFormConfig({
          title: 'Add workflow',
          fields: [
            imageConfig,
            memoryConfig,
            algorithmConfig,
            minDeploymentsConfig,
            maxDeploymentsConfig,
            pathMappingConfig,
          ],
          submit: onSave,
        });
      case ModalFormModes.UPDATE:
        return setModalFormConfig({
          title: 'Update workflow',
          fields: [
            algorithmConfig,
            minDeploymentsConfig,
            maxDeploymentsConfig,
          ],
          submit: onUpdate,
        });
      default:
        return setModalFormConfig(defaultFormModalConfig);
    }
  }, [modalFormMode, onSave, onUpdate]);

  return (
    <>
      <TableWidget
        className={className}
        title="Workflows List"
        onRefresh={onRefresh}
        {...widgetProps}
        cols={[
          { header: 'Image', key: 'image', maxWidth: '200px' },
          { header: 'Memory Limit(b)', key: 'memoryLimit' },
          { header: 'Min', key: 'minDeployments' },
          { header: 'Max', key: 'maxDeployments' },
          { header: 'Algorithm', key: 'algorithmDisplay' },
          { header: 'Mapping', key: 'mapping', type: 'InfoIcon' }]}
        rows={workflows}
        actions={[
          { title: 'Add workflow', icon: Icons.ADD, onClick: onAddClick },
          { title: 'Update workflow', icon: Icons.EDIT, onClick: onUpdateClick },
          { title: 'Delete workflow', icon: Icons.DELETE, onClick: onDelete },
        ]}
        activeRowKey={selectedWorkflowId}
        onRowClick={setSelectedWorkflowId}
      />
      <ModalWrapper
        title={modalFormConfig.title}
        show={!!modalFormMode}
        onHide={() => setModalFormMode(null)}
        valid={valid}
        onSubmit={modalFormConfig.submit}
      >
        <FormBuilder data={modalData} setData={setModalData} configs={modalFormConfig.fields} changeValidity={setValid} />
      </ModalWrapper>
      <SimpleModal dismiss={dismissModalNotification} {...modalNotificationConfigs} />
    </>
  );
}

export default WorkflowsTable;
