import React from 'react';
import {BrowserRouter, Route, Routes, useLocation} from 'react-router-dom';
import PrivateRoute from './PrivateRoute';
import HomePage from '../pages/Home';
import Login from '../pages/Login';
import NotFound from '../pages/NotFound';
import {CSSTransition, TransitionGroup} from 'react-transition-group';
import Header from "../components/Header";
import NodesPage from "../pages/Nodes";

export const routesPath = {
  homePage: '/',
  nodesPage: '/nodes',
  loginPage: '/login',
  aboutPage: '/about'
};

const routesWithHeader = [
  routesPath.homePage,
  routesPath.nodesPage,
  routesPath.aboutPage
]

const headerMenus = [
  {name: 'Home', path: routesPath.homePage},
  {name: 'Nodes', path: routesPath.nodesPage},
  {name: 'About', path: routesPath.aboutPage}
]

export const AppRoutes = () => {
  const location = useLocation();

  return (
    <CSSTransition
      key={location.pathname}
      classNames='fade'
      timeout={200}>
      <>
        {routesWithHeader.includes(location.pathname) && <Header menus={headerMenus} activeMenuPath={location.pathname}/>}
        <Routes location={location}>
          <Route path={routesPath.homePage} element={
            <PrivateRoute>
              <HomePage/>
            </PrivateRoute>
          }/>
          <Route path={routesPath.nodesPage} element={
            <PrivateRoute>
              <NodesPage/>
            </PrivateRoute>
          }/>
          <Route path={routesPath.loginPage} element={<Login/>}/>
          <Route path='*' element={<NotFound/>}/>
        </Routes>
      </>
    </CSSTransition>
  )
}
