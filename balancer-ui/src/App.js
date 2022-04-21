import React from 'react';
import './App.css';
import {AuthenticationProvider} from './providers/Authentication';
import {AppRoutes} from './navigation/appRoutes';
import {BrowserRouter} from 'react-router-dom';
import {TransitionGroup} from 'react-transition-group';
import {Provider} from "react-redux";
import store from './redux/store';

const App = () => {
  return (
    <Provider store={store}>
      <AuthenticationProvider>
        <BrowserRouter>
          <TransitionGroup>
            <AppRoutes/>
          </TransitionGroup>
        </BrowserRouter>
      </AuthenticationProvider>
    </Provider>
  );
}

export default App;
