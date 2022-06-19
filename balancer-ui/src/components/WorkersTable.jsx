import React, { useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import TableWidget from '../generic-components/TableWidget';
import { addWorker, deleteWorker, disableWorker, getWorkers, updateWorker } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import ModalWrapper from '../generic-components/ModalWrapper';
import FormBuilder from '../generic-components/FormBuilder';
import { SimpleModal, SimpleModalTypes } from './SimpleModal';
import { Icons, ModalFormModes, WorkerNodeStatus } from '../constants';

const aliasValidator = (data) => data.alias && data.alias.length >= 1 && data.alias.length <= 50;
const hostValidator = (data) => data.host && data.host.length >= 1 && data.host.length <= 50;
const portValidator = (data) => {
  if (!data.port) {
    return false;
  }
  const port = parseInt(data.port, 10);
  return (`${port}` === `${data.port}`) && port > 0 && port < 65535;
};

const aliasConfig = { key: 'alias', label: 'Alias', info: 'Alias can have a maximum length of 50 characters', validator: aliasValidator };
const hostConfig = { key: 'host', label: 'Host', info: 'Worker host address', validator: hostValidator };
const portConfig = { key: 'port', label: 'Port', info: 'Worker controller port', type: 'number', validator: portValidator };

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

  const onAddClick = () => {
    if (!widgetProps.error) {
      setModalData({ port: 8081 }); // set default values
    }
    setValid(false);
    setModalFormMode(ModalFormModes.ADD);
  };

  const onUpdateClick = () => {
    if (ensureWorkerSelected('Update worker')) {
      const worker = getSelectedWorker();
      setModalData(worker);
      setValid(false);
      setModalFormMode(ModalFormModes.UPDATE);
    }
  };

  const onSave = useCallback(() => {
    actionWrapper({
      action: addWorker,
      params: modalData,
      cb: () => {
        setModalFormMode(null);
      },
    });
  }, [modalData]);

  const onUpdate = useCallback(() => {
    actionWrapper({
      action: updateWorker,
      params: modalData,
      cb: () => {
        setModalFormMode(null);
      },
    });
  }, [modalData]);

  const onDisable = () => {
    const worker = getSelectedWorker();
    const actionName = worker.status === WorkerNodeStatus.STARTED || worker.status === WorkerNodeStatus.STARTING ? 'Disable' : 'Enable';
    const title = `${actionName} worker`;
    if (ensureWorkerSelected('Disable worker')) {
      setModalNotificationConfigs({
        title,
        description: `Are you sure you want to ${actionName.toLowerCase()} the worker "${worker.alias || ''}", this action might be destructive?`,
        type: SimpleModalTypes.CONFIRMATION,
        dismiss: (res) => {
          if (res) {
            actionWrapper({ action: disableWorker, params: { id: selectedWorkerId } });
          }
          dismissModalNotification();
        },
      });
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

  useEffect(() => {
    actionWrapper({ action: getWorkers });
  }, []);

  useEffect(() => {
    switch (modalFormMode) {
      case ModalFormModes.ADD:
        return setModalFormConfig({
          title: 'Add workflow',
          fields: [
            aliasConfig,
            hostConfig,
            portConfig,
          ],
          submit: onSave,
        });
      case ModalFormModes.UPDATE:
        return setModalFormConfig({
          title: 'Update workflow',
          fields: [
            aliasConfig,
            portConfig,
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
        title="Workers List"
        {...widgetProps}
        onRefresh={onRefresh}
        cols={[
          { key: 'statusIcon', type: 'Icon', width: '20px', titleKey: 'statusTitle' },
          { header: 'Alias', key: 'alias' },
          { header: 'Host', key: 'host' },
          { header: 'Port', key: 'port', width: '70px' }]}
        rows={workers}
        actions={[
          { title: 'Add worker', icon: Icons.ADD, onClick: onAddClick },
          { title: 'Update worker', icon: Icons.EDIT, onClick: onUpdateClick },
          { title: 'Disable worker', icon: Icons.FREEZE, onClick: onDisable },
          { title: 'Delete worker', icon: Icons.DELETE, onClick: onDelete },
        ]}
        activeRowKey={selectedWorkerId}
        onRowClick={setSelectedWorkerId}
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

export default WorkersTable;
