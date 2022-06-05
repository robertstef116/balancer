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

const _getWorkers = async (token, dispatch) => {
  const workers = await api.getWorkers(token);
  dispatch({
    type: types.GET_WORKERS,
    payload: {
      workers,
    },
  });
};

export const getWorkers = (reload, cb) => async (dispatch, getState) => {
  const { workers, token } = getState();
  try {
    if (workers === null || reload) {
      await _getWorkers(token, dispatch);
    }
    cb();
  } catch (e) {
    cb(errors.GET_WORKERS_ERROR);
  }
};

const _getWorkflows = async (token, dispatch) => {
  const workflows = await api.getWorkflows(token);
  dispatch({
    type: types.GET_WORKFLOWS,
    payload: {
      workflows,
    },
  });
};

export const getWorkflows = (reload, cb) => async (dispatch, getState) => {
  const { workflows, token } = getState();
  try {
    if (workflows === null || reload) {
      await _getWorkflows(token, dispatch);
    }
    cb();
  } catch (e) {
    cb(errors.GET_WORKFLOWS_ERROR);
  }
};

const _getDeployments = async (token, dispatch) => {
  const deployments = await api.getDeployments(token);
  dispatch({
    type: types.GET_DEPLOYMENTS,
    payload: {
      deployments,
    },
  });
};

export const getResources = (reload, cb) => async (dispatch, getState) => {
  const {
    token, workers, workflows, deployments,
  } = getState();

  try {
    const promises = [];
    if (workers === null || reload) {
      promises.push(_getWorkers(token, dispatch));
    }
    if (workflows === null || reload) {
      promises.push(_getWorkflows(token, dispatch));
    }
    if (deployments === null || reload) {
      promises.push(_getDeployments(token, dispatch));
    }
    await Promise.all(promises);
    cb();
  } catch (e) {
    cb(errors.GET_RESOURCES);
  }
};

export const getConfigs = (reload, cb) => async (dispatch, getState) => {
  const { configs, token } = getState();
  try {
    if (configs === null || reload) {
      const newConfigs = await api.getConfigs(token);

      dispatch({
        type: types.GET_CONFIGS,
        payload: {
          configs: newConfigs,
        },
      });
    }
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

export const getAnalyticsData = async (from, workerId, workflowId, deploymentId, cb) => {
  try {
    const res = await api.getAnalyticsData({
      from, workerId, workflowId, deploymentId,
    });

    cb(null, res);
  } catch (e) {
    cb(errors.GET_ANALYTICS_ERROR);
  }
};

export const getWorkflowAnalyticsData = async (from, workerId, workflowId, cb) => {
  try {
    const res = await api.getWorkflowAnalyticsData({
      from, workerId, workflowId,
    });

    cb(null, res);
  } catch (e) {
    cb(errors.GET_WORKFLOW_ANALYTICS_ERROR);
  }
};
