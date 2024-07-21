import React from 'react';
import { Button, Modal } from 'react-bootstrap';
import classNames from 'classnames';
import Spinner from '../components/Spinner';
import { Icons } from '../constants';

function ModalWrapper({
  title, show, onHide, onSubmit, children, valid, widgetProps = { error: null, dismissError: null }, submitTitle = 'Save',
}) {
  const { isLoading, error, dismissError } = widgetProps;

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>
          {title}
        </Modal.Title>
      </Modal.Header>
      {error && (
        <div className="bg-danger text-light px-1 d-inline-flex justify-content-between rounded m-1 text-truncate">
          <span className="text-truncate" title={error}>
            {error}
          </span>
          <i className={classNames('icon-button bi', Icons.DELETE)} onClick={dismissError} />
        </div>
      )}
      <Modal.Body>
        {children}
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onSubmit} disabled={!valid}>{submitTitle}</Button>
      </Modal.Footer>
      <Spinner visible={isLoading} />
    </Modal>
  );
}

export default ModalWrapper;
