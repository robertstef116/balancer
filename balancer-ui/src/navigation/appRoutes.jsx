import React from 'react';
import {
  Route, Routes, useLocation,
} from 'react-router-dom';
import { CSSTransition } from 'react-transition-group';
import PrivateRoute from './PrivateRoute';
import HomePage from '../pages/Home';
import Login from '../pages/Login';
import NotFound from '../pages/NotFound';
import Header from '../generic-components/Header';
import NodesPage from '../pages/Nodes';
import ConfigurationPage from '../pages/Configuration';

export const routesPath = {
  homePage: '/',
  nodesPage: '/nodes',
  configurationPage: '/config',
  loginPage: '/login',
  aboutPage: '/about',
};

const routesWithHeader = [
  routesPath.homePage,
  routesPath.nodesPage,
  routesPath.configurationPage,
  routesPath.aboutPage,
];

const headerMenus = [
  { name: 'Home', path: routesPath.homePage },
  { name: 'Nodes', path: routesPath.nodesPage },
  { name: 'Configuration', path: routesPath.configurationPage },
  { name: 'About', path: routesPath.aboutPage },
];

export function AppRoutes() {
  const location = useLocation();

  return (
    <CSSTransition
      key={location.pathname}
      classNames="fade"
      timeout={200}
    >
      <>
        {routesWithHeader.includes(location.pathname) && (
        <Header
          menus={headerMenus}
          activeMenuPath={location.pathname}
        />
        )}
        <Routes location={location}>
          <Route
            path={routesPath.homePage}
            element={(
              <PrivateRoute>
                <HomePage />
              </PrivateRoute>
          )}
          />
          <Route
            path={routesPath.nodesPage}
            element={(
              <PrivateRoute>
                <NodesPage />
              </PrivateRoute>
          )}
          />
          <Route
            path={routesPath.configurationPage}
            element={(
              <PrivateRoute>
                <ConfigurationPage />
              </PrivateRoute>
          )}
          />
          <Route path={routesPath.loginPage} element={<Login />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </>
    </CSSTransition>
  );
}
