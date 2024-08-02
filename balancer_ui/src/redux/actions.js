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

const _getWorkers = async (dispatch) => {
  const workers = await api.getWorkers();
  dispatch({
    type: types.GET_WORKERS,
    payload: {
      workers,
    },
  });
};

export const getWorkers = ({ reload }, cb) => async (dispatch, getState) => {
  const { workers } = getState();
  try {
    if (workers === null || reload) {
      await _getWorkers(dispatch);
    }
    cb();
  } catch (e) {
    cb(errors.GET_WORKERS_ERROR);
  }
};

export const updateWorker = ({ id, alias, state }, cb) => async (dispatch) => {
  try {
    await api.updateWorker({ id, alias, state });
    dispatch({
      type: types.UPDATE_WORKER,
      payload: {
        id, alias, state,
      },
    });
    cb();
  } catch (e) {
    cb(errors.UPDATE_WORKERS_ERROR);
  }
};

export const deleteWorker = ({ id }, cb) => async (dispatch) => {
  try {
    await api.deleteWorker({ id });
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

const _getWorkflows = async (dispatch) => {
  const workflows = await api.getWorkflows();
  dispatch({
    type: types.GET_WORKFLOWS,
    payload: {
      workflows,
    },
  });
};

export const getWorkflows = ({ reload }, cb) => async (dispatch, getState) => {
  const { workflows } = getState();
  try {
    if (workflows === null || reload) {
      await _getWorkflows(dispatch);
    }
    cb();
  } catch (e) {
    cb(errors.GET_WORKFLOWS_ERROR);
  }
};

export const addWorkflow = ({
  image,
  memoryLimit,
  cpuLimit,
  algorithm,
  minDeployments,
  maxDeployments,
  pathMapping,
}, cb) => async (dispatch) => {
  try {
    const workflow = await api.addWorkflow({
      image,
      memoryLimit,
      cpuLimit,
      algorithm,
      minDeployments,
      maxDeployments,
      pathMapping,
    });
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

export const updateWorkflow = ({ id, algorithm, minDeployments, maxDeployments }, cb) => async (dispatch) => {
  try {
    await api.updateWorkflow({ id, algorithm, minDeployments, maxDeployments });
    dispatch({
      type: types.UPDATE_WORKFLOW,
      payload: { id, algorithm, minDeployments, maxDeployments },
    });
    cb();
  } catch (e) {
    cb(errors.UPDATE_WORKFLOWS_ERROR);
  }
};

export const deleteWorkflow = ({ id }, cb) => async (dispatch) => {
  try {
    await api.deleteWorkflow({ id });
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

export const getResources = ({ reload }, cb) => async (dispatch, getState) => {
  const { workers, workflows } = getState();

  try {
    const promises = [];
    if (workers === null || reload) {
      promises.push(_getWorkers(dispatch));
    }
    if (workflows === null || reload) {
      promises.push(_getWorkflows(dispatch));
    }
    await Promise.all(promises);
    cb();
  } catch (e) {
    cb(errors.GET_RESOURCES);
  }
};

export const getAnalyticsData = ({ durationMs, workflowId, metric, cancelToken }, cb) => errorWrapper(async () => {
  const res = await api.getAnalyticsData({
    durationMs, workflowId, metric, cancelToken,
  });

  cb(null, res);
}, errors.GET_ANALYTICS_ERROR, cb);

export const getBalancingAnalyticsData = async ({
  durationMs,
  workflowId,
  path,
  cancelToken,
}, cb) => errorWrapper(async () => {
  const res = await api.getBalancingAnalyticsData({
    durationMs, workflowId, path, cancelToken,
  });

  cb(null, res);
}, errors.GET_WORKFLOW_ANALYTICS_ERROR, cb);
