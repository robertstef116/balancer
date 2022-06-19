import axios from 'axios';
import { API_URL } from './constants';

export const login = async ({ username, password }) => {
  const res = await axios.post(`${API_URL}/login`, { username, password });
  if (res.status === 200 && res.data?.token) {
    return res.data.token;
  }
  throw new Error('Login failed!');
};

export const getWorkers = async () => {
  const res = await axios.get(`${API_URL}/worker`);
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Get workers failed!');
};

export const addWorker = async (token, { alias, host, port }) => {
  const res = await axios.post(`${API_URL}/worker`, { alias, host, port });
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Add worker failed!');
};

export const updateWorker = async (token, { id, alias, port }) => {
  const res = await axios.put(`${API_URL}/worker/${id}`, { alias, port });
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Update worker failed!');
};

export const disableWorker = async (token, { id }) => {
  const res = await axios.put(`${API_URL}/worker/${id}/stop`);
  if (res.status === 200) {
    return;
  }
  throw new Error('Delete worker failed!');
};

export const deleteWorker = async (token, { id }) => {
  const res = await axios.delete(`${API_URL}/worker/${id}`);
  if (res.status === 200) {
    return;
  }
  throw new Error('Delete worker failed!');
};

export const getWorkflows = async () => {
  const res = await axios.get(`${API_URL}/workflow`);
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Get workflows failed!');
};

export const addWorkflow = async (token, { image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping }) => {
  const res = await axios.post(`${API_URL}/workflow`, { image, memoryLimit, algorithm, minDeployments, maxDeployments, pathMapping });
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Add workflow failed!');
};

export const updateWorkflow = async (token, { id, algorithm, minDeployments, maxDeployments }) => {
  const res = await axios.put(`${API_URL}/workflow/${id}`, { algorithm, minDeployments, maxDeployments });
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Update workflow failed!');
};

export const deleteWorkflow = async (token, { id }) => {
  const res = await axios.delete(`${API_URL}/workflow/${id}`);
  if (res.status === 200) {
    return;
  }
  throw new Error('Delete workflow failed!');
};

export const getDeployments = async () => {
  const res = await axios.get(`${API_URL}/deployment`);
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Get deployments failed!');
};

export const getConfigs = async () => {
  const res = await axios.get(`${API_URL}/config`);
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Get configs failed!');
};

export const saveConfigs = async ({ configs }) => {
  const res = await axios.put(`${API_URL}/config`, configs);
  if (res.status !== 200) {
    throw new Error('Failed to save configs!');
  }
};

export const getAnalyticsData = async ({
  from, workerId, workflowId, deploymentId, cancelToken,
}) => {
  let qs = '';
  if (workerId) {
    qs += `&workerId=${workerId}`;
  }
  if (workflowId) {
    qs += `&workflowId=${workflowId}`;
  }
  if (deploymentId) {
    qs += `&deploymentId=${deploymentId}`;
  }

  const res = await axios.get(`${API_URL}/analytics/requests?from=${from}${qs}`, {
    cancelToken,
  });
  if (res.status === 200) {
    return res.data;
  }

  throw new Error('Failed to analytics data!');
};

export const getWorkflowAnalyticsData = async ({
  from, workerId, workflowId, cancelToken,
}) => {
  let qs = '';
  if (workerId) {
    qs += `&workerId=${workerId}`;
  }
  if (workflowId) {
    qs += `&workflowId=${workflowId}`;
  }

  const res = await axios.get(`${API_URL}/analytics/scaling?from=${from}${qs}`, {
    cancelToken,
  });
  if (res.status === 200) {
    return res.data;
  }

  throw new Error('Failed to load workflow analytics data!');
};
