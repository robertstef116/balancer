import React from 'react';
import { Button, Modal } from 'react-bootstrap';

function ModalWrapper({
  title, show, onHide, onSubmit, children, valid, submitTitle = 'Save',
}) {
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>
          {title}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {children}
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onSubmit} disabled={!valid}>{submitTitle}</Button>
      </Modal.Footer>
    </Modal>
  );
}

export default ModalWrapper;
