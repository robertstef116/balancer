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

export const getWorkflows = async () => {
  const res = await axios.get(`${API_URL}/workflow`);
  if (res.status === 200) {
    return res.data;
  }
  throw new Error('Get workflows failed!');
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
