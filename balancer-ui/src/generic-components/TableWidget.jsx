import React from 'react';
import { Table } from 'react-bootstrap';
import classNames from 'classnames';
import WidgetWrapper from './WidgetWrapper';

function TableWidget({
  title, className, cols, rows, isLoading, onRefresh, actions, error, dismissError, activeRowKey, onRowClick,
}) {
  const renderCell = (data, col) => {
    switch (col.type) {
      case 'Icon':
        return <td key={col.key} style={{ width: col.width }}><i className={data[col.key]} /></td>;
      case 'InfoIcon':
        return (
          <td key={col.key} style={{ width: col.width }}>
            <i className="bi bi-info-circle text-secondary" title={data[col.key]} />
          </td>
        );
      default:
        return (
          <td key={col.key} className="text-truncate" style={{ width: col.width, maxWidth: col.maxWidth }} title={col.maxWidth ? data[col.key] : ''}>
            {data[col.key]}
          </td>
        );
    }
  };
  return (
    <WidgetWrapper
      title={title}
      isLoading={isLoading}
      className={className}
      onRefresh={onRefresh}
      actions={actions}
      error={error}
      dismissError={dismissError}
    >
      <div className="scrollable-table overflow-auto w-100 h-100">
        <Table striped borderless hover>
          <thead>
            <tr>
              {cols.map((col) => <th key={col.key} className="align-top">{col.header}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows && rows.map((data) => (
              <tr
                key={data.id}
                className={classNames({ active: activeRowKey === data.id })}
                {...onRowClick && { onClick: () => onRowClick(data.id) }}
              >
                {cols.map((col) => renderCell(data, col))}
              </tr>
            ))}
          </tbody>
        </Table>
      </div>
    </WidgetWrapper>
  );
}

export default TableWidget;
