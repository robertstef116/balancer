import React, {useState, useEffect, createContext, useContext} from 'react';
import {useDispatch, useSelector} from "react-redux";
import * as actions from '../redux/actions';

const AuthContext = createContext({});

const useAuth = () => {
  return useContext(AuthContext);
}

// const fakeAuth = {
//   load() {
//     const token = sessionStorage.getItem('JWT_TOKEN');
//     const username = sessionStorage.getItem('USERNAME');
//     if (token && username) {
//       return {username, token};
//     }
//     return null;
//   },
//   signin(token, username, cb) {
//     sessionStorage.setItem('JWT_TOKEN', token);
//     sessionStorage.setItem('USERNAME', username);
//     cb();
//   },
//   signout(cb) {
//     sessionStorage.removeItem('JWT_TOKEN');
//     sessionStorage.removeItem('USER');
//     cb();
//   }
// };

const useProvideAuth = () => {
  const [isLoading, setIsLoading] = useState(true);
  const isAuthenticated = useSelector(state => state.isAuthenticated);
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(actions.loadCredentials(() => {
      setIsLoading(false);
    }));
  }, [dispatch]);

  return {
    isLoading,
    isAuthenticated
  };
}

const AuthenticationProvider = ({children}) => {
  const auth = useProvideAuth();

  return (
    <AuthContext.Provider value={auth}>
      {children}
    </AuthContext.Provider>
  )
};

export {
  useAuth,
  AuthenticationProvider
}
