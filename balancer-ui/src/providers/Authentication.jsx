import React, {
  useState, useEffect, createContext, useContext,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as actions from '../redux/actions';

const AuthContext = createContext({});

const useAuth = () => useContext(AuthContext);

const useProvideAuth = () => {
  const [isLoading, setIsLoading] = useState(true);
  const isAuthenticated = useSelector((state) => state.isAuthenticated);
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(actions.loadCredentials(() => {
      setIsLoading(false);
    }));
  }, [dispatch]);

  return {
    isLoading,
    isAuthenticated,
  };
};

function AuthenticationProvider({ children }) {
  const auth = useProvideAuth();

  return (
    <AuthContext.Provider value={auth}>
      {children}
    </AuthContext.Provider>
  );
}

export {
  useAuth,
  AuthenticationProvider,
};
