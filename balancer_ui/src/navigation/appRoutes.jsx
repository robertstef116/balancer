import React from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';
import HomePage from '../pages/Home';
import Login from '../pages/Login';
import NotFound from '../pages/NotFound';
import Header from '../generic-components/Header';
import ResourcesPage from '../pages/Resources';
import Footer from '../generic-components/Footer';

export const routesPath = {
  homePage: '/',
  resourcesPage: '/resources',
  loginPage: '/login',
};

const routesWithHeader = [
  routesPath.homePage,
  routesPath.resourcesPage,
];

const headerMenus = [
  { name: 'Analytics', path: routesPath.homePage },
  { name: 'Resources', path: routesPath.resourcesPage },
];

export function AppRoutes() {
  const location = useLocation();

  return (
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
          path={routesPath.resourcesPage}
          element={(
            <PrivateRoute>
              <ResourcesPage />
            </PrivateRoute>
          )}
        />
        <Route path={routesPath.loginPage} element={<Login />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
      <Footer />
    </>
  );
}
