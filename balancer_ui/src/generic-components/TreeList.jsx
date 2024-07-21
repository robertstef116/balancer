import React, { useEffect, useState } from 'react';
import { Accordion } from 'react-bootstrap';
import classNames from 'classnames';
import WidgetWrapper from './WidgetWrapper';

function List({
  data, selectedKey, onSelectionChanged, path = [],
}) {
  const [activeKey, setActiveKey] = useState(null);

  useEffect(() => {
    setActiveKey(null);
  }, [selectedKey]);

  const selectItem = (item) => {
    if (item.id === activeKey) {
      setActiveKey(selectedKey);
      onSelectionChanged(path);
    } else {
      setActiveKey(item.id);
      onSelectionChanged([...path, item]);
    }
  };

  return (
    <Accordion className="accordion-list" activeKey={activeKey} flush>
      {data.map((item) => (
        item.children && item.children.length
          ? (
            <Accordion.Item key={item.id} eventKey={item.id} className="bg-light border-0">
              <Accordion.Header onClick={() => selectItem(item)}>
                <span className={classNames('text-truncate ', { 'active-item': item.id === activeKey })}>
                  {item.name}
                </span>
              </Accordion.Header>
              <Accordion.Body className="p-0 ps-4">
                <List
                  data={item.children}
                  selectedKey={activeKey}
                  path={[...path, item]}
                  onSelectionChanged={onSelectionChanged}
                />
              </Accordion.Body>
            </Accordion.Item>
          )
          : (
            <div
              key={item.id}
              className={classNames('p-1 simple-list-item user-select-none text-truncate', { 'active-item': item.id === activeKey })}
              onClick={() => selectItem(item)}
            >
              {item.name}
            </div>
          )
      ))}
    </Accordion>
  );
}

function TreeList({
  classname, data, widgetProps, onSelectionChanged, onRefresh,
}) {
  return (
    <WidgetWrapper
      className={classname}
      title="Resources"
      widgetProps={widgetProps}
      onRefresh={onRefresh}
    >
      <div className="p-2">
        <List data={data} onSelectionChanged={onSelectionChanged} />
      </div>
    </WidgetWrapper>
  );
}

export default TreeList;
