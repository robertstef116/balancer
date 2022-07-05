import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { AppRoutes } from './navigation/appRoutes';
import { AuthenticationProvider } from './providers/Authentication';
import store from './redux/store';

function App() {
  return (
    <Provider store={store}>
      <AuthenticationProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthenticationProvider>
    </Provider>
  );
}

export default App;
