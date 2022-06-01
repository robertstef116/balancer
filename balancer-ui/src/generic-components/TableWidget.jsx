import React from 'react';
import { Table } from 'react-bootstrap';
import WidgetWrapper from './WidgetWrapper';

function TableWidget({
  className, cols, rows, isLoading,
}) {
  const onAddClick = () => {
  };

  const renderCell = (data, col) => {
    switch (col.type) {
      case 'Icon':
        return <td key={col.key} style={{ width: col.width }}><i className={data[col.key]} /></td>;
      case 'InfoIcon':
        return (
          <td key={col.key} className="text-center" style={{ width: col.width }}>
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
      title="Workers List"
      isLoading={isLoading}
      className={className}
      actions={[
        { title: 'Add worker', icon: 'bi-plus', onClick: onAddClick },
      ]}
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
              <tr key={data.id}>
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
