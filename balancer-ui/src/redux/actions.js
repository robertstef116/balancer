import * as types from './types';
import * as errors from './errorTypes';
import * as api from '../api';

export const login = (username, password, cb) => async (dispatch) => {
  try {
    const token = await api.login({ username, password });
    if (token) {
      sessionStorage.setItem('JWT_TOKEN', token);
      sessionStorage.setItem('USERNAME', username);
      dispatch({
        type: types.LOGIN,
        payload: {
          username,
          token,
          isAuthenticated: true,
        },
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
      isAuthenticated: false,
    },
  });
};

export const loadCredentials = (cb) => (dispatch) => {
  const token = sessionStorage.getItem('JWT_TOKEN');
  const username = sessionStorage.getItem('USERNAME');
  if (token && username) {
    dispatch({
      type: types.SET_CREDENTIALS,
      payload: {
        username,
        token,
        isAuthenticated: true,
      },
    });
  }
  cb();
};

export const getWorkers = (cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    const workers = await api.getWorkers(token);
    dispatch({
      type: types.GET_WORKERS,
      payload: {
        workers,
      },
    });
    cb();
  } catch (e) {
    cb(errors.GET_WORKERS_ERROR);
  }
};

export const getWorkflows = (cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    const workflows = await api.getWorkflows(token);
    dispatch({
      type: types.GET_WORKFLOWS,
      payload: {
        workflows,
      },
    });
    cb();
  } catch (e) {
    cb(errors.GET_WORKFLOWS_ERROR);
  }
};

export const getConfigs = (cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    const configs = await api.getConfigs(token);
    dispatch({
      type: types.GET_CONFIGS,
      payload: {
        configs,
      },
    });
    cb();
  } catch (e) {
    cb(errors.GET_CONFIGS_ERROR);
  }
};

export const saveConfigs = (configs, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.saveConfigs({ token, configs });
    dispatch({
      type: types.SAVE_CONFIGS,
      payload: {
        configs,
      },
    });
    cb();
  } catch (e) {
    cb(errors.SAVE_CONFIGS_ERROR);
  }
};
