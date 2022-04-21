import * as types from './types';
import * as errors from './errorTypes';
import * as api from '../api';

export const login = (username, password, cb) => async (dispatch, getState) => {
  try {
    const token = await api.login(username, password);
    if (token) {
      sessionStorage.setItem('JWT_TOKEN', token);
      sessionStorage.setItem('USERNAME', username);
      dispatch({
        type: types.LOGIN,
        payload: {
          username,
          token,
          isAuthenticated: true
        }
      });
      cb();
    }
  } catch (e) {
    cb(errors.LOGIN_ERROR);
  }
};

export const logout = () => (dispatch) => {
  sessionStorage.removeItem('JWT_TOKEN');
  sessionStorage.removeItem('USERNAME');
  dispatch({
    type: types.LOGIN,
    payload: {
      username: null,
      token: null,
      isAuthenticated: false
    }
  });
}

export const loadCredentials = (cb) => (dispatch) => {
  const token = sessionStorage.getItem('JWT_TOKEN');
  const username = sessionStorage.getItem('USERNAME');
  if (token && username) {
    dispatch({
      type: types.SET_CREDENTIALS,
      payload: {
        username,
        token,
        isAuthenticated: true
      }
    })
  }
  cb();
}
