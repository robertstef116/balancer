import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from 'react-bootstrap';

function NotFound() {
  const navigator = useNavigate();

  return (
    <div
      className="justify-content-center text-center h-100 d-flex flex-column justify-content-around bg-secondary py-4 mt-5"
    >
      <span className="display-1 fw-bold text-primary animate__bounceIn animate__bounceInDown">404</span>
      <span className="display-4 fw-bolder text-dark animate__bounceIn animate__bounceInLeft">Not Found</span>
      <span className="pt-5">
        <Button
          className="btn-lg fw-bolder animate__bounceIn animate__bounceInLeft"
          onClick={() => navigator('/')}
        >
          Home
        </Button>
      </span>
    </div>
  );
}

export default NotFound;
