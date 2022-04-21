import axios from 'axios';
import {API_URL} from "./constants";

export const login = async (username, password) => {
  const res = await axios.post(`${API_URL}/login`, {username, password});
  if (res.status === 200 && res.data?.token) {
    return res.data.token;
  }
  throw new Error('Login failed!');
};
