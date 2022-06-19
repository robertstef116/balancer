import { Cancel } from 'axios';
import * as types from './types';
import * as errors from './errorTypes';
import * as api from '../api';

const errorWrapper = async (action, defaultError, cb) => {
  try {
    await action();
  } catch (e) {
    if (e instanceof Cancel) {
      cb(e);
    } else {
      cb(errors.GET_ANALYTICS_ERROR);
    }
  }
};

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

export const getWorkers = ({ reload }, cb) => async (dispatch, getState) => {
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

export const addWorker = ({ alias, host, port }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    const worker = await api.addWorker(token, { alias, host, port });
    dispatch({
      type: types.ADD_WORKER,
      payload: {
        worker,
      },
    });
    cb();
  } catch (e) {
    cb(errors.ADD_WORKERS_ERROR);
  }
};

export const updateWorker = ({ id, alias, port }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.updateWorker(token, { id, alias, port });
    dispatch({
      type: types.UPDATE_WORKER,
      payload: {
        id, alias, port,
      },
    });
    cb();
  } catch (e) {
    cb(errors.UPDATE_WORKERS_ERROR);
  }
};

export const disableWorker = ({ id }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.disableWorker(token, { id });
    dispatch({
      type: types.DISABLE_WORKER,
      payload: {
        id,
      },
    });
    cb();
  } catch (e) {
    cb(errors.DISABLE_WORKER_ERROR);
  }
};

export const deleteWorker = ({ id }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.deleteWorker(token, { id });
    dispatch({
      type: types.DELETE_WORKER,
      payload: {
        id,
      },
    });
    cb();
  } catch (e) {
    cb(errors.DELETE_WORKERS_ERROR);
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

export const getWorkflows = ({ reload }, cb) => async (dispatch, getState) => {
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

export const addWorkflow = ({ image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    const workflow = await api.addWorkflow(token, { image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping });
    dispatch({
      type: types.ADD_WORKFLOW,
      payload: {
        workflow,
      },
    });
    cb();
  } catch (e) {
    cb(errors.ADD_WORKFLOWS_ERROR);
  }
};

export const updateWorkflow = ({ id, algorithm, minDeployments, maxDeployments }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.updateWorkflow(token, { id, algorithm, minDeployments, maxDeployments });
    dispatch({
      type: types.UPDATE_WORKFLOW,
      payload: {
        id, algorithm, minDeployments, maxDeployments,
      },
    });
    cb();
  } catch (e) {
    cb(errors.UPDATE_WORKFLOWS_ERROR);
  }
};

export const deleteWorkflow = ({ id }, cb) => async (dispatch, getState) => {
  const { token } = getState();
  try {
    await api.deleteWorkflow(token, { id });
    dispatch({
      type: types.DELETE_WORKFLOW,
      payload: {
        id,
      },
    });
    cb();
  } catch (e) {
    cb(errors.DELETE_WORKFLOWS_ERROR);
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

export const getResources = ({ reload }, cb) => async (dispatch, getState) => {
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

export const getConfigs = ({ reload }, cb) => async (dispatch, getState) => {
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

export const saveConfigs = ({ configs }, cb) => async (dispatch, getState) => {
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

export const getAnalyticsData = ({ from, workerId, workflowId, deploymentId, cancelToken }, cb) => errorWrapper(async () => {
  const res = await api.getAnalyticsData({
    from, workerId, workflowId, deploymentId, cancelToken,
  });

  cb(null, res);
}, errors.GET_ANALYTICS_ERROR, cb);

export const getWorkflowAnalyticsData = async ({ from, workerId, workflowId, cancelToken }, cb) => errorWrapper(async () => {
  const res = await api.getWorkflowAnalyticsData({
    from, workerId, workflowId, cancelToken,
  });

  cb(null, res);
}, errors.GET_WORKFLOW_ANALYTICS_ERROR, cb);
