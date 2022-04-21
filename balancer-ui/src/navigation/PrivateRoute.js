import React from 'react';
import {Navigate, useLocation} from 'react-router-dom';
import {useAuth} from '../providers/Authentication';

const PrivateRoute = ({children}) => {
  let auth = useAuth();
  let location = useLocation();

  if (auth.isLoading) {
    return <div>loading...</div>
  }

  if (!auth.isAuthenticated) {
    return <Navigate to='/login' state={{from: location}} replace/>
  }

  return children;
};

export default PrivateRoute;
