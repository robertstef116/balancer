import React, { useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TableWidget from '../generic-components/TableWidget';
import { deleteWorker, getWorkers, updateWorker } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import ModalWrapper from '../generic-components/ModalWrapper';
import FormBuilder from '../generic-components/FormBuilder';
import { SimpleModal, SimpleModalTypes } from './SimpleModal';
import { Icons, ModalFormModes, WorkerNodeStatus } from '../constants';

const aliasValidator = (data) => data.alias && data.alias.length >= 1 && data.alias.length <= 50;

const aliasConfig = {
  key: 'alias',
  label: 'Alias',
  info: 'Alias can have a maximum length of 50 characters',
  validator: aliasValidator,
};

const defaultFormModalConfig = { title: '', fields: [], submit: null };

function WorkersTable({ className }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const workers = useSelector((state) => state.workers);
  const [modalFormMode, setModalFormMode] = useState(null);
  const [modalData, setModalData] = useState({});
  const [valid, setValid] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState(null);
  const [modalNotificationConfigs, setModalNotificationConfigs] = useState({});
  const [modalFormConfig, setModalFormConfig] = useState(defaultFormModalConfig);

  const getSelectedWorker = () => workers.find((w) => w.id === selectedWorkerId);

  const ensureWorkerSelected = (title) => {
    if (!selectedWorkerId) {
      setModalNotificationConfigs({ title, description: 'Please select a worker first!' });
      return false;
    }
    return true;
  };

  const onRefresh = () => {
    actionWrapper({ action: getWorkers, reload: true });
  };

  const dismissModalNotification = () => {
    setModalNotificationConfigs({});
  };

  const onUpdateClick = () => {
    if (ensureWorkerSelected('Update worker')) {
      const worker = getSelectedWorker();
      setModalData(worker);
      setValid(false);
      setModalFormMode(ModalFormModes.UPDATE);
    }
  };

  const onUpdate = useCallback(() => {
    actionWrapper({
      action: updateWorker,
      params: modalData,
      cb: () => {
        setModalFormMode(null);
      },
    });
  }, [modalData]);

  const onUpdateStateClick = () => {
    if (ensureWorkerSelected('Update worker state')) {
      const worker = getSelectedWorker();
      const updatedState = worker.state === WorkerNodeStatus.ONLINE || worker.state === WorkerNodeStatus.OFFLINE ? WorkerNodeStatus.DISABLED : WorkerNodeStatus.OFFLINE;
      const actionName = worker.state === WorkerNodeStatus.ONLINE || worker.state === WorkerNodeStatus.OFFLINE ? 'Disable' : 'Enable';
      const title = `${actionName} worker`;
      setModalNotificationConfigs({
        title,
        description: `Are you sure you want to ${actionName.toLowerCase()} the worker "${worker.alias || ''} - ${worker.id}", this action might be destructive?`,
        type: SimpleModalTypes.CONFIRMATION,
        dismiss: (res) => {
          if (res) {
            actionWrapper({ action: updateWorker, params: { id: selectedWorkerId, state: updatedState } });
          }
          dismissModalNotification();
        },
      });
    }
  };

  const onDeleteClick = () => {
    const title = 'Delete worker';
    if (ensureWorkerSelected('Delete worker')) {
      const worker = getSelectedWorker();
      setModalNotificationConfigs({
        title,
        description: `Are you sure you want to remove the worker "${worker.alias || ''} - ${worker.id}", this action is permanent?`,
        type: SimpleModalTypes.CONFIRMATION,
        dismiss: (res) => {
          if (res) {
            actionWrapper({ action: deleteWorker, params: { id: selectedWorkerId } });
            setSelectedWorkerId(null);
          }
          dismissModalNotification();
        },
      });
    }
  };

  const onDescribeClick = () => {
    if (ensureWorkerSelected('Describe worker')) {
      const worker = getSelectedWorker();
      setModalNotificationConfigs({
        title: `Worker ${worker.alias} description`,
        description: `The identification id of the worker is ${worker.id}`,
        type: SimpleModalTypes.INFO,
      });
    }
  };

  useEffect(() => {
    actionWrapper({ action: getWorkers });
  }, []);

  useEffect(() => {
    switch (modalFormMode) {
      case ModalFormModes.UPDATE:
        return setModalFormConfig({
          title: 'Update worker',
          fields: [
            aliasConfig,
          ],
          submit: onUpdate,
        });
      default:
        return setModalFormConfig(defaultFormModalConfig);
    }
  }, [modalFormMode, onUpdate]);

  return (
    <>
      <TableWidget
        className={className}
        title="Workers List"
        widgetProps={!modalFormMode && widgetProps}
        onRefresh={onRefresh}
        cols={[
          { key: 'stateIcon', type: 'Icon', width: '20px', titleKey: 'stateTitle' },
          { header: 'Alias', key: 'alias' },
          { header: 'State', key: 'state' }]}
        rows={workers}
        actions={[
          { title: 'Update worker', icon: Icons.EDIT, onClick: onUpdateClick },
          { title: 'Disable worker', icon: Icons.FREEZE, onClick: onUpdateStateClick },
          { title: 'Delete worker', icon: Icons.DELETE, onClick: onDeleteClick },
          { title: 'Describe worker', icon: Icons.INFO, onClick: onDescribeClick },
        ]}
        activeRowKey={selectedWorkerId}
        onRowClick={setSelectedWorkerId}
      />
      <ModalWrapper
        title={modalFormConfig.title}
        widgetProps={widgetProps}
        show={!!modalFormMode}
        onHide={() => setModalFormMode(null)}
        valid={valid}
        onSubmit={modalFormConfig.submit}
      >
        <FormBuilder
          data={modalData}
          setData={setModalData}
          configs={modalFormConfig.fields}
          changeValidity={setValid}
        />
      </ModalWrapper>
      <SimpleModal dismiss={dismissModalNotification} {...modalNotificationConfigs} />
    </>
  );
}

export default WorkersTable;
