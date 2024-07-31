import React from 'react';
import { Table } from 'react-bootstrap';
import classNames from 'classnames';
import WidgetWrapper from './WidgetWrapper';
import { Icons } from '../constants';

function TableWidget({
  title, className, cols, rows, onRefresh, actions, widgetProps, activeRowKey, onRowClick,
}) {
  const renderCell = (data, col) => {
    switch (col.type) {
      case 'Icon':
        return (
          <td key={col.key} style={{ width: col.width }} title={col.titleKey ? data[col.titleKey] : ''}>
            <i
              className={data[col.key]}
            />
          </td>
        );
      case 'InfoIcon':
        return (
          <td key={col.key} style={{ width: col.width }}>
            <i className={classNames('text-secondary', Icons.INFO)} title={data[col.key]} />
          </td>
        );
      default:
        return (
          <td
            key={col.key}
            className="text-truncate"
            style={{ width: col.width, maxWidth: col.maxWidth }}
            title={col.maxWidth ? data[col.key] : ''}
          >
            {data[col.key]}
          </td>
        );
    }
  };
  return (
    <WidgetWrapper
      title={title}
      className={className}
      onRefresh={onRefresh}
      actions={actions}
      widgetProps={widgetProps}
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
