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

export const addWorker = ({ alias, host, port }, cb) => async (dispatch) => {
  try {
    const worker = await api.addWorker({ alias, host, port });
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

export const updateWorker = ({ id, alias, port }, cb) => async (dispatch) => {
  try {
    await api.updateWorker({ id, alias, port });
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

export const disableWorker = ({ id }, cb) => async (dispatch) => {
  try {
    await api.disableWorker({ id });
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

export const addWorkflow = ({ image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping }, cb) => async (dispatch) => {
  try {
    const workflow = await api.addWorkflow({ image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping });
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
      payload: {
        id, algorithm, minDeployments, maxDeployments,
      },
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

const _getDeployments = async (dispatch) => {
  const deployments = await api.getDeployments();
  dispatch({
    type: types.GET_DEPLOYMENTS,
    payload: {
      deployments,
    },
  });
};

export const getResources = ({ reload }, cb) => async (dispatch, getState) => {
  const { workers, workflows, deployments } = getState();

  try {
    const promises = [];
    if (workers === null || reload) {
      promises.push(_getWorkers(dispatch));
    }
    if (workflows === null || reload) {
      promises.push(_getWorkflows(dispatch));
    }
    if (deployments === null || reload) {
      promises.push(_getDeployments(dispatch));
    }
    await Promise.all(promises);
    cb();
  } catch (e) {
    cb(errors.GET_RESOURCES);
  }
};

export const getConfigs = ({ reload }, cb) => async (dispatch, getState) => {
  const { configs } = getState();
  try {
    if (configs === null || reload) {
      const newConfigs = await api.getConfigs();

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

export const saveConfigs = ({ configs }, cb) => async (dispatch) => {
  try {
    await api.saveConfigs({ configs });

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
