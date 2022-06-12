import React from 'react';
import { Form } from 'react-bootstrap';
import classNames from 'classnames';

function FormRow({
  label, error, value = '', onChange, info, type = 'text',
}) {
  return (
    <div className="row pt-3">
      <div className="col-5">
        <Form.Label>{label}</Form.Label>
      </div>
      <div className={classNames('col', { 'has-error': !!error })}>
        <div className="d-flex align-items-center">
          <Form.Control
            className="w-100"
            type={type}
            value={value}
            onChange={onChange}
          />
          {info && <i className="bi bi-info-circle text-primary ms-2" title={info} />}
        </div>
        <div className="input-error">
          {error}
        </div>
      </div>
    </div>
  );
}

export default FormRow;
