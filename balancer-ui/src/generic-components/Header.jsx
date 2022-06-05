import React from 'react';
import {
  Container, Nav, Navbar, NavDropdown,
} from 'react-bootstrap';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import * as actions from '../redux/actions';

function Header({ menus, activeMenuPath }) {
  const username = useSelector((state) => state.username);
  const dispatch = useDispatch();
  const navigator = useNavigate();

  const logout = () => {
    dispatch(actions.logout());
  };

  const navigate = (path) => {
    navigator(path);
  };

  return (
    <Navbar collapseOnSelect expand="sm" bg="dark" variant="dark">
      <Container fluid>
        <Navbar.Brand className="logo"><img key="logo" src={`${window.location.origin}/balancer-logo.png`} alt="BL" /></Navbar.Brand>
        <Navbar.Toggle aria-controls="responsive-navbar-nav" />
        <Navbar.Collapse id="responsive-navbar-nav">
          <Nav className="me-auto" activeKey={activeMenuPath}>
            {menus.map((menu) => (
              <Nav.Link key={menu.path} eventKey={menu.path} onClick={() => navigate(menu.path)}>
                {menu.name}
              </Nav.Link>
            ))}
          </Nav>
          <Nav className="username-header ms-auto">
            <NavDropdown title={username} className="fw-bolder">
              <NavDropdown.Item onClick={logout}>
                Logout
              </NavDropdown.Item>
            </NavDropdown>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}

export default Header;
