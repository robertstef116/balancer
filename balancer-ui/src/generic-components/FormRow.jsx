import React, { useEffect, useRef } from 'react';
import { Button, Dropdown, Form } from 'react-bootstrap';
import classNames from 'classnames';
import { Icons } from '../constants';

function FormRow({
  label, error, value = '', onChange, info, type = 'text', options = [], mapValueType = 'text',
}) {
  const mapEntriesKeyCount = useRef(0);

  const onMapKeyChange = (val, entry) => {
    const entryValue = value.find((e) => e.internalKey === entry.internalKey);
    entryValue.key = val;
    onChange([...value]);
  };

  const onMapValueChange = (val, entry) => {
    const entryValue = value.find((e) => e.internalKey === entry.internalKey);
    entryValue.value = val;
    onChange([...value]);
  };

  const getControl = () => {
    switch (type) {
      case 'dropdown':
        const selectedOption = options.find((opt) => opt.value === value);
        return (
          <Dropdown className="w-100 h-100">
            <Dropdown.Toggle className="d-flex justify-content-between align-items-center w-100 h-100 form-dropdown">
              <span className="text-truncate">
                {selectedOption ? selectedOption.display : ' '}
              </span>
            </Dropdown.Toggle>
            <Dropdown.Menu>
              {options.map((optionConfig) => (
                <Dropdown.Item
                  key={optionConfig.value}
                  onClick={() => onChange(optionConfig.value)}
                >
                  {optionConfig.display}
                </Dropdown.Item>
              ))}
            </Dropdown.Menu>
          </Dropdown>
        );
      case 'map':
        return (
          <div className="w-100">
            <div className="container p-0">
              {value && value.map((entry) => (
                <div key={entry.internalKey} className="row m-0 align-items-center pb-1">
                  <Form.Control
                    className="col"
                    value={entry.key}
                    onChange={({ target }) => onMapKeyChange(target.value, entry)}
                  />
                  <i className={classNames('col-auto p-1 bi', Icons.ARROW_RIGHT)} />
                  <Form.Control
                    className="col"
                    value={entry.value}
                    type={mapValueType}
                    onChange={({ target }) => onMapValueChange(target.value, entry)}
                  />
                  <i
                    className={classNames('col-auto p-1 widget-action bi', Icons.DELETE)}
                    title="Delete"
                    onClick={() => onChange(value.filter((v) => v.internalKey !== entry.internalKey))}
                  />
                </div>
              ))}
            </div>
            <div className="text-end">
              <Button
                variant="outline-primary"
                className="py-0"
                onClick={() => onChange([...value, { internalKey: mapEntriesKeyCount.current++, key: '', value: '' }])}
              >
                <i className={classNames('bi', Icons.ADD)} />
              </Button>
            </div>
          </div>
        );
      default:
        return (
          <Form.Control
            className="w-100"
            type={type}
            value={value}
            onChange={({ target }) => onChange(target.value)}
          />
        );
    }
  };

  useEffect(() => {
    if (type === 'map' && !value) {
      onChange([{ internalKey: mapEntriesKeyCount.current++, key: '', value: '' }]);
    }
  }, []);

  return (
    <div className="row pt-3">
      <div className="col-5">
        <Form.Label>{label}</Form.Label>
      </div>
      <div className={classNames('col', { 'has-error': !!error })}>
        <div className="d-flex align-items-center">
          {getControl()}
          {info && <i className={classNames('text-primary ms-2', Icons.INFO)} title={info} />}
        </div>
        <div className="input-error">
          {error}
        </div>
      </div>
    </div>
  );
}

export default FormRow;
