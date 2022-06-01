import React, { useEffect, useRef } from 'react';
import { useSelector } from 'react-redux';
import TableWidget from '../generic-components/TableWidget';
import { getWorkers } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

function WorkersTable({ className }) {
  const { widgetProps, actionWrapper } = useWidgetUtils();
  const workers = useSelector((state) => state.workers);
  const workersRef = useRef(workers);

  useEffect(() => {
    if (!workersRef.current) {
      actionWrapper({ action: getWorkers });
    }
  }, []);

  return (
    <TableWidget
      className={className}
      {...widgetProps}
      cols={[
        {
          header: '', key: 'inUse', type: 'Icon', width: '20px',
        },
        { header: 'Alias', key: 'alias' },
        { header: 'Host', key: 'host' },
        { header: 'Port', key: 'port', width: '70px' }]}
      rows={workers}
    />
  );
}
export default WorkersTable;
