import React from 'react';
import './App.css';
import { BrowserRouter } from 'react-router-dom';
import { TransitionGroup } from 'react-transition-group';
import { Provider } from 'react-redux';
import { AppRoutes } from './navigation/appRoutes';
import { AuthenticationProvider } from './providers/Authentication';
import store from './redux/store';

function App() {
  return (
    <Provider store={store}>
      <AuthenticationProvider>
        <BrowserRouter>
          <TransitionGroup>
            <AppRoutes />
          </TransitionGroup>
        </BrowserRouter>
      </AuthenticationProvider>
    </Provider>
  );
}

export default App;
