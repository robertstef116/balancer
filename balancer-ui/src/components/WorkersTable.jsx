import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TableWidget from '../generic-components/TableWidget';
import { addWorker, deleteWorker, getWorkers, updateWorker } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import ModalWrapper from '../generic-components/ModalWrapper';
import FormBuilder from '../generic-components/FormBuilder';
import { SimpleModal, SimpleModalTypes } from './SimpleModal';

const aliasValidator = (data) => data.alias && data.alias.length >= 1 && data.alias.length <= 50;
const hostValidator = (data) => data.host && data.host.length >= 1 && data.host.length <= 50;
const portValidator = (data) => {
  if (!data.port) {
    return false;
  }
  const port = parseInt(data.port, 10);
  return (typeof data.port !== 'string' || `${port}` === data.port) && port > 0 && port < 65535;
};

const ModalFormModes = {
  ADD: 'ADD',
  UPDATE: 'UPDATE',
};

const aliasConfig = { key: 'alias', label: 'Alias', info: 'Alias can have a maximum length of 50 characters', validator: aliasValidator };
const hostConfig = { key: 'host', label: 'Host', info: 'Worker host address', validator: hostValidator };
const portConfig = { key: 'port', label: 'Port', info: 'Worker controller port', type: 'number', validator: portValidator };

function WorkersTable({ className }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const workers = useSelector((state) => state.workers);
  const [modalFormMode, setModalFormMode] = useState(null);
  const [modalData, setModalData] = useState({});
  const [valid, setValid] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState(null);
  const [modalNotificationConfigs, setModalNotificationConfigs] = useState({});

  const getSelectedWorker = () => workers.find((w) => w.id === selectedWorkerId);

  const getModalFormConfigs = () => {
    switch (modalFormMode) {
      case ModalFormModes.ADD:
        return [
          aliasConfig,
          hostConfig,
          portConfig,
        ];
      case ModalFormModes.UPDATE:
        return [
          aliasConfig,
          portConfig,
        ];
      default:
        return [];
    }
  };

  const onRefresh = () => {
    actionWrapper({ action: getWorkers, reload: true });
  };

  const dismissModalNotification = () => {
    setModalNotificationConfigs({});
  };

  const onAddClick = () => {
    if (!widgetProps.error) {
      setModalData({});
    }
    setValid(false);
    setModalFormMode(ModalFormModes.ADD);
  };

  const onSave = () => {
    setModalFormMode(null);
    actionWrapper({
      action: addWorker,
      params: modalData,
    });
  };

  const onUpdate = () => {
    setModalFormMode(null);
    actionWrapper({
      action: updateWorker,
      params: modalData,
    });
  };

  const ensureWorkerSelected = (title) => {
    if (!selectedWorkerId) {
      setModalNotificationConfigs({ title, description: 'Please select a worker first!' });
      return false;
    }
    return true;
  };

  const onUpdateClick = () => {
    if (ensureWorkerSelected('Update worker')) {
      const worker = getSelectedWorker();
      setModalData(worker);
      setValid(false);
      setModalFormMode(ModalFormModes.UPDATE);
    }
  };

  const onDisable = () => {
    if (ensureWorkerSelected('Disable worker')) {
      //
    }
  };

  const onDelete = () => {
    const title = 'Delete worker';
    if (ensureWorkerSelected('Delete worker')) {
      const worker = getSelectedWorker();
      setModalNotificationConfigs({
        title,
        description: `Are you sure you want to remove the worker "${worker.alias || ''}", this action is permanent?`,
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

  const getModalFormSubmitMethod = () => {
    switch (modalFormMode) {
      case ModalFormModes.ADD:
        return onSave;
      case ModalFormModes.UPDATE:
        return onUpdate;
      default:
        return null;
    }
  };

  useEffect(() => {
    actionWrapper({ action: getWorkers });
  }, []);

  return (
    <>
      <TableWidget
        className={className}
        {...widgetProps}
        onRefresh={onRefresh}
        cols={[
          { key: 'inUseIcon', type: 'Icon', width: '20px' },
          { header: 'Alias', key: 'alias' },
          { header: 'Host', key: 'host' },
          { header: 'Port', key: 'port', width: '70px' }]}
        rows={workers}
        actions={[
          { title: 'Add worker', icon: 'bi-plus-lg', onClick: onAddClick },
          { title: 'Update worker', icon: 'bi-pencil', onClick: onUpdateClick },
          { title: 'Disable worker', icon: 'bi-snow', onClick: onDisable },
          { title: 'Delete worker', icon: 'bi-x-lg', onClick: onDelete },
        ]}
        activeRowKey={selectedWorkerId}
        onRowClick={setSelectedWorkerId}
      />
      <ModalWrapper
        title="Add worker"
        show={!!modalFormMode}
        onHide={() => setModalFormMode(null)}
        valid={valid}
        onSubmit={getModalFormSubmitMethod()}
      >
        <FormBuilder data={modalData} setData={setModalData} configs={getModalFormConfigs()} changeValidity={setValid} />
      </ModalWrapper>
      <SimpleModal dismiss={dismissModalNotification} {...modalNotificationConfigs} />
    </>
  );
}

export default WorkersTable;
