import React, { useEffect, useState } from 'react';
import { Button, Modal } from 'react-bootstrap';

export const SimpleModalTypes = {
  WARNING: 'WARNING',
  CONFIRMATION: 'CONFIRMATION',
};

export function SimpleModal({ title, description, type = SimpleModalTypes.WARNING, dismiss }) {
  const [visible, setVisible] = useState(false);

  const onDismiss = (val) => {
    setVisible(false);
    dismiss(val);
  };

  useEffect(() => {
    if (description) {
      setVisible(true);
    }
  }, [description]);

  return (
    <Modal show={visible} onHide={() => onDismiss(false)}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {description}
      </Modal.Body>
      <Modal.Footer>
        {type === SimpleModalTypes.WARNING && <Button onClick={() => onDismiss(false)}>Ok</Button>}
        {type === SimpleModalTypes.CONFIRMATION && (
        <>
          <Button onClick={() => onDismiss(true)}>Yes</Button>
          <Button onClick={() => onDismiss(false)}>No</Button>
        </>
        )}
      </Modal.Footer>
    </Modal>
  );
}
