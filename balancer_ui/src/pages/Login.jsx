import React, { useState } from 'react';
import { Button, Container, Form } from 'react-bootstrap';
import { useDispatch } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import classNames from 'classnames';
import { APP_NAME, Icons } from '../constants';
import * as actions from '../redux/actions';
import Spinner from '../components/Spinner';

function LoginPage() {
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();

  const from = location.state?.from?.pathname || '/';

  const onLogin = (event) => {
    event.preventDefault();
    setIsLoading(true);
    const username = event.target[0]?.value;
    const password = event.target[1]?.value;
    dispatch(actions.login(username, password, (err) => {
      if (err) {
        setIsLoading(false);
        setError(err);
        return;
      }
      navigate(from, { replace: true });
    }));
  };

  return (
    <Container
      className="login col-sm-9 col-md-7 col-lg-6 col-xl-5 col-xxl-4 bg-light rounded p-4 mt-4 animate__animated animate__fadeInTopLeft animate__fast"
    >
      <span className="text-uppercase fw-bolder h1">{APP_NAME}</span>
      <Form noValidate onSubmit={onLogin}>
        <Form.Group className="mb-3 mt-4" controlId="username">
          <Form.Label>Username</Form.Label>
          <Form.Control type="text" placeholder="Enter username" />
        </Form.Group>
        <Form.Group className="mb-4" controlId="password">
          <Form.Label>Password</Form.Label>
          <Form.Control type="password" placeholder="Enter password" />
        </Form.Group>
        {!!error && (
        <div className="text-danger mb-4">
          <i className={classNames('pe-1', Icons.ERROR)} />
          {error}
        </div>
        )}
        <Button variant="primary" type="submit">
          Login
        </Button>
      </Form>
      <Spinner visible={isLoading} />
    </Container>
  );
}

export default LoginPage;
